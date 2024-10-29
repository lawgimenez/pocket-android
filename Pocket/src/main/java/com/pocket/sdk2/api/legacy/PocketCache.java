package com.pocket.sdk2.api.legacy;

import android.content.Context;

import com.pocket.app.AppLifecycle;
import com.pocket.app.AppLifecycleEventDispatcher;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.Modeller;
import com.pocket.sdk.api.generated.enums.PremiumAllTimeStatus;
import com.pocket.sdk.api.generated.enums.PremiumFeature;
import com.pocket.sdk.api.generated.thing.Account;
import com.pocket.sdk.api.generated.thing.LoginInfo;
import com.pocket.sdk.preferences.AppPrefs;
import com.pocket.sync.source.JsonConfig;
import com.pocket.sync.source.subscribe.PublishingSubscriber;
import com.pocket.sync.source.subscribe.Subscriber;
import com.pocket.sync.source.threads.Publisher;
import com.pocket.sync.spec.Syncable;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.Include;
import com.pocket.sync.value.protect.StringEncrypter;
import com.pocket.sync.value.protect.TinkEncrypter;
import com.pocket.util.java.Safe;
import com.pocket.util.prefs.StringPreference;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Ideally, no UI or feature code should expect instant access to data or persisted state.
 * UI or feature code should either have data passed to it (bound) or load state asynchronously.
 * Because ultimately, if you are loading persisted data it is coming from disk in some way.
 * <p>
 * However, there are many places in our app that break this general idea and were built with the expectation
 * that it could obtain certain state quickly, on the ui thread. This is left over from some older components and views.
 * <p>
 * As part of transitioning to {@link Pocket}, it was out of scope to try to reimagine how
 * much of the app expects to get state, so this class is a temporary bridge between those worlds
 * and offers the state it wants immediately, caching it from {@link Pocket}.
 * <p>
 * We should move away from this and try to build UI and feature code that, like mentioned above,
 * is either passed the state it should bind/display, or it should be built with the expectation
 * that retrieving the state is asynchronous and load it from {@link Pocket} directly.
 * <p>
 * These values are only cached and could be out of date in certain race conditions.
 * An example would be accessing this during a {@link Subscriber#onUpdate(Thing)},
 * there is no guarantee of order of which subscribers are called, so if your subscriber is called before this
 * cache has its own onUpdate callback (which updates the cache), then you could be accessing old data.
 * There are likely other race conditions.
 */
@Deprecated
@Singleton
public class PocketCache implements AppLifecycle {

	/** No special Json parsing configuration rules are required here */
	private static final JsonConfig JSON_CONFIG = Syncable.NO_ALIASES;

	private final StringPreference store;
	private StringEncrypter encrypter;

	/** Access through {@link #cached()} to ensure initialized. */
	private LoginInfo cached;

	@Inject
	public PocketCache(
			Pocket pocket,
			@ApplicationContext Context context,
			AppPrefs prefs,
			@Nullable LegacyMigration legacy,
			AppLifecycleEventDispatcher dispatcher
	) {
		dispatcher.registerAppLifecycleObserver(this);
		this.store = prefs.PKT_CACHE;
		encrypter = new TinkEncrypter(context, "pktcache");
		if (legacy != null) updateCache(legacy.loginInfo());
		pocket.setup(() -> pocket.bind(pocket.spec().things().loginInfo().build(), new PublishingSubscriber<>(this::updateCache, Publisher.CALLING_THREAD), null));
	}
	
	@Override
	public LogoutPolicy onLogoutStarted() {
		return new LogoutPolicy() {
			@Override public void stopModifyingUserData() {}
			
			@Override
			public void deleteUserData() {
				store.set(null);
				cached = null;
			}
			
			@Override public void restart() {}

			@Override public void onLoggedOut() {}
		};
	}
	
	private synchronized void updateCache(LoginInfo i) {
		cached = i;
		store.set(encrypter.encrypt(i.toJson(JSON_CONFIG, Include.DANGEROUS).toString())); // Since it contains access tokens and other potential personal or sensitive info, just encrypt the whole thing, rather than picking and choosing fields.
	}
	
	private synchronized LoginInfo cached() {
		if (cached == null) {
			String cache = store.get();
			LoginInfo i = LoginInfo.from(Modeller.toObjectNode(cache != null ? encrypter.decrypt(cache) : null), JSON_CONFIG);
			if (i != null) {
				cached = i;
			} else {
				updateCache(new LoginInfo.Builder().build());
			}
		}
		return cached;
	}
	
	public synchronized boolean hasPremium() {
		return Safe.getBoolean(() -> cached().account.premium_status);
	}
	
	public synchronized PremiumAllTimeStatus premium_alltime_status() {
		return Safe.get(() -> cached().account.premium_alltime_status);
	}
	
	public synchronized boolean hasPremiumTrial() {
		return hasPremium() && Safe.getBoolean(() -> cached().account.premium_on_trial);
	}
	
	public synchronized int annotations_per_article_limit() {
		int value = Safe.getInt(() -> cached().account.annotations_per_article_limit);
		if (value <= 0) return 3;
		return value;
	}
	
	public synchronized boolean isLoggedIn() {
		return cached().access_token != null;
	}
	
	public synchronized LoginInfo loginInfo() {
		return cached();
	}

	public synchronized Account account() {
		return cached().account;
	}
	
	public synchronized String getEmail() {
		return Safe.get(() -> cached().account.email);
	}
	
	public synchronized String getUsername() {
		return Safe.get(() -> cached().account.username);
	}
	
	public synchronized boolean hasUserSetUsername() {
		return hasUserSetUsername(getUsername());
	}
	
	public static boolean hasUserSetUsername(String username) {
		return !StringUtils.startsWith(username, "*");
	}
	
	public synchronized boolean wasSignup() {
		return Safe.value(cached().wasSignup);
	}
	
	public synchronized boolean hasFeature(PremiumFeature feature) {
		return Safe.getBoolean(() -> cached().account.premium_features.contains(feature));
	}
	
	public synchronized String getUID() {
		return Safe.get(() -> cached().account.user_id);
	}

	public synchronized boolean hasPremiumAndPaid() {
		return hasPremium() && !hasPremiumTrial();
	}
}
