package com.pocket.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.ideashower.readitlater.R;
import com.pocket.app.auth.AuthenticationActivity;
import com.pocket.sdk.AndroidPocket;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.AppSync;
import com.pocket.sdk.api.generated.thing.Guid;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.dev.ErrorHandler;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sdk.util.dialog.AlertMessaging;
import com.pocket.sdk2.api.legacy.PocketCache;
import com.pocket.sync.source.PendingResult;
import com.pocket.sync.source.result.SyncException;
import com.pocket.sync.source.threads.AndroidUiThreadPublisher;
import com.pocket.sync.source.threads.PendingImpl;
import com.pocket.sync.thing.Thing;
import com.pocket.util.android.thread.TaskPool;
import com.pocket.util.java.Logs;
import com.pocket.util.prefs.BooleanPreference;
import com.pocket.util.prefs.Preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Manages which user account is logged into the app.
 * <p>
 * For the app, a "login" and a "logout" is a lot more than just what the {@link Pocket} instance takes care of.
 * So this will manage the login/signup/logout methods of {@link Pocket#user()} on behalf of the app
 * and dispatch login/logout events to the rest of the app to make sure all of the app's
 * state and ui smoothly moves between logged in/out states.
 * <p>
 * App components should use this class to login, signup or logout instead of hitting the similar methods in {@link Pocket#user()}.
 * App components can and should still access account information from the pocket instance, just don't perform login, signup or logout outside of this class.
 * <p>
 * To login, use {@link #authenticate(AuthOperation, OnAuthSuccess, OnAuthFail)}
 * To logout, use {@link #logout(AbsPocketActivity)}
 * To react to login or logout events, see login and logout events in {@link AppLifecycle}.
 */
@Singleton
public class UserManager implements AppLifecycle {
	
	private final Pocket pocket;
	private final AppMode mode;
	private final AppThreads threads;
	private final AppScope scope;
	private final PocketCache pktcache;
	private final ActivityMonitor activities;
	private final Context context;
	private final AppLifecycleEventDispatcher dispatcher;
	private final InstallReferrer referrer;
	private final Device device;
	private final Preferences prefs;
	private final BooleanPreference signedOutExperienceEnabled;
	private final BooleanPreference hadBadCredentials;
	private final BooleanPreference hasDeletedAccount;
	private final ErrorHandler errorHandler;
	
	private boolean isLoggingOut;
	private boolean isStoppingData;
	private boolean isModifyingLogin;

	@Inject
	public UserManager(
			Pocket pocket,
			AppMode mode,
			AppThreads threads,
			AppScope scope,
			AppSync appsync,
			PocketCache pktcache,
			ActivityMonitor activities,
			@ApplicationContext Context context,
			AppLifecycleEventDispatcher dispatcher,
			Preferences prefs,
			Device device,
			InstallReferrer referrer,
			ErrorHandler errorHandler
	) {
		dispatcher.registerAppLifecycleObserver(this);
		this.pocket = pocket;
		this.mode = mode;
		this.threads = threads;
		this.scope = scope;
		this.pktcache = pktcache;
		this.activities = activities;
		this.context = context;
		this.dispatcher = dispatcher;
		this.referrer = referrer;
		this.device = device;
		this.signedOutExperienceEnabled = prefs.forApp("noaccntxp", false);
		this.hadBadCredentials = prefs.forApp("invalidcred", false);
		this.hasDeletedAccount = prefs.forApp("deltdaccnt", false);
		this.prefs = prefs; // Keep a reference since we need special handling at logout
		this.errorHandler = errorHandler;
		
		pocket.user().setLoginListener(event -> {
			switch (event) {
				case LOGGED_IN:
				case LOGGED_OUT:
					if (!isModifyingLogin && mode.isForInternalCompanyOnly()) {
						throw new RuntimeException("Login modified outside of " + UserManager.class.getName());
					}
					break;
				case LOGGED_OUT_TOKEN_WAS_REVOKED:
					threads.runOrPostOnUiThread(() -> {
						hadBadCredentials.set(true);
						logout();
					});
					break;
			}
		});
		
		appsync.addFlags(g -> g.include_account(1));
		appsync.addInitialFlags(g -> g.forceaccount(1));
	}
	
	/**
	 * Asynchronously log the app into a specific user's Pocket account.
	 *
	 * @param operation A lambda that will perform the {@link Pocket.UserApi} login or signup method you want to use. Within this you should only use the provided api to hit one login or signup method and use the {@link com.pocket.sdk.Pocket.AuthenticationExtras} provided to it.
	 * @param success A callback if successful
	 * @param failure A callback if this fails
	 */
	public void authenticate(AuthOperation operation, OnAuthSuccess success, OnAuthFail failure) {
		threads.async(() -> {
			Pocket.AuthenticationExtras extras = AndroidPocket.authenticationExtras(context, referrer.getGooglePlayReferrer());
			if (mode.isForInternalCompanyOnly()) {
				// There is an Alpha setting that allows overriding these values for testing.
				extras = new Pocket.AuthenticationExtras(extras.referrer, device.anid(), device.sid());
			}
			
			isModifyingLogin = true;
			operation.authenticate(pocket.user(), extras);
			isModifyingLogin = false;
			
			if (pktcache.isLoggedIn()) {
				boolean isNewUser = pktcache.wasSignup();
				dispatcher.dispatch((component) -> {
					try {
						component.onLoggingIn(isNewUser);
					} catch (Throwable t) {
						// Don't allow errors here to break the login process.
						Logs.printStackTrace(t);
					}
				});
				
				threads.runOrPostOnUiThread(() -> {
					success.onAuthSuccess();
					dispatcher.dispatch((component) -> component.onLoggedIn(isNewUser));
				});
			} else {
				try {
					// This is not expected, but attempt to clear the pocket state back to what it was before login. We'll maybe lose some analytic actions, but the important thing is to get the user logged in.
					Guid guid = pocket.syncLocal(pocket.spec().things().guid().build()).get();
					isModifyingLogin = true;
					pocket.user().logout();
					isModifyingLogin = false;
					if (guid != null && guid.guid != null) pocket.initialize(guid);
					throw new RuntimeException("Not logged in.");
				} catch (Throwable t) {
					// INCEPTION NOISE PLAYS
					// we are getting really deep into errors here, this state is very unexpected but the best we can do is fail here and hope for the best on next retry.
					throw new RuntimeException(t);
				}
			}
		},
		e -> {
			Logs.printStackTrace(e);
			threads.runOrPostOnUiThread(() -> failure.onAuthFail(e));
		});
	}
	
	public interface AuthOperation {
		/**
		 * Invoke one of the login and signup methods on the userApi, whichever you want to use for your case. Use the provided extras.
		 * Do not catch any exceptions here or do anything else, the {@link #authenticate(AuthOperation, OnAuthSuccess, OnAuthFail)} implementation will handle the response and any errors.
		 */
		void authenticate(Pocket.UserApi userApi, Pocket.AuthenticationExtras extras) throws Exception;
	}
	
	public interface OnAuthSuccess {
		void onAuthSuccess();
	}
	
	public interface OnAuthFail {
		void onAuthFail(Throwable error);
	}
	
	/**
	 * Logout, removing all user data and reseting the app back to the logged out state.
	 * @param activity If available, the current screen and the login page will be opened when complete. If not provided, logout will happen in the background.
	 */
	public void logout(final AbsPocketActivity activity) {
		if (isLoggingOut || !pktcache.isLoggedIn()) {
			return;
		}
		isLoggingOut = true;

		final ProgressDialog progressDialog;
		if (activity != null) {
			// Revert the activity to a splash screen appearance.
			activity.getWindow().setBackgroundDrawableResource(R.drawable.splash_window_bg);
			activity.hideContent();
			activity.getPocketFragmentManager().removeAllFragments(); // REVIEW doesn't hide the settings dialog fragment on tablets
			progressDialog = ProgressDialog.show(activity, null, activity.getString(R.string.dg_logging_out), true, false);
		} else {
			progressDialog = null;
		}
		
		List<LogoutPolicy> policies = new ArrayList<>();
		dispatcher.dispatch((component) -> {
			LogoutPolicy policy = component.onLogoutStarted();
			if (policy != null) policies.add(policy);
		});
		
		// Perform Stop modifying data requests off the ui thread in a thread pool
		isStoppingData = true;
		TaskPool pool = threads.newWakefulPool("logout", 5);
		CountDownLatch latch = new CountDownLatch(policies.size());
		for (LogoutPolicy policy : policies) {
			pool.submit(() -> {
				tryYourBest(policy::stopModifyingUserData);
				latch.countDown();
			});
		}
		
		// Await those to be complete
		pool.submit(() -> {
			try {
				latch.await();
			} catch (InterruptedException ignore) {}
			pocket.await();
			
			// Delete data
			for (LogoutPolicy policy : policies) {
				tryYourBest(policy::deleteUserData);
			}
			
			// Log out of the Pocket instance
			isModifyingLogin = true;
			try {
				pocket.user().logout();
			} catch (SyncException unexpected) {
				throw new RuntimeException(unexpected);
			}
			isModifyingLogin = false;
			
			// Now kill any remaining tasks running
			// To be really strict, we could consider moving this up before the delete your data step and change AppThreads to reject any calls until restarted.
			LogoutPolicy appThreads = threads.getLogoutPolicy();
			appThreads.stopModifyingUserData();
			appThreads.deleteUserData();
			LogoutPolicy appScope = scope.getLogoutPolicy();
			appScope.stopModifyingUserData();
			appScope.deleteUserData();
			
			// Jump back to the UI thread to wrap up
			threads.postOnUiThread(() -> {
				tryYourBest(pool::shutdown);
				// Delete some further user state
				context.deleteDatabase("webview.db"); // REVIEW these don't appear to be right anymore on newer versions of android
				context.deleteDatabase("webviewCache.db");
				prefs.clearUser();
				isStoppingData = false;
				
				// Restart Delegates
				appThreads.restart();
				appScope.restart();
				for (LogoutPolicy policy : policies) {
					policy.restart();
				}
				
				boolean userIsStillPresent = activity != null && !activity.isFinishing();
				AlertMessaging.dismissSafely(progressDialog, activity);

				// Kill all activities
				Intent broadcastIntent = new Intent();
				broadcastIntent.setAction(AbsPocketActivity.ACTION_LOGOUT);
				LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);

				// Restart at the default Activity if they were in the app
				if (activity != null) {
					activity.finishAffinity();
					if (userIsStillPresent) {
						activity.startDefaultActivity();
						activity.overridePendingTransition(0, 0);
					}
				}

				appThreads.onLoggedOut();
				for (LogoutPolicy policy : policies) {
					policy.onLoggedOut();
				}
				isLoggingOut = false;
			});
		});
	}

	private void logout() {
		logout(AbsPocketActivity.from(activities.getVisible()));
	}

	public PendingResult<Thing, SyncException> deleteAccount() {
		var proxy = new PendingImpl<Thing, SyncException>(new AndroidUiThreadPublisher());
		var pendingDelete = pocket.syncRemote(
				null,
				pocket.spec().actions().deleteUser().time(Timestamp.now()).build()
		);
		proxy.proxy(pendingDelete);
		pendingDelete
				.onSuccess(it -> {
					proxy.success(it);
					hasDeletedAccount.set(true);
					logout();
				})
				.onFailure(proxy::fail);
		return proxy;
	}

	public boolean hasDeletedAccount() {
		return hasDeletedAccount.get();
	}

	public void onShowedDeletedAccountToast() {
		hasDeletedAccount.set(false);
	}

	public boolean hadBadCredentials() {
		return hadBadCredentials.get();
	}

	public void onShowedBadCredentialsMessage() {
		hadBadCredentials.set(false);
	}

	public void enableSignedOutExperience() {
		signedOutExperienceEnabled.set(true);
	}

	public Class<? extends Activity> getDefaultActivity() {
		if (pktcache.isLoggedIn()) {
			return MainActivity.class;
		} else if (signedOutExperienceEnabled.get()) {
			return MainActivity.class;
		} else {
			return AuthenticationActivity.class;
		}
	}

	@Override
	public void onActivityResumed(Activity activity) {
		if (hadBadCredentials.get()) {
			if ((activity instanceof AuthenticationActivity)) {
				hadBadCredentials.set(false);
				new AlertDialog.Builder(activity)
						.setTitle(R.string.dg_forced_logout_t)
						.setMessage(R.string.dg_forced_logout_m)
						.setPositiveButton(R.string.ac_ok, null)
						.show();
			}
		}
	}
	
	interface ThrowingRunnable {
		void run() throws Throwable;
	}
	
	/** Ignores (but reports) any exceptions thrown. */
	private void tryYourBest(ThrowingRunnable r) {
		try {
			r.run();
		} catch (Throwable t) {
			errorHandler.reportError(t);
		}
	}
	
	/** @return true if in the middle of the stop data part of a logout process. */
	public boolean isStoppingData() {
		return isStoppingData;
	}

}
