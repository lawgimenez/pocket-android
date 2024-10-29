package com.pocket.sdk;

import com.google.common.annotations.VisibleForTesting;
import com.pocket.sdk.api.endpoint.ApiException;
import com.pocket.sdk.api.endpoint.AppInfo;
import com.pocket.sdk.api.endpoint.Credentials;
import com.pocket.sdk.api.endpoint.DeviceInfo;
import com.pocket.sdk.api.generated.PocketAuthType;
import com.pocket.sdk.api.generated.enums.AuthMethod;
import com.pocket.sdk.api.generated.enums.LogoutReason;
import com.pocket.sdk.api.generated.thing.Account;
import com.pocket.sdk.api.generated.thing.GetAfterLogin;
import com.pocket.sdk.api.generated.thing.Getuser;
import com.pocket.sdk.api.generated.thing.Guid;
import com.pocket.sdk.api.generated.thing.LoginInfo;
import com.pocket.sdk.api.generated.thing.OAuthAuthorize;
import com.pocket.sdk.api.generated.thing.OAuthSsoauth;
import com.pocket.sdk.api.generated.thing.PremiumGift;
import com.pocket.sdk.api.generated.thing.Signup;
import com.pocket.sdk.api.source.PocketRemoteSource;
import com.pocket.sdk.api.source.PocketSource;
import com.pocket.sdk.api.spec.PocketSpec;
import com.pocket.sdk.api.thing.AccountUtil;
import com.pocket.sdk.api.value.AccessToken;
import com.pocket.sdk.api.value.Password;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.network.EclecticOkHttpClient;
import com.pocket.sync.action.Action;
import com.pocket.sync.source.AppSource;
import com.pocket.sync.source.AsyncClientSource;
import com.pocket.sync.source.PendingResult;
import com.pocket.sync.source.Source;
import com.pocket.sync.source.result.RemotePriority;
import com.pocket.sync.source.result.Result;
import com.pocket.sync.source.result.SyncException;
import com.pocket.sync.source.subscribe.Changes;
import com.pocket.sync.source.subscribe.PublishingSubscriber;
import com.pocket.sync.source.subscribe.Subscriber;
import com.pocket.sync.source.subscribe.Subscription;
import com.pocket.sync.source.threads.JavaThreadPools;
import com.pocket.sync.source.threads.Publisher;
import com.pocket.sync.source.threads.ThreadPools;
import com.pocket.sync.source.threads.ThreadPublisher;
import com.pocket.sync.space.Holder;
import com.pocket.sync.space.Space;
import com.pocket.sync.space.mutable.MutableSpace;
import com.pocket.sync.space.persist.MigrationStorage;
import com.pocket.sync.thing.Thing;
import com.pocket.util.java.Safe;

import org.apache.commons.lang3.StringUtils;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import okhttp3.OkHttpClient;

/**
 * A Pocket that can work locally, offline and syncs with Pocket's API.
 *
 * <h2>The Basics</h2>
 * Create an instance with {@link #Pocket(Config)}, or use {@code AndroidPocket} from {@code sync-pocket-android}.
 * <p>
 * As needed, use the {@link #user()} API to log in, sign up or log out the user in this Pocket.
 * <p>
 * Changes to a Pocket are called Actions. See what actions are available with {@code spec().actions()}.
 * <p>
 * Objects and state in Pocket are called Things. See what things are available with {@code spec().things()}.
 * <p>
 * To apply an action and make a change, or to obtain the current state of a thing,
 * use the {@link #spec()} to specify the action or thing and then
 * use {@link #sync(Thing, Action...)} or one of the various sync() variants.
 * <p>
 * To listen for changes, use {@link #subscribe(Changes, Subscriber)} or a {@code bind()} variant.
 * <p>
 * To request that a thing be persisted for offline usage, use {@link #remember(Holder, Thing...)} and {@link #forget(Holder, Thing...)}
 * <p>
 * Data will be persisted across app sessions, as long as you recreate Pocket with the same settings.
 * It is recommended to keep a singleton instance of Pocket in your app.
 * Clear all data from the instance with {@code user().logout()}.
 *
 * <h2>Going Deeper</h2>
 *
 * TODO
 *
 * This is also backed by an {@link AppSource} that has some specific implementation details that are good to understand. TODO can we organize the interfaces here so those docs appear on these methods?
 */

public class Pocket implements AsyncClientSource {
	
	private final PocketSource source;
	private final Config config;
	private final PocketSpec spec;
	private final UserApi account;
	private final Set<SetupStep> setupSteps = new HashSet<>();
	
	private boolean initialized;
	
	/**
	 * A Pocket with custom configuration.
	 */
	public Pocket(Config config) {
		this.config = config;
		this.spec = config.spec;
		this.account = new UserApi();
		this.source = new PocketSource(spec, config.space.setSpec(spec), config.remote, config.publisher, config.threads);
		this.source.autoSendPriorityActions(true);
		this.source.autoSyncInvalidatedThings(true);
		this.source.errorMonitor(e -> {
			// Check if anything triggered a revoked token error
			SyncException se = SyncException.unwrap(e);
			if (se == null) return;
			boolean revoked = false;
			if (ApiException.unwrapType(se) == ApiException.Type.POCKET_ACCESS_TOKEN_REVOKED
					|| ApiException.unwrapType(se.result.result_t.cause) == ApiException.Type.POCKET_ACCESS_TOKEN_REVOKED) {
				revoked = true;
			} else {
				for (Result r : se.result.result_a.values()) {
					if (ApiException.unwrapType(r.cause) == ApiException.Type.POCKET_ACCESS_TOKEN_REVOKED) {
						revoked = true;
						break;
					}
				}
			}
			if (revoked) {
				try {
					user().logout(LogoutReason.ACCESS_TOKEN_REVOKED);
				} catch (SyncException t) {
					throw new RuntimeException(t);
				}
			}
		});
	}
	
	/**
	 * Use this method rather than accessing {@link #source} directly,
	 * as it ensures some internal initialization has completed before general usage.
	 */
	private synchronized PocketSource source() {
		if (!initialized) {
			initialized = true;
			source.transaction(space -> {
				Holder holder = Holder.persistent("auth");

				// Setup credentials
				LoginInfo loginInfo = spec().things().loginInfo().build();
				space.remember(holder, loginInfo); // Remember early on, so this covers migrations
				space.initialize(loginInfo);
				updateCredentials(space.get(loginInfo));
				// Listen for future changes and respond immediately so changes are applied before any PendingResult.get() releases.
				subscribe(Changes.of(loginInfo), new PublishingSubscriber<>(this::updateCredentials, Publisher.CALLING_THREAD));
				// Also remember guid, for anyone getting it directly, instead of from LoginInfo.
				space.remember(holder, spec().things().guid().build());

				// Many remote actions in the Applier invalidate this Thing in order to flag
				// that the account info should be updated, if it isn't remembered it has no effect.
				// This does also mean implementations must be careful to use syncRemote()
				// otherwise they will get our local cache.
				Getuser getuser = AccountUtil.getuser(spec());
				space.remember(holder, getuser); 
				space.initialize(getuser);
			});
		}
		return source;
	}
	
	private synchronized void updateCredentials(LoginInfo info) {
		source().setCredentials(new Credentials(info.access_token != null ? info.access_token.value : null, info.guid, config.device, config.app));
		config.remote.setMaxActions(Safe.value(info.maxActions));
	}
	
	@Override
	public PocketSpec spec() {
		return spec;
	}
	
	@Override
	public <T extends Thing> PendingResult<T, SyncException> sync(T thing, Action... actions) {
		return source().sync(blockLogins(thing), actions);
	}
	
	@Override
	public <T extends Thing> PendingResult<T, SyncException> syncRemote(T thing, Action... actions) {
		return source().syncRemote(blockLogins(thing), actions);
	}
	
	@Override
	public <T extends Thing> PendingResult<T, SyncException> syncLocal(T thing, Action... actions) {
		return source().syncLocal(blockLogins(thing), actions);
	}
	
	@Override
	public PendingResult<Void, SyncException> syncActions(RemotePriority type) {
		return source().syncActions(type);
	}
	
	/** Throw an exception if the thing is a login request. */
	private <T extends Thing> T blockLogins(T thing) {
		if (thing != null && thing.auth() == PocketAuthType.LOGIN) throw new RuntimeException(thing.type() + " is not permitted. Use Pocket.user() instead.");
		return thing;
	}
	
	/**
	 * Note: Subscriptions are automatically stopped and removed on {@link UserApi#logout()}. See {@link #setup(SetupStep)} for help if you need a more permanent subscriber.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public <T extends Thing> Subscription subscribe(Changes<T> change, Subscriber<T> sub) {
		return source().subscribe(change, sub);
	}
	
	@Override
	public <T extends Thing> Subscription bindLocal(T thing, Subscriber<T> subscriber, BindingErrorCallback onFailure) {
		return source().bindLocal(thing, subscriber, onFailure);
	}
	
	@Override
	public <T extends Thing> Subscription bind(T thing, Subscriber<T> subscriber, BindingErrorCallback onFailure) {
		return source().bind(thing, subscriber, onFailure);
	}

	public <T extends Thing> Subscription bind(boolean forceRemote, T thing, Subscriber<T> subscriber, BindingErrorCallback onFailure) {
		return source().bind(forceRemote, thing, subscriber, onFailure);
	}
	
	/**
	 * Note: Holds are automatically cleared on {@link UserApi#logout()}. See {@link #setup(SetupStep)} for help if you need a more permanent hold.
	 * {@inheritDoc}
	 */
	@Override
	public PendingResult<Void, Throwable> remember(Holder holder, Thing... identities) {
		return source().remember(holder, identities);
	}
	
	@Override
	public PendingResult<Void, Throwable> forget(Holder holder, Thing... identities) {
		return source().forget(holder, identities);
	}
	
	/**
	 * Note: Holds are automatically cleared on {@link UserApi#logout()}. See {@link #setup(SetupStep)} for help if you need a more permanent hold.
	 * {@inheritDoc}
	 */
	@Override
	public PendingResult<Void, Throwable> initialize(Thing thing) {
		return source().initialize(thing);
	}
	
	@Override
	public PendingResult<boolean[], Throwable> contains(String... idkeys) {
		return source().contains(idkeys);
	}
	
	@Override
	public PendingResult<boolean[], Throwable> contains(Thing... things) {
		return source().contains(things);
	}
	
	@Override
	public PendingResult<Void, Throwable> await() {
		return source().await();
	}
	
	/**
	 * Register some code to run now as well as immediately after logging out.
	 * This is a helper for initialization work your feature or component might want
	 * to have always attached to the pocket instance.
	 * <p>
	 * Since all subscriptions are stopped when the instance logs out you'd typically
	 * need to resetup holders and subscriptions on logout.
	 * This is a convenience so you can do both the startup and log out init in one call.
	 *
	 * @param step The work to do now and later if the instance is logged out.
	 *             This should not be long running. If you need it to be long running, use this to kick off an async process.
	 *             Any errors this throws will be ignored. If you want to catch those errors you must catch and handle it yourself.
	 */
	public void setup(SetupStep step) {
		try {
			step.setup();
		} catch (Throwable ignore) {}
		
		synchronized (this) {
			setupSteps.add(step);
		}
	}

	/**
	 * This is only provided for some debugging and logging during the initial sync rollout.
	 * It is expected to be removed in a follow up release. If we decide to keep this as an official api,
	 * then should implement it properly like other methods accessing the space via source() apis instead of directly.
	 * @param type
	 * @return
	 */
	@Deprecated
	public int count(String type) {
		return config.space.count(type); // TODO if we keep this API, handle properly
	}
	
	public interface SetupStep {
		/** See {@link Pocket#setup(SetupStep)} */
		void setup();
	}
	
	/** @return APIs to manage the logged in user. */
	public UserApi user() {
		return account;
	}
	
	/**
	 * A helper for managing which user is logged in.
	 * All methods in this API are blocking operations.
	 */
	public class UserApi {
		
		private OnLoginEventListener onLoginEvent;
		
		/**
		 * A blocking call to check if a user is logged in.
		 * @return true if there is a user logged in.
		 * @throws SyncException If there was an error checking.
		 */
		public synchronized boolean isLoggedIn() throws SyncException {
			return sync(spec.things().loginInfo().build()).get().access_token != null;
		}
		
		/**
		 * Log in a user by username.
		 * This is a blocking call.
		 * @throws SyncException If it fails or if a user is already logged in.
		 */
		@VisibleForTesting
		public synchronized void login(String usernameOrEmail, String password, AuthenticationExtras extras) throws SyncException {
			if (isLoggedIn()) throw new SyncException(null, "Already logged in");
			
			usernameOrEmail = StringUtils.defaultString(usernameOrEmail);  // REVIEW do we need this? what about trimming?
			password = StringUtils.defaultString(password);
			
			OAuthAuthorize.Builder builder = spec().things().oAuthAuthorize()
					.username(usernameOrEmail)
					.password(new Password(password))
					.grant_type("credentials")
					.timezone(extras.timezoneOffset)
					.include_account(true)
					.device_manuf(config.device.deviceManufactuer)
					.device_model(config.device.deviceModel)
					.device_product(config.device.deviceProduct)
					.device_anid(extras.anid)
					.device_sid(extras.sid);
			if (extras.referrer != null) {
				builder.play_referrer(extras.referrer);
			}
			
			OAuthAuthorize auth = source().syncRemote(builder.build()).get();
			updateLoggedInAccount(auth.access_token, auth.account, AuthMethod.POCKET, auth.prompt_password, auth.premium_gift, false);
		}

		/**
		 * Log in using an already known access token such as when logging in with Apple via v3/apple/android/connect
		 * This is a blocking call.
		 * @throws SyncException If it fails or if a user is already logged in.
		 */
		public synchronized void loginWithAccessToken(String accessToken, AuthenticationExtras extras) throws SyncException {
			if (StringUtils.isBlank(accessToken)) throw new SyncException(null, "Missing access token");
			LoginInfo info = sync(spec.things().loginInfo().build()).get();
			if (info != null && info.access_token != null) throw new SyncException(null, "Already logged in");

			GetAfterLogin.Builder builder = spec().things().getAfterLogin()
					.timezone(extras.timezoneOffset)
					.include_account(true)
					.device_manuf(config.device.deviceManufactuer)
					.device_model(config.device.deviceModel)
					.device_product(config.device.deviceProduct)
					.device_anid(extras.anid)
					.device_sid(extras.sid);
			if (extras.referrer != null) {
				builder.play_referrer(extras.referrer);
			}
			
			String guid = info != null ? info.guid : null;
			try {
				// Setup the access token and try the request
				source().setCredentials(new Credentials(accessToken, guid, config.device, config.app));
				GetAfterLogin auth = source().syncRemote(builder.build()).get();
				updateLoggedInAccount(new AccessToken(accessToken), auth.account, AuthMethod.POCKET, auth.prompt_password, auth.premium_gift, false);
			} catch (Throwable t) {
				// Reset back to not having an access token
				source().setCredentials(new Credentials(null, guid, config.device, config.app));
				throw new SyncException(null, t);
			}
		}

		private synchronized void updateLoggedInAccount(AccessToken access_token, Account account, AuthMethod method, Boolean prompt_password, PremiumGift premium_gift, boolean wasSignup) throws SyncException {
			// Currently the server has a bug where this can happen sometimes, but a good check regardless.
			if (access_token == null || StringUtils.isBlank(access_token.value) || account == null) {
				throw new SyncException(null, "Missing account info");
			}
			
			LoginInfo info = new LoginInfo.Builder()
					.access_token(access_token)
					.account(account)
					.wasSignup(wasSignup)
					.authMethod(method)
					.prompt_password(prompt_password)
					.premium_gift(premium_gift)
					.build();
			source().sync(null, spec().actions().update_logged_in_account().time(Timestamp.now()).info(info).build()).get();
			
			if (onLoginEvent != null) onLoginEvent.onLoginEvent(LoginEvent.LOGGED_IN);
		}

		/**
		 * Makes an attempt to send all pending actions to the server and then clears all data from this source. Including:
		 * <ul>
		 *     <li>The logged in user and their access token, account info.</li>
		 *     <li>The {@link Guid}</li>
		 *     <li>All holds made through {@link #remember(Holder, Thing...)}</li>
		 *     <li>Stops and removes all subscriptions made through {@link #subscribe(Changes, Subscriber)} and bind methods</li>
		 *     <li>All remembered things</li>
		 *     <li>All pending actions that haven't been sent to the server yet</li>
		 * </ul>
		 * This returns this instance to a logged out state.
		 * <p>
		 * If it fails to send pending actions, it will continue with the logout regardless. If you need to guarantee all actions
		 * have successfully been sent, invoke an empty {@link #syncRemote(Thing, Action...)} before logging out.
		 */
		public synchronized void logout() throws SyncException {
			logout(LogoutReason.USER);
		}
		
		private synchronized void logout(LogoutReason reason) throws SyncException {
			// Attempt to stop and await all work
			// TODO might be nice to have an abandon all like method on AppSource?
			source().stopAllSubscriptions();
			// Make a single attempt to send any pending actions before clearing.
			try {
				syncActions(null).get();
			} catch (SyncException ignore) {}
			source().await(); // Make a best attempt to avoid race conditions before the logout below action nukes everything. If we find race condition cases here, we could refine this
			sync(null, spec().actions().logout().reason(reason).time(Timestamp.now()).build()).get();
			initialized = false;
			for (SetupStep step : setupSteps) {
				try {
					step.setup();
				} catch (Throwable ignore){}
			}
			if (onLoginEvent != null) onLoginEvent.onLoginEvent(reason == LogoutReason.ACCESS_TOKEN_REVOKED ? LoginEvent.LOGGED_OUT_TOKEN_WAS_REVOKED : LoginEvent.LOGGED_OUT);
		}

		/** Listen for changes to login/logout states. */
		public void setLoginListener(OnLoginEventListener listener) {
			this.onLoginEvent = listener;
		}
		
	}
	
	public enum LoginEvent {
		LOGGED_IN,
		LOGGED_OUT,
		/** The user was logged out because some response from the v3 api indicated the token was revoked. See {@link ApiException.Type#POCKET_ACCESS_TOKEN_REVOKED#}*/
		LOGGED_OUT_TOKEN_WAS_REVOKED
	}
	
	public interface OnLoginEventListener {
		void onLoginEvent(LoginEvent event);
	}
	
	/**
	 * Additional information that the Pocket authentication endpoints ask for.
	 */
	public static class AuthenticationExtras {
		
		/** See {@link OAuthAuthorize#device_anid} {@link OAuthSsoauth#device_anid} or {@link Signup#device_anid} */
		public final String anid;
		/** See {@link OAuthAuthorize#device_sid} {@link OAuthSsoauth#device_sid} or {@link Signup#device_sid} */
		public final String sid;
		/** See {@link OAuthAuthorize#timezone} {@link OAuthSsoauth#timezone} or {@link Signup#timezone} */
		public final String timezoneOffset;
		/** See {@link OAuthAuthorize#play_referrer} {@link OAuthSsoauth#play_referrer} or {@link Signup#play_referrer} */
		public final String referrer;
		
		public AuthenticationExtras(String referrer, String anid, String sid) {
			this.anid = anid;
			this.sid = sid;
			this.timezoneOffset = String.valueOf( Calendar.getInstance().getTimeZone().getOffset(System.currentTimeMillis()) / 1000 / 60 );
			this.referrer = referrer;
		}
		
	}
	
	/**
	 * Configuration of a {@link Pocket} instance.
	 */
	public static class Config {

		public static class Builder {
			
			protected final AppInfo app;
			protected final DeviceInfo device;
			protected final String name;
			
			protected PocketSpec spec;
			protected Space space;
			protected Publisher publisher;
			protected PocketRemoteSource remote;
			protected MigrationStorage.Type migration;
			protected ThreadPools threads;

			/**
			 * A new config builder with the default settings for a pure java environment.
			 */
			public Builder(String name, AppInfo app, DeviceInfo device) {
				this.name = name;
				this.app = app;
				this.device = device;
			}
			
			/**
			 * @return A new instance. Any values not set will use built in defaults.
			 */
			public Config build() {
				// Ensure defaults
				if (spec == null) spec = new PocketSpec();
				if (remote == null) remote = new PocketRemoteSource(new EclecticOkHttpClient(new OkHttpClient()));
				
				// Setup for Java
				if (publisher == null) publisher = new ThreadPublisher();
				if (threads == null) threads = new JavaThreadPools();
				if (space == null) space = new MutableSpace();
				
				return new Config(this);
			}
			
			/**
			 * Override the default persistence implementation.
			 * @param space
			 * @return
			 */
			public Builder space(Space space) {
				this.space = space;
				return this;
			}
			
			/**
			 * Override the default {@link ThreadPools} implementation.
			 * @param threads
			 * @return
			 */
			public Builder threads(ThreadPools threads) {
				this.threads = threads;
				return this;
			}
			
			/**
			 * Override the default {@link Publisher} for async callbacks
			 * @param publisher
			 * @return
			 */
			public Builder publisher(Publisher publisher) {
				this.publisher = publisher;
				return this;
			}
			
			/** Override the {@link Source} that connects with the Pocket server. */
			public Builder remote(PocketRemoteSource remote) {
				this.remote = remote;
				return this;
			}
			
			/**
			 * Override the {@link PocketSpec}.
			 * @param spec
			 * @return
			 */
			public Builder spec(PocketSpec spec) {
				this.spec = spec;
				return this;
			}
			
			/**
			 * If non null, when setting up the default space, it will wrap it in a {@link com.pocket.sync.space.persist.MigrationStorage}
			 * and use this as the migration logic.  This will only have an effect if the space implementation is default and for Android configs.
			 */
			public Builder migration(MigrationStorage.Type migration) {
				this.migration = migration;
				return this;
			}
			
		}
		
		public final PocketSpec spec;
		public final AppInfo app;
		public final DeviceInfo device;
		public final Space space;
		public final Publisher publisher;
		public final ThreadPools threads;
		public final PocketRemoteSource remote;
		
		public Config(Builder builder) {
			spec = builder.spec;
			app = builder.app;
			device = builder.device;
			space = builder.space;
			publisher = builder.publisher;
			remote = builder.remote;
			threads = builder.threads;
		}
		
	}
	
}
