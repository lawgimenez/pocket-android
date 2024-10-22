package com.pocket.sdk.api;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.NetworkType;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pocket.app.App;
import com.pocket.app.AppLifecycle;
import com.pocket.app.AppLifecycleEventDispatcher;
import com.pocket.app.AppThreads;
import com.pocket.app.ItemCap;
import com.pocket.app.Jobs;
import com.pocket.app.build.Versioning;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.PocketActions;
import com.pocket.sdk.api.generated.PocketThings;
import com.pocket.sdk.api.generated.thing.Fetch;
import com.pocket.sdk.api.generated.thing.Get;
import com.pocket.sdk.api.generated.thing.LocalItems;
import com.pocket.sdk.api.generated.thing.LoginInfo;
import com.pocket.sdk.api.generated.thing.SyncState;
import com.pocket.sdk.api.thing.GetUtil;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.http.NetworkStatus;
import com.pocket.sdk2.api.legacy.PocketCache;
import com.pocket.sync.source.PendingResult;
import com.pocket.sync.source.result.RemotePriority;
import com.pocket.sync.source.result.SyncException;
import com.pocket.sync.space.Holder;
import com.pocket.util.android.thread.TaskRunnable;
import com.pocket.util.java.Logs;
import com.pocket.util.java.Safe;
import com.pocket.util.prefs.BooleanPreference;
import com.pocket.util.prefs.Preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;

/**
 * Keeps "My List", and other state as requested by components, regularly in sync with the remote server based on the user's preferences.
 * Handles syncing {@link Get} and {@link Fetch} for the app and will persist and maintain the following state:
 * <ul>
 *     <li>The user's unread items, "My List" - {@link com.pocket.sdk.api.generated.thing.Saves}</li>
 *     <li>The user's tag list - {@link com.pocket.sdk.api.generated.thing.Tags}</li>
 *     <li>The user's recent searches - {@link com.pocket.sdk.api.generated.thing.RecentSearches}</li>
 *     <li>Any {@link com.pocket.sdk.api.generated.thing.Group} needed to display My List - {@link com.pocket.sdk.api.generated.thing.Groups}</li>
 * </ul>
 * Other components can use this to schedule their regular remote syncing work at times that will best fit the user's preferences:
 * <ul>
 *     <li>{@link #addInitialFlags(GetFlags)} and {@link #addFlags(GetFlags)} : Lets you add additional flags to the {@link Get} request this makes when syncing.</li>
 *     <li>{@link #addWork(SyncWork)} : Lets you perform your syncing work during the app's sync process.</li>
 * </ul>
 * Components can of course sync with the remote anytime they need using the {@link Pocket} APIs, but if components need to schedule
 * regular updates, this class can help do that at times the user wants.
 * <p>
 * There are a few different times and ways the app may perform this sync:
 * <ul>
 *     <li>Automatically after log in</li>
 *     <li>Automatically on app open (If they have the "Sync on open" setting on).</li>
 *     <li>Automatically based on {@link com.pocket.sdk.util.service.BackgroundSync} settings.</li>
 *     <li>Automatically sends important unsent actions when leaving the app, or when an internet connection becomes available</li>
 *     <li>Manually triggered through some UI, by invoking one of the {@link #sync()} methods.</li>
 * </ul>
 */
@Singleton
public class AppSync implements AppLifecycle {
	
	private static final boolean DEBUG = false;
	private static final String LOG_TAG = "AppSync";
	
	private final Holder holder = Holder.persistent("AppSync");
	private final ArrayList<SyncingListener> syncListeners = new ArrayList<>();
	private final ArrayList<SyncWork> syncWork = new ArrayList<>();
	private final ArrayList<Runnable> fetchWork = new ArrayList<>();
	private final ArrayList<GetFlags> flags = new ArrayList<>();
	private final ArrayList<GetFlags> firstFlags = new ArrayList<>();
	private final Object lock = new Object();
	private final PocketCache pktcache;
	private final Pocket pocket;
	private final AppThreads threads;
	private final BooleanPreference autoSync;
	/** To support early ui thread access of {@link #hasFetched()} we need to cache this value. This should match {@link SyncState#fetched}. */
	private final BooleanPreference hasFetched;
	
	private Sender sender;
	private SyncTask active;

	@Inject
	public AppSync(
			PocketCache pktcache,
			Pocket pocket,
			AppThreads threads,
			Jobs jobs,
			Preferences prefs,
			NetworkStatus networkStatus,
			ItemCap itemCap,
			Versioning versioning,
			AppLifecycleEventDispatcher dispatcher
	) {
		dispatcher.registerAppLifecycleObserver(this);
		this.pktcache = pktcache;
		this.pocket = pocket;
		this.threads = threads;
		this.hasFetched = prefs.forUser("hasFetched", false);
		this.autoSync = prefs.forUser("autoSync", true);
		this.sender = new Sender(pocket, jobs, networkStatus);
		
		pocket.setup(() -> {
			pocket.remember(holder, pocket.spec().things().syncState().build());
			pocket.initialize(pocket.spec().things().syncState().build());
		});
		// Setup some features. These could move to their own components if needed, but those components would only do this, so for now they are just here:
		// My List
		pocket.setup(() -> {
			LocalItems locals = pocket.spec().things().localItems().max(itemCap.getCap()).build();
			pocket.remember(holder, locals);
			pocket.initialize(locals);

			// A few releases back we made a change to how Item.domain is derived locally.
			// Unfortunately this doesn't magically rederive remembered Items. Which meant we
			// couldn't rely on the domain or display_domain to always be there.
			// To fix this, we'll have to do a one-time forced rederive of all local items.
			if (versioning.upgraded(7, 65, 0, 0)) {
				pocket.syncLocal(locals).onSuccess(remembered -> {
					if (remembered.items != null) {
						pocket.sync(
								null,
								pocket.spec().actions().rederive_items()
										.items(remembered.items)
										.time(Timestamp.now())
										.build()
						);
					}
				});
			}
		});
		// Recent Searches
		pocket.setup(() -> {
			pocket.remember(holder, pocket.spec().things().recentSearches().build());
			pocket.initialize(pocket.spec().things().recentSearches().build());
		});
		addInitialFlags(g -> g.forcepremium(1));
		addFlags(g -> g.premium(1));
		// Tag list
		pocket.setup(() -> {
			pocket.remember(holder, pocket.spec().things().tags().build());
			pocket.initialize(pocket.spec().things().tags().build());
		});
		addInitialFlags(g -> g.forcetaglist(1));
		addFlags(g -> g.taglist(1));
		// Groups
		pocket.setup(() -> {
			pocket.remember(holder, pocket.spec().things().groups().build());
			pocket.initialize(pocket.spec().things().groups().build());
		});
		addInitialFlags(g -> g.forcerediscovery(1));
		addFlags(g -> g.rediscovery(1));
	}
	
	@Override
	public void onUserPresent() {
		if (pktcache.isLoggedIn() && autoSync.get()) {
			sync();
		}
		sender.onUserPresent();
	}
	
	@Override
	public void onLoggedIn(boolean isNewUser) {
		sync();
	}
	
	@Override
	public void onUserGone(Context context) {
		sender.onUserGone();
	}
	
	/**
	 * Add additional work to do during what the app considers a "sync".
	 * See {@link SyncWork#sync(boolean, Get, LoginInfo)} for important details on usage.
	 * @see #addWork(Runnable) If you just want to use this as a hook to kick off some async work.
	 */
	public void addWork(SyncWork work) {
		synchronized (lock) {
			syncWork.add(work);
		}
	}
	
	/**
	 * If you just want to be notified of when a sync is happening and kick off some async work,
	 * you can use this variant to do so.
	 * @param hook A callback that will run during an app sync. <b>The same important rules in {@link SyncWork#sync(boolean, Get, LoginInfo)} apply here.</b>
	 */
	public void addWork(Runnable hook) {
		addWork((f, g, l) -> {
			hook.run();
			return null;
		});
	}
	
	/**
	 * Set some code to be executed now if {@link #hasFetched()} or in the future when/if fetching completes.
	 * Will be invoked each time fetching completes if the user logs in, out and back in for example.
	 * This should not be a long running operation! If you need to do blocking work use this as a hook to start work on another thread.
	 * Any errors thrown in this runnable will be caught and ignored.
	 */
	public void addFetchedWork(Runnable hook) {
		synchronized (lock) {
			fetchWork.add(hook);
		}
		if (hasFetched()) {
			dispatchEvent(false, Arrays.asList(hook), Runnable::run);
		}
	}
	
	/**
	 * Add flags to all {@link Get} requests made during syncing.
	 * See {@link GetFlags#flag(Get.Builder)} for more details.
	 */
	public void addFlags(GetFlags flagger) {
		synchronized (lock) {
			flags.add(flagger);
		}
	}
	
	/**
	 * Add flags to the initial {@link Get} during the fetch process.
	 * See {@link GetFlags#flag(Get.Builder)} for more details.
	 */
	public void addInitialFlags(GetFlags flagger) {
		synchronized (lock) {
			firstFlags.add(flagger);
		}
	}
	
	/** Same as {@link #sync(SyncSuccess, SyncFail, SyncProgress)} with null listeners. */
	public TaskRunnable sync() {
		return sync(null, null, null);
	}
	
	/**
	 * Perform a "sync". If one is already pending, this will listen to that pending sync rather than start a new one.
	 *
	 * If logged out, this just sends pending actions.
	 * If logged in, but not fetched, this sends actions and then starts or continues the fetching process.
	 * After fetching is complete, this sends actions and then syncs a {@link Get} with a {@link Get#changes_since},
	 * including any flags set internally and with {@link #addFlags(GetFlags)}, and invoking any work added via {@link #addWork(SyncWork)}.
	 *
	 * @param onSuccess An optional callback to be invoked when successful
	 * @param onFail An optional callback to be invoked if it fails
	 * @param progress An optional callback that will report fetching progress if fetching occurs.
	 * @return A task that you can wait on if needed to block until complete
	 */
	public TaskRunnable sync(SyncSuccess onSuccess, SyncFail onFail, SyncProgress progress) {
		App.checkIfDebuggable(); // Random annoyance added for those trying to decompile and change our source code. (Was originally in GetTask)
		
		synchronized (lock) {
			if (active == null) {
				active = new SyncTask();
				
				// Update sync listeners now and when completed
				dispatchEvent(true, syncListeners, l -> l.onSyncStateChanged(true));
				active.listen(onSuccess, onFail, progress);
				active.listen(
						() -> dispatchEvent(true, syncListeners, l -> l.onSyncStateChanged(false)),
						error -> dispatchEvent(true, syncListeners, l -> l.onSyncStateChanged(false)),
						null);
				threads.submit(active);
			} else {
				active.listen(onSuccess, onFail, progress);
			}
			return active;
		}
	}
	
	public interface SyncSuccess {
		void onSyncCompleted();
	}
	public interface SyncFail {
		void onSyncFailed(Throwable e);
	}
	public interface SyncProgress {
		void onSyncProgress(float progress);
	}
	interface ListenerDispatch<T> {
		void dispatch(T listener) throws Exception;
	}
	public interface SyncWork {
		/**
		 * Perform additional sync work.
		 *
		 * When logged in (not in guest mode or pre-login),
		 * this will be invoked after the {@link Get} that {@link AppSync} performs.
		 * If logged out, this will be after it's done its simpler logged out sync.
		 *
		 * Any exceptions thrown in this method are ignored and have no impact on the app sync.
		 * This can be a blocking operation but for the best efficiency your work should be async should return a {@link PendingResult}.
		 * If you are doing multiple requests consider invoking {@link #addWork(SyncWork)} for each separately so you can return a pending result for each one.
		 * <b>Do not attempt to interact with {@link AppSync} during this call or there is a very strong risk of thread locks.</b>
		 * @param isFirst true if this is during the initial sync after logging in. (called the fetch)
		 * @param get The response from {@link Get} during this sync. This will be null when logged out.
		 * @param loginInfo {@link LoginInfo} state as it was at the beginning of the sync process for ease of checking if there is a logged in user.
		 * @return The pending result of your work or null if there is nothing further to do.
		 */
		PendingResult sync(boolean isFirst, @Nullable Get get, @NonNull LoginInfo loginInfo) throws Exception;
	}
	public interface GetFlags {
		/**
		 * Add flags you'd like to have in the sync.
		 * Note: Any non-flag values you set on this builder will be ignored.
		 * You can only turn flags on, off values are ignored.
		 * If multiple components ask for the same flag, it will remain on or take the highest integer value provided.
		 * This will be invoked separately for each get call as it is preparing to invoke it, avoid long running operations.
		 * <b>Dev Note: If you add a new flag, make sure it has been setup internally in {@link AppSync#getFlags(List, SyncState)}, otherwise adding it here will have no effect.</b>
		 */
		void flag(Get.Builder get);
	}
	
	private <T> void dispatchEvent(boolean onUiThread, List<T> listeners, ListenerDispatch<T> dispatch) {
		Runnable run = () -> {
			List<T> copy;
			synchronized (lock) {
				copy = new ArrayList<>(listeners);
			}
			for (T listener : copy) {
				try {
					dispatch.dispatch(listener);
				} catch (Throwable t) {
					// Don't allow a listener crash to prevent other listeners from getting their callbacks
					Logs.printStackTrace(t);
				}
			}
		};
		if (onUiThread) {
			threads.runOrPostOnUiThread(run);
		} else {
			run.run();
		}
	}
	
	private class SyncTask extends TaskRunnable {
		
		private List<SyncSuccess> onSuccess = new ArrayList<>();
		private List<SyncFail> onFail = new ArrayList<>();
		private List<SyncProgress> onProgress = new ArrayList<>();
		
		@Override
		public void backgroundOperation() throws Exception {
			try {
				sync();
				clearActive();
				dispatchEvent(true, onSuccess, l -> l.onSyncCompleted());
				
			} catch (Throwable e) {
				Logs.printStackTrace(e);
				clearActive();
				dispatchEvent(true, onFail, l -> l.onSyncFailed(e));
			}
		}
		
		private void clearActive() {
			synchronized (lock) {
				active = null;
			}
		}
		
		private void dispatchProgress(int complete, int total) {
			float progress = (float) complete / total;
			dispatchEvent(true, onProgress, l -> l.onSyncProgress(progress));
		}
		
		public void listen(SyncSuccess onSuccess, SyncFail onFail, SyncProgress onProgress) {
			synchronized (lock) {
				if (onSuccess != null) this.onSuccess.add(onSuccess);
				if (onFail != null) this.onFail.add(onFail);
				if (onProgress != null) this.onProgress.add(onProgress);
			}
		}
		
		private void sync() throws SyncException {
			PocketThings t = pocket.spec().things();
			PocketActions a = pocket.spec().actions();
			
			SyncState syncState = pocket.sync(t.syncState().build()).get();
			LoginInfo loginInfo = pocket.sync(t.loginInfo().build()).get();
			if (loginInfo.account == null) {
				// While logged out, just send any unsent actions
				pocket.syncActions(null).get();

				// Do additional work.
				List<PendingResult> extra = new ArrayList<>();
				dispatchEvent(false, syncWork, listener -> extra.add(listener.sync(false, null, loginInfo)));
				for (PendingResult pending : extra) {
					if (pending == null) continue;
					try {
						pending.get(); // For additional safety we could time out on waiting for each request so if something goes wrong in one of the outside requests, it doesn't block all of app sync.
					} catch (Throwable ignore) {}
				}
				
			} else if (!Safe.value(syncState.fetched)) {
				// Perform the fetch - which loads in the user's current state from the server as our initial state
				// As this returns items and other things, they will be persisted and updated as requested above
				// Here we use pocket.sync() rather than syncRemote() so if a fetch process was previously interrupted, it allows it to quickly grab the local one and pick up where it left off.
				final List<Object> completedWork = new ArrayList<>();
				Holder fetchHolder = Holder.persistent("fetch"); // By using a holder, if the process restarts later, it can skip ones already loaded.
				Fetch first = t.fetch().shares(true).annotations(true).build();
				pocket.remember(fetchHolder, first);
				first = pocket.sync(first).get();
				completedWork.add(first);
				
				int totalWork = first.remaining_chunks
						+1 // To count the first fetch
						+1 // To count the first "get" we'll do
						+1 // To count for the final action that sets the fetching state as complete
						+syncWork.size(); // To count the misc. other parts we grab afterwards
				dispatchProgress(completedWork.size(), totalWork);
				
				List<PendingResult<Fetch, SyncException>> fetches = new ArrayList<>();
				for (int i = 1; i <= first.remaining_chunks; i++) {
					Fetch chunk = t.fetch()
							.shares(true)
							.annotations(true)
							.updatedBefore(first.since)
							.offset(first.passthrough.firstChunkSize + first.passthrough.fetchChunkSize * (i-1))
							.count(first.passthrough.fetchChunkSize)
							.chunk(i)
							.build();
					pocket.remember(fetchHolder, chunk);
					fetches.add(pocket.syncRemote(chunk)
							.onSuccess(f -> {
								completedWork.add(f);
								dispatchProgress(completedWork.size(), totalWork);
							}));
				}
				for (PendingResult<Fetch, SyncException> p : fetches) {
					p.get(); // Wait until all are complete or throw
				}
				
				pocket.forget(fetchHolder); // Safe to release this, since anything we wanted from it will have been extracted and remembered.
				
				dispatchProgress(completedWork.size(), totalWork);
				
				// As the final part of the fetch, perform the first "get"
				SyncState s = pocket.sync(t.syncState().build()).get();
				List<GetFlags> allflags = new ArrayList<>(firstFlags);
				allflags.addAll(flags);
				Get get = pocket.syncRemote(getFlags(allflags, s).build()).get();
				completedWork.add(get);
				
				List<PendingResult> extra = new ArrayList<>();
				dispatchEvent(false, syncWork, listener -> extra.add(listener.sync(true, get, loginInfo)));
				for (PendingResult pending : extra) {
					if (pending == null) continue;
					try {
						completedWork.add(pending.get());
					} catch (Throwable ignore) {
						completedWork.add(null);
					} finally {
						dispatchProgress(completedWork.size(), totalWork);
					}
				}
				
				// Mark as complete
				completedWork.add(pocket.sync(null, a.fetch_completed().time(Timestamp.now()).build()).get());
				dispatchProgress(completedWork.size(), totalWork);
				hasFetched.set(true);
				dispatchEvent(false, fetchWork, Runnable::run);
				
			} else {
				// Normal sync
				SyncState s = pocket.sync(t.syncState().build()).get();
				Get get = pocket.syncRemote(getFlags(flags, s).build()).get();
				
				List<PendingResult> extra = new ArrayList<>();
				dispatchEvent(false, syncWork, listener -> extra.add(listener.sync(false, get, loginInfo)));
				for (PendingResult pending : extra) {
					if (pending == null) continue;
					try {
						pending.get(); // For additional safety we could time out on waiting for each request so if something goes wrong in one of the outside requests, it doesn't block all of app sync.
					} catch (Throwable ignore) {}
				}
			}
		}
	}
	
	/**
	 * Returns a builder that has all of the outside requests for flags set.
	 * Outside callers can only set flags, they can't turn them off or change any other non-flag values.
	 */
	private Get.Builder getFlags(List<GetFlags> flags, SyncState state) {
		List<Get> requests = new ArrayList<>(flags.size());
		dispatchEvent(false, flags, listener -> {
			Get.Builder requested = pocket.spec().things().get();
			listener.flag(requested);
			requests.add(requested.build());
		});
		
		Get master = pocket.spec().things().get().build();
		for (Get requested : requests) {
			Get.Builder mod = master.builder();
				// Dev Note: I had considered being more generic and just iterating over a serialized version of the request
				// and just blinding applying values, but that would give an outside component the ability to set identity fields
				// that could break the functionality of this class, so this, though verbose, is the safest way to ensure that
				// the flag setting api truly can only set flags and nothing else.
				// It also means that we must add new flags here to make them supported for outside use.
				
				// These flags are organized in the same order and grouping that they appear in the schema
				
				if (requested.declared.videos) mod.videos(Math.max(Safe.value(requested.videos), Safe.value(master.videos)));
				if (requested.declared.images) mod.images(Math.max(Safe.value(requested.images), Safe.value(master.images)));
				if (requested.declared.include_item_tags) mod.include_item_tags(Math.max(Safe.value(requested.include_item_tags), Safe.value(master.include_item_tags)));
				if (requested.declared.positions) mod.positions(Math.max(Safe.value(requested.positions), Safe.value(master.positions)));
				if (requested.declared.meta) mod.meta(Math.max(Safe.value(requested.meta), Safe.value(master.meta)));
				if (requested.declared.posts) mod.posts(Math.max(Safe.value(requested.posts), Safe.value(master.posts)));
				if (requested.declared.authors) mod.authors(Math.max(Safe.value(requested.authors), Safe.value(master.authors)));
				if (requested.declared.annotations) mod.annotations(Math.max(Safe.value(requested.annotations), Safe.value(master.annotations)));
				
				if (requested.declared.forceposts) mod.forceposts(Math.max(Safe.value(requested.forceposts), Safe.value(master.forceposts)));
				if (requested.declared.forcetweetupgrade) mod.forcetweetupgrade(Math.max(Safe.value(requested.forcetweetupgrade), Safe.value(master.forcetweetupgrade)));
				if (requested.declared.forcevideolength) mod.forcevideolength(Math.max(Safe.value(requested.forcevideolength), Safe.value(master.forcevideolength)));
				if (requested.declared.forceannotations) mod.forceannotations(Math.max(Safe.value(requested.forceannotations), Safe.value(master.forceannotations)));
				if (requested.declared.force70upgrade) mod.force70upgrade(Math.max(Safe.value(requested.force70upgrade), Safe.value(master.force70upgrade)));
				
				if (requested.declared.shares) mod.shares(Math.max(Safe.value(requested.shares), Safe.value(master.shares)));
				if (requested.declared.forcemails) mod.forcemails(Math.max(Safe.value(requested.forcemails), Safe.value(master.forcemails)));
				
				if (requested.declared.rediscovery) mod.rediscovery(Math.max(Safe.value(requested.rediscovery), Safe.value(master.rediscovery)));
				if (requested.declared.forcerediscovery) mod.forcerediscovery(Math.max(Safe.value(requested.forcerediscovery), Safe.value(master.forcerediscovery)));
				
				if (requested.declared.taglist) mod.taglist(Math.max(Safe.value(requested.taglist), Safe.value(master.taglist)));
				if (requested.declared.forcetaglist) mod.forcetaglist(Math.max(Safe.value(requested.forcetaglist), Safe.value(master.forcetaglist)));
			
				if (requested.declared.include_account) mod.include_account(Math.max(Safe.value(requested.include_account), Safe.value(master.include_account)));
				if (requested.declared.forceaccount) mod.forceaccount(Math.max(Safe.value(requested.forceaccount), Safe.value(master.forceaccount)));
			
				if (requested.declared.include_notification_status) mod.include_notification_status(Math.max(Safe.value(requested.include_notification_status), Safe.value(master.include_notification_status)));
				
				if (requested.declared.premium) mod.premium(Math.max(Safe.value(requested.premium), Safe.value(master.premium)));
				if (requested.declared.forcepremium) mod.forcepremium(Math.max(Safe.value(requested.forcepremium), Safe.value(master.forcepremium)));

				if (requested.declared.include_connected_accounts) mod.include_connected_accounts(Math.max(Safe.value(requested.include_connected_accounts), Safe.value(master.include_connected_accounts)));
				if (requested.declared.forceconnectedaccounts) mod.forceconnectedaccounts(Math.max(Safe.value(requested.forceconnectedaccounts), Safe.value(master.forceconnectedaccounts)));
				
				if (requested.declared.listCount) mod.listCount(Safe.value(requested.listCount) || Safe.value(master.listCount));
				if (requested.declared.forceListCount) mod.forceListCount(Safe.value(requested.forceListCount) || Safe.value(master.forceListCount));
			
				if (requested.declared.forcesettings) mod.forcesettings(Math.max(Safe.value(requested.forcesettings), Safe.value(master.forcesettings)));
				if (requested.declared.listen) mod.listen(Safe.value(requested.listen) || Safe.value(master.listen));
				
				master = mod.build();
		}
		
		// Setup defaults for all requests
		Get.Builder builder = GetUtil.setAllItemFlags(master.builder());
		if (state.since != null) {
			builder.changes_since(state.since);
		}
		return builder;
	}
	
	public void addOnSyncStateChangedListener(SyncingListener listener) {
		synchronized (lock) {
			syncListeners.add(listener);
		}
	}
	
	public interface SyncingListener {
		void onSyncStateChanged(boolean isSyncing);
	}
	
	/**
	 * Has the app logged in and completed its fetching process yet?
	 */
	public boolean hasFetched() {
		return hasFetched.get();
	}
	
	public BooleanPreference autoSyncPref() {
		return autoSync;
	}
	
	/**
	 * This class performs the client conventions / contract for ensuring actions go out in a timely manner.
	 *
	 * Client conventions for when to send actions:  TODO find a good place to document this platform wide.
	 *
	 * <ul>
	 * 	<li>A client app should send important (asap) actions immediately.</li>
	 *  <li>Non-asap actions are sent lazily whenever the next send happens to go out.</li>
	 *  <li>When a user leaves the app, it should attempt to do a send to help ensure all session actions go out,
	 * 		regardless of whether or not there was an asap action in the session.</li>
	 * 	<li>If a client is offline and has pending actions, it should attempt to send them when a connection comes back online.
	 * 		TODO looks like in the current implementation we only do this if there are asap actions pending,
	 * 		but if the convention is to do a send at the end of a session,
	 * 		wouldn't we also want to send non-asap actions?
	 * 		If we don't then if the user never returns to the app, those
	 * 		non-asap actions will never go out.</li>
	 * </ul>
	 *
	 * This class will handle the last two conventions: When the user leaves the app it will attempt to send,
	 * and if there are asap actions waiting to go out, it will send them when the device reconnects.
	 *
	 * A few cases to test when making changes: TODO unit tests
	 * <ul>
	 * <li>When user leaves app, all actions are attempted to send.</li>
	 * <li>When asap actions are pending and app is in the background, and offline, coming online sends actions</li>
	 * <li>When asap actions are pending and app is in the foreground, and offline, coming online sends actions</li>
	 * <li>If a background job fails, it retries again next time the device reconnects</li>
	 * <li>When opening the app, connection listener started and job is cancelled.</li>
	 * <li>When closing the app, connection listener stopped and if pending asap actions, job is scheduled</li>
	 * </ul>
	 */
	private static class Sender {
		
		private final NetworkStatus networkStatus;
		private final Pocket pocket;
		private final Jobs jobs;
		private boolean isOnline;
		
		Sender(Pocket pocket, Jobs jobs, NetworkStatus networkStatus) {
			this.pocket = pocket;
			this.jobs = jobs;
			this.networkStatus = networkStatus;
			jobs.registerCreator(FlushSendJob.class, FlushSendJob::new);
		}
		
		private NetworkStatus.Listener connectionListener = new NetworkStatus.Listener() {
			@Override
			public void onStatusChanged(NetworkStatus status) {
				boolean wasOnline = isOnline;
				isOnline = status.isOnline();
				if (DEBUG) Logs.v(LOG_TAG, "in app connection state change: " + isOnline);
				if (!wasOnline && isOnline) {
					if (DEBUG) Logs.v(LOG_TAG, "in app connection state change triggers send");
					pocket.syncActions(RemotePriority.SOON);
				}
			}
		};
		
		public void onUserPresent() {
			if (DEBUG) Logs.v(LOG_TAG, "user opened app");
			
			// Unschedule jobs, since it is only needed for when the user is not in the app.
			unscheduleJob();
			
			// Monitor the connection status manually while the user is in the app.
			// This will fulfill the convention of sending pending actions when a device reconnects.
			// While the app is in the foreground, using a connection listener will be faster than an background job.
			isOnline = networkStatus.isOnline();
			networkStatus.addListener(connectionListener);
		}
		
		public void onUserGone() {
			if (DEBUG) Logs.v(LOG_TAG, "user left app");
			
			// While in the background, we'll use a job to handle connection monitoring instead.
			networkStatus.removeListener(connectionListener);
			
			// Fulfill the convention of sending when they leave the app.
			if (DEBUG) Logs.v(LOG_TAG, "manually send");
			pocket.syncActions(null)
					.onFailure(e -> {
						// Schedule a job that will attempt this when the internet is back
						if (DEBUG) Logs.v(LOG_TAG, "failed to send, schedule for later");
						scheduleJob();
					});
		}
		
		private void scheduleJob() {
			if (DEBUG) Logs.v(LOG_TAG, "schedule job");
			jobs.scheduleOneOff(FlushSendJob.class, 1, NetworkType.UNMETERED);
		}
		
		private void unscheduleJob() {
			if (DEBUG) Logs.v(LOG_TAG, "unschedule");
			jobs.cancel(FlushSendJob.class);
		}
		
		/**
		 * If there are any important pending actions needing to be sent to the server, this will attempt to send them all.
		 */
		public static class FlushSendJob extends Worker {
			
			FlushSendJob(@NonNull Context context, @NonNull WorkerParameters workerParams) {
				super(context, workerParams);
			}
			
			@NonNull
			@Override
			public Result doWork() {
				if (DEBUG) Logs.v(LOG_TAG, "onRunJob");
				try {
					App.getApp().pocket().syncActions(RemotePriority.SOON).get();
					return Result.success();
				} catch (SyncException e) {
					if (DEBUG) Logs.v(LOG_TAG, "onRunJob reschedule");
					Logs.printStackTrace(e);
					return Result.retry();
				}
			}
			
		}
	}
	
}
