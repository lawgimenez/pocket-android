package com.pocket.sdk2.api.legacy;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.app.AppLifecycle;
import com.pocket.app.AppLifecycleEventDispatcher;
import com.pocket.app.AppThreads;
import com.pocket.sdk.dev.ErrorHandler;
import com.pocket.app.VersionUtil;
import com.pocket.app.build.Versioning;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.Modeller;
import com.pocket.sdk.api.generated.enums.AuthMethod;
import com.pocket.sdk.api.generated.enums.CxtEvent;
import com.pocket.sdk.api.generated.enums.CxtPage;
import com.pocket.sdk.api.generated.enums.CxtSection;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.api.generated.enums.GroupId;
import com.pocket.sdk.api.generated.enums.Imageness;
import com.pocket.sdk.api.generated.enums.ItemSortKey;
import com.pocket.sdk.api.generated.enums.ItemStatus;
import com.pocket.sdk.api.generated.enums.OfflineStatus;
import com.pocket.sdk.api.generated.enums.PositionType;
import com.pocket.sdk.api.generated.enums.PostService;
import com.pocket.sdk.api.generated.enums.SearchContextKey;
import com.pocket.sdk.api.generated.enums.SharedItemStatus;
import com.pocket.sdk.api.generated.enums.UserMessageResult;
import com.pocket.sdk.api.generated.enums.VideoType;
import com.pocket.sdk.api.generated.enums.Videoness;
import com.pocket.sdk.api.generated.thing.AcEmail;
import com.pocket.sdk.api.generated.thing.Account;
import com.pocket.sdk.api.generated.thing.Annotation;
import com.pocket.sdk.api.generated.thing.Author;
import com.pocket.sdk.api.generated.thing.ConnectedAccounts;
import com.pocket.sdk.api.generated.thing.DomainMetadata;
import com.pocket.sdk.api.generated.thing.Friend;
import com.pocket.sdk.api.generated.thing.Group;
import com.pocket.sdk.api.generated.thing.Image;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.generated.thing.ListenSettings;
import com.pocket.sdk.api.generated.thing.LoginInfo;
import com.pocket.sdk.api.generated.thing.Position;
import com.pocket.sdk.api.generated.thing.Post;
import com.pocket.sdk.api.generated.thing.RecentFriends;
import com.pocket.sdk.api.generated.thing.RecentSearches;
import com.pocket.sdk.api.generated.thing.SearchQuery;
import com.pocket.sdk.api.generated.thing.SharedItem;
import com.pocket.sdk.api.generated.thing.SyncState;
import com.pocket.sdk.api.generated.thing.Tag;
import com.pocket.sdk.api.generated.thing.Tags;
import com.pocket.sdk.api.generated.thing.Video;
import com.pocket.sdk.api.source.V3Source;
import com.pocket.sdk.api.thing.TagUtil;
import com.pocket.sdk.api.value.AccessToken;
import com.pocket.sdk.api.value.EmailString;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.api.value.UrlString;
import com.pocket.sdk.offline.cache.AssetDirectory;
import com.pocket.sdk.offline.cache.AssetUser;
import com.pocket.sdk.offline.cache.Assets;
import com.pocket.sdk.offline.cache.AssetsDatabase;
import com.pocket.sdk.preferences.AppPrefs;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sdk.util.dialog.AlertMessaging;
import com.pocket.sdk.util.file.AndroidStorageLocation;
import com.pocket.sync.action.Action;
import com.pocket.sync.source.result.RemotePriority;
import com.pocket.sync.source.result.SyncException;
import com.pocket.sync.space.Holder;
import com.pocket.sync.space.Space;
import com.pocket.sync.space.mutable.MutableSpace;
import com.pocket.sync.space.persist.DumbStorage;
import com.pocket.sync.space.persist.MemoryStorage;
import com.pocket.sync.space.persist.MigrationStorage;
import com.pocket.sync.spec.Spec;
import com.pocket.sync.thing.Thing;
import com.pocket.util.android.sql.SqliteUtil;
import com.pocket.util.java.JsonUtil;
import com.pocket.util.java.Safe;
import com.pocket.util.java.StopWatch;
import com.pocket.util.prefs.BooleanPreference;
import com.pocket.util.prefs.Preferences;
import com.pocket.util.prefs.StringPreference;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Performs the upgrades needed to migrate to the new sync engine (version 7.42.0.0):
 * <ul>
 *     <li>Moves Item, Friend and other thing state from the old storage locations (The DbDelegate/ListOperation sqlite database and misc preferences) to {@link com.pocket.sdk.Pocket}'s {@link com.pocket.sync.space.Space}</li>
 *     <li>Renames item markup folders from the no longer used Item.unique_id to {@link Item#idkey()}</li>
 *     <li>Moves the asset and asset user data from the old sqlite database to the new {@link AssetsDatabase} and the new {@link AssetUser}s</li>
 * </ul>
 *
 * <h2>The design of this migration:</h2>
 * <ul>
 *     <li>{@link com.pocket.app.PocketModule} invokes {@link #createIfNeeded(Preferences, Context, Versioning, Assets, ErrorHandler, AppPrefs, int)}</li>
 *     <li>That method decides if the migration needs to run and returns an instance if so</li>
 *     <li>When {@link PocketCache} is created, if it receives a non-null instance of this, it will grab {@link #loginInfo()} and save it as its cached data.</li>
 *     <li>When {@link com.pocket.sdk.Pocket} is created, if it receives a non-null instance of this, it will use {@link #storage()} as its {@link com.pocket.sdk.Pocket.Config.Builder#migration(MigrationStorage.Source)}.</li>
 *     <li>If the migration is not needed, those components receive a null instance and run normally.</li>
 * </ul>
 * The way this is setup is that when we no longer need the migration code, we can just delete it and the code that references it.
 * It tries to have all of the app code basically run normally / by default with very little tweaking to support this.
 * <p>
 * Since this does the bulk of its migration work during {@link DumbStorage#restore(Spec, DumbStorage.ThingCallback, DumbStorage.HolderCallback, DumbStorage.ActionCallback, DumbStorage.InvalidCallback)},
 * which the app is already prepared to handle taking a few seconds, this will to the user just look like a slightly longer loading
 * of their list when they open the app.
 * <p>
 * It is also implemented in a way that if it fails, it can be retried. It does not delete any data.
 * (With the exception of the cases outlined below that reset the app entirely)
 * If we find in the wild some users hit an issue, we can release a patch and it will be able to retry.
 * We can handle cleaning up the old files in a future update after we are sure the migration path worked well for users.
 * <p>
 *
 * <h2>There are a few possible user experiences:</h2>
 *
 * <h3>Upgrading from before Pocket 6.7.0.1 (released in Dec 2018)</h3>
 * The v3 API shutoff support for versions of the app pre-6.7 because of the item id max integer limit. See https://pocket.slack.com/archives/C8199PDK5/p1525183865000149 for more details on that.
 * Due to this, testing upgrade paths from versions before this will be extremely challenging.
 * Given this and analytics on app upgrades and active users, less than 0.2% of recent app upgrades come from pre 6.7.0.0 versions,
 * we decided that we would use this as the cut off for eligible upgrade paths.  Since we've historically supported upgrading from
 * even the original 10 year old version of the app, this will also allow us to trim off a ton complex migration paths from the code base.
 * <p>
 * So when upgrading from pre-6.7.0.1, we will not attempt to migrate any data and will reset the app back to the default state.
 *
 * <h3>Upgrading from Pocket 6.7.0.1 or later</h3>
 * We will automatically migrate their data like a normal app upgrade. Using {@link Versioning#addUpgradePrepTask(Runnable)} we'll attempt to start
 * this migration quietly right away so its done before they open the app.  If they open the app before it is complete, the
 * app will just appear to take a bit longer to load its content in tabs/screens.
 *
 * <h4>Upgrading from Pocket 6.7.0.1 or later, but before logged in or fetching completed</h4>
 * Acts similar to pre-6.7.0.1 upgrades.
 *
 * 	<h2>Notes about stuff not migrated or doesn't need specific logic:</h2>
 * 	<ul>
 * 	    <li>Spocs aren't current active server side, so no need to try to migrate anything, they'd all just be out of date</li>
 * 	    <li>{@link com.pocket.app.UserManager} Login data is handled by other parts of the migration, google connection data continues to use the same prefs</li>
 * 	    <li>{@link com.pocket.sdk.api.UserMessaging} Uses same prefs as before</li>
 * 	    <li>{@link com.pocket.sdk.premium.billing.google.GooglePlayBilling} If there was a pending purchase awaiting activation we won't attempt to migrate it, they can restore the purchase if needed, but in recent troubleshooting we've seen a lot of bad purchases being retried over and over.</li>
 * 	    <li>Not migrating tweets, they will need to be redownloaded. Their format changed enough that it would be complex to migrate. They will need to be redownloaded when they open the reader</li>
 * 	</ul>
 *
 */
public class LegacyMigration implements AppLifecycle {
	private static BooleanPreference forceReset;

	public static LegacyMigration createIfNeeded(
			Preferences prefs,
			Context context,
			Versioning versioning,
			Assets assets,
			ErrorHandler errorReporter,
			AppPrefs appPrefs,
			int maxItems,
			AppThreads appThreads,
			AppLifecycleEventDispatcher dispatcher
	) {
		BooleanPreference isMigrated = prefs.forApp("sync-engine-setup", false);
		forceReset = prefs.forApp("sync-engine-setup-force-reset", false);
		
		// Already migrated?
		if (isMigrated.get()) {
			deleteLegacyDb(context, prefs, errorReporter);
			return null;
		}
		
		// First app run? No need to do anything further
		if (versioning.isFirstRun()) {
			isMigrated.set(true);
			return null;
		}
		
		// Grab state from the legacy preferences
		LoginInfo login = extractLoginInfo(prefs);
		SyncState syncState = extractSyncState(prefs);
		
		// Should we reset the app instead of migrating?
		// Using 6.7.0.1 instead of 6.7.0.0 so that we can test this reset path by installing 6.7.0.0 (which is the earliest version that is still supported by v3)
		if (forceReset.get() ||
				versioning.from() < VersionUtil.toVersionCode(6, 7, 0, 1) ||
				login.account == null || // If not logged in
				!syncState.fetched) { // or in some corner case, part way through fetching also just reset the app so we don't need to test those corner cases.
			versioning.resetApp(assets.getAssetDirectoryQuietly());
			throw new RuntimeException(); // It is expected the process has stopped by this point, so this should not run.
		}
		
		// Ok, we'll migrate!
		// This migration path can take several seconds, maybe upwards of a minute for large accounts
		// so take advantage of the update receiver if available to start this work before the user opens the app.
		versioning.addUpgradePrepTask(() -> {
			try {
				App.from(context).pocket().syncLocal(login).get(); // Just need to do anything to trigger the Pocket instance initializing since our processing happenings during storage restoration.
			} catch (SyncException ignore) {}
		});
		LegacyMigration legacyMigration = new LegacyMigration(
				context,
				prefs,
				assets,
				isMigrated,
				login,
				syncState,
				errorReporter,
				appPrefs,
				maxItems,
				dispatcher
		);

		appThreads.postOnUiThread(legacyMigration::onComponentsCreated);

		return legacyMigration;
	}

	private static void deleteLegacyDb(Context context, Preferences prefs, ErrorHandler errorReporter) {
		BooleanPreference deletedOldDb = prefs.forApp("removed-legacy-db", false);
		if (!deletedOldDb.get()) {
			try {
				context.deleteDatabase("ril");
				deletedOldDb.set(true);
			} catch (Exception e) {
				errorReporter.reportError(e);
			}
		}
	}

	/** Creates a current {@link LoginInfo} from legacy data. */
	private static LoginInfo extractLoginInfo(Preferences prefs) {
		LoginInfo.Builder builder = new LoginInfo.Builder();
		builder.guid(prefs.forApp("guid", (String) null).get());
		String usermeta = prefs.forUser("user_meta", (String) null).get();
		builder.maxActions(prefs.forApp("api_max_actions", V3Source.MAX_ACTIONS_DEFAULT).get());
		if (usermeta != null) {
			// Restore the access token from the old simple obfuscation used in the older app
			String accessToken1 = prefs.forUser("scrollbarHash", (String) null).get();
			String accessToken2 = prefs.forUser("c2dmTempId", (String) null).get();
			String restoredToken = accessToken1 != null && accessToken2 != null ? accessToken1.substring(10).concat(accessToken2) : null;
			if (restoredToken != null) builder.access_token(new AccessToken(restoredToken));
			
			builder.wasSignup(prefs.forUser("isNewUser", false).get());
			builder.authMethod(AuthMethod.find(prefs.forUser("logmethod", 0).get()));
			builder.account(Account.from(Modeller.toObjectNode(usermeta), V3Source.JSON_CONFIG));
		}
		return builder.build();
	}
	
	/** Creates a current {@link SyncState} from legacy data. */
	private static SyncState extractSyncState(Preferences prefs) {
		SyncState.Builder builder = new SyncState.Builder();
		builder.fetched(prefs.forUser("hasFetched", false).get() && prefs.forUser("firstSyncComplete", false).get());
		long since = prefs.forUser("since", 0L).get();
		if (since > 0) builder.since(new Timestamp(since));
		return builder.build();
	}
	
	private final Context context;
	private final Preferences prefs;
	private final AppPrefs appPrefs;
	private final BooleanPreference isMigrated;
	private final Assets assets;
	private final LoginInfo loginInfo;
	private final SyncState syncState;
	private final CountDownLatch componentsReady = new CountDownLatch(1);
	private final ErrorHandler errorReporter;
	private final int maxItems;

	private boolean reportedError;
	private boolean reportedAssetError;
	private boolean userBecamePresent;

	private boolean showDialogOnActivityResume = true;
	private AlertDialog dialog;
	
	private LegacyMigration(
			Context context,
			Preferences prefs,
			Assets assets,
			BooleanPreference isMigrated,
			LoginInfo loginInfo,
			SyncState syncState,
			ErrorHandler errorReporter,
			AppPrefs appPrefs,
			int maxItems,
			AppLifecycleEventDispatcher dispatcher
	) {
		dispatcher.registerAppLifecycleObserver(this);
		this.context = context;
		this.prefs = prefs;
		this.appPrefs = appPrefs;
		this.assets = assets;
		this.isMigrated = isMigrated;
		this.loginInfo = loginInfo;
		this.syncState = syncState;
		this.errorReporter = errorReporter;
		this.maxItems = maxItems;
	}
	
	/** Returns the {@link LoginInfo} that was found in legacy storage. */
	public LoginInfo loginInfo() {
		return loginInfo;
	}
	
	/** {@link com.pocket.app.PocketModule} should invoke this after all components have been initialized and {@link App#from(Context)} will return a working instance. */
	public void onComponentsCreated() {
		componentsReady.countDown();
	}
	
	/** {@link com.pocket.app.PocketSingleton} should use this as the {@link Pocket.Config.Builder#migration(MigrationStorage.Type)}. */
	public MigrationStorage.Source storage() {
		return new MigrationStorage.Source() {
			@Override
			public void restore(Spec spec, DumbStorage.ThingCallback things, DumbStorage.HolderCallback holders, DumbStorage.ActionCallback actions, DumbStorage.InvalidCallback invalids) {
				StopWatch duration = new StopWatch();
				duration.resume();
				SQLiteDatabase db = null;
				try {
					// In order to safely access AssetUser APIs App.onCreate needs to have finished
					// It is way out of scope to refactor AssetUser's static methods to fix this,
					// so instead if this gets called fast enough that the components are all setup yet,
					// just wait until they are.
					componentsReady.await();
					
					// Migrate some preferences
					prefs(prefs, appPrefs);
					
					// Load the legacy database
					db = new LegacyDatabase(context).getWritableDatabase();
					
					// Collect all data into a Space backed by a DumbStorage
					// This lets us imprint things as we find it, which will make sure they are derived properly
					// and merged properly if multiple instances are found.
					// When restoring things, try to have the more trustworthy states at the end, so they are imprinted last.
					DumbStorage tempStorage = new MemoryStorage();
					MutableSpace space = new MutableSpace(tempStorage).setSpec(spec);
					Holder holder = Holder.persistent("legacy_migration"); // It has to be persistent since spaces will discard session holds when restoring.
					AtomicInteger itemCount = new AtomicInteger();
					AtomicInteger thingCount = new AtomicInteger();
					
					// Restore Things and Actions
					try {
						things(db, maxItems, itemCount, value -> {
							// Create a session hold for every thing that is restored,
							// This will make sure they are all held until all PocketComponents have initialized and obtained their own holds
							space.remember(holder, value);
							space.imprint(value);
							itemCount.set(space.countOf(Item.THING_TYPE));
							thingCount.set(space.countOf(null));
						});
					} catch (Throwable t) {
						// Try to gather more info if we hit an out of memory error
						if (t instanceof OutOfMemoryError) {
							MemoryLimitLogging.rethrowWithInfo(t, thingCount.get(), itemCount.get());
						} else {
							throw t;
						}
					}
					actions(spec, db, space);
					
					// Now take our temp storage and pass all of its contents up to the migration
					tempStorage.restore(spec, things, holders, actions, invalids);
					
					Pocket pocket = App.from(context).pocket();
					
					// Release this persistent hold. This is async so it will run in the future.
					pocket.forget(holder).onFailure(errorReporter::reportError); // Failure is not expected, but if this fails, these things may be held forever, so report it
					
					// Done!
					deleteLegacyDb(context, prefs, errorReporter);
					isMigrated.set(true);
					dismissDialog();
					
					// Analytics event https://docs.google.com/spreadsheets/d/1My7HyOBBiC4_RSg9oBUQxtT7CWxCAM_tzDs_N7LGj3s/edit#gid=0
					duration.pause();
					pocket.sync(null, pocket.spec().actions().pv_wt()
							.view(CxtView.MOBILE)
							.type_id(((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass())
							.reason_code(UserMessageResult.create(itemCount.get()))
							.section(CxtSection.SYNC_UPGRADE)
							.page(CxtPage.create(duration.length()+""))
							.action_identifier(CxtEvent.create(userBecamePresent ? "1" : "0"))
							.time(Timestamp.now())
							.build());
					
				} catch (Throwable t) {
					if (!reportedError) {
						reportedError = true;
						errorReporter.reportError(t);
					}
					throw new RuntimeException(t);
					
				} finally {
					IOUtils.closeQuietly(db);
				}
			}

			private void prefs(Preferences prefs, AppPrefs appPrefs) {
				StringPreference sort = prefs.forUser("sort", ItemSortKey.NEWEST.value);
				switch (prefs.forUser("queueSort", 0).get()) {
					case 0: sort.set(ItemSortKey.NEWEST.value); break;
					case 1: sort.set(ItemSortKey.OLDEST.value); break;
				}
				appPrefs.START_IN_ARTICLE_VIEW.set(prefs.forUser("readerViewType", 1).get() == 1);
			}
			
			private void things(SQLiteDatabase db, int maxItems, AtomicInteger itemCounter, DumbStorage.ThingCallback things) throws Exception {
				// This is ordered such that if there is a chance that the same thing might be found,
				// the more important instance is restored last, so it is imprinted last.  For example the profile instance in
				// the login info is more important than if that same profile appears in a post in item.posts
				connectedAccounts(prefs, things);
				listen(prefs, things);
				tags(db, things);
				recentSearches(db, things);
				groups(prefs, things);
				sendToFriend(db, prefs, things);
				items(db, maxItems, itemCounter, things);
				things.restored(syncState);
				things.restored(loginInfo);
			}
			
			private void connectedAccounts(Preferences prefs, DumbStorage.ThingCallback out) {
				ArrayNode json = JsonUtil.stringToArrayNode(prefs.forUser("social_accs", (String) null).get());
				if (json != null) {
					List<PostService> list = new ArrayList<>();
					for (JsonNode v : json) {
						PostService f = PostService.find(Modeller.asString(v));
						if (f != null) list.add(f);
					}
					if (!list.isEmpty()) out.restored(new ConnectedAccounts.Builder().connectedAccounts(list).build());
				}
			}
			
			private void listen(Preferences prefs, DumbStorage.ThingCallback out) {
				out.restored(new ListenSettings.Builder()
						.item_max_word_count(prefs.forUser("lstn_maxwc", 24000).get())
						.item_min_word_count(prefs.forUser("lstn_minwc", 0).get())
						.build());
			}
			
			private void groups(Preferences prefs, DumbStorage.ThingCallback things) {
				ObjectNode groupsJson = JsonUtil.stringToObjectNode(prefs.forUser("groups", (String) null).get());
				if (groupsJson != null) {
					for (JsonNode group : groupsJson) {
						things.restored(Group.from(group, V3Source.JSON_CONFIG));
					}
				}
			}
			
			private void tags(SQLiteDatabase db, DumbStorage.ThingCallback out) {
				try {
					List<String> tags = new ArrayList<>();
					Cursor cursor = db.rawQuery("SELECT tag FROM tags", null);
					while (cursor.moveToNext()) {
						Tag tag = TagUtil.clean(cursor.getString(0));
						if (tag != null) tags.add(tag.tag);
					}
					if (!tags.isEmpty()) out.restored(new Tags.Builder().tags(tags).build());
					cursor.close();
				} catch (SQLiteException e) {
					// If tags table is missing, just force full app reset next time.
					forceReset.set(true);
					throw e;
				}
			}
			
			private void actions(Spec spec, SQLiteDatabase db, Space out) {
				Cursor cursor = db.rawQuery("SELECT action, send_asap FROM sync_queue", null);
				while (cursor.moveToNext()) {
					Action action = spec.actions().action(JsonUtil.stringToObjectNode(cursor.getString(0)), V3Source.JSON_CONFIG);
					if (action != null) out.addAction(action, cursor.getInt(1) == 1 ? RemotePriority.SOON : RemotePriority.WHENEVER);
				}
				cursor.close();
			}
			
			private void recentSearches(SQLiteDatabase db, DumbStorage.ThingCallback out) {
				List<SearchQuery> recents = new ArrayList<>();
				Cursor cursor = db.rawQuery("SELECT search, cxt_key, cxt_val FROM recent_search ORDER BY sort_id DESC", null);
				while (cursor.moveToNext()) {
					SearchQuery.Builder builder = new SearchQuery.Builder();
					builder.search(cursor.getString(0));
					SearchContextKey cxt_key = SearchContextKey.find(cursor.getString(1));
					if (cxt_key != null) {
						builder.context_key(cxt_key);
						String cxt_val = cursor.getString(2);
						if (cxt_val != null) builder.context_value(cxt_val);
					}
					recents.add(builder.build());
				}
				if (!recents.isEmpty()) out.restored(new RecentSearches.Builder().searches(recents).build());
				cursor.close();
			}
			
			private void sendToFriend(SQLiteDatabase db, Preferences prefs, DumbStorage.ThingCallback out) {
				// Friends
				Map<Integer, Friend> localFriendIdToFriend = new HashMap<>();
				Cursor cursor = db.rawQuery("SELECT friend_id, name, username, avatar_url, last_share_time, local_friend_id FROM friends", null);
				while (cursor.moveToNext()) {
					Friend.Builder friend = new Friend.Builder();
					int friend_id = cursor.getInt(0);
					if (friend_id > 0) friend.friend_id(friend_id+"");
					String name = cursor.getString(1);
					if (name != null) friend.name(name);
					String username = cursor.getString(2);
					if (username != null) friend.username(username);
					String avatar_url = cursor.getString(3);
					if (!StringUtils.isEmpty(avatar_url)) friend.avatar_url(new UrlString(avatar_url));
					long last_share_time = cursor.getLong(4);
					if (last_share_time > 0) friend.time_shared(Timestamp.fromMillis(last_share_time));
					
					Friend built = friend.build();
					if (built.friend_id != null) {
						out.restored(built);
						localFriendIdToFriend.put(cursor.getInt(5), built);
					}
				}
				cursor.close();
				
				// Ac Emails
				cursor = db.rawQuery("SELECT local_friend_id, email FROM ac_emails", null);
				while (cursor.moveToNext()) {
					AcEmail.Builder ace = new AcEmail.Builder();
					int local_friend_id = cursor.getInt(0);
					Friend friend = localFriendIdToFriend.get(local_friend_id);
					if (friend != null && friend.friend_id != null) ace.friend_id(friend.friend_id);
					if (friend != null && friend.time_shared != null) ace.time_shared(friend.time_shared);
					String email = cursor.getString(1);
					if (!StringUtils.isEmpty(email)) ace.email(new EmailString(email));
					out.restored(ace.build());
				}
				cursor.close();
				
				// Recent Friends
				ArrayNode recents = JsonUtil.stringToArrayNode(prefs.forUser("rfri", (String)null).get());
				if (recents != null && recents.size() > 0) {
					List<Friend> list = new ArrayList<>();
					for (JsonNode r : recents) {
						String friend_id = JsonUtil.getValueAsText(r, "friend_id", null);
						if (!StringUtils.isEmpty(friend_id)) {
							list.add(new Friend.Builder().friend_id(friend_id).build());
						} else {
							Friend f = localFriendIdToFriend.get(JsonUtil.getValueAsInt(r, "local_friend_id", 0));
							if (f != null) list.add(f);
						}
					}
					if (!list.isEmpty()) out.restored(new RecentFriends.Builder().recent_friends(list).build());
				}
			}
			
			private void items(SQLiteDatabase db, int maxItems, AtomicInteger itemCounter, DumbStorage.ThingCallback things) throws Exception {
				// Some columns were added in 7.0 and we support upgrading from 6.7+ so make sure they are there so we don't throw errors below
				if (!SqliteUtil.columnExists(db, "items", "is_index")) db.execSQL("ALTER TABLE items ADD COLUMN is_index INTEGER");
				if (!SqliteUtil.columnExists(db, "items", "domain_metadata")) db.execSQL("ALTER TABLE items ADD COLUMN domain_metadata VARCHAR");
				if (!SqliteUtil.columnExists(db, "items", "listen_duration_estimate")) db.execSQL("ALTER TABLE items ADD COLUMN listen_duration_estimate INTEGER");
				if (!SqliteUtil.columnExists(db, "item_annotations", "patch")) db.execSQL("ALTER TABLE item_annotations ADD COLUMN patch VARCHAR");
				if (!SqliteUtil.columnExists(db, "item_annotations", "version")) db.execSQL("ALTER TABLE item_annotations ADD COLUMN version INTEGER NOT NULL DEFAULT 1");
				
				Map<String, Item> uniqueIdToItem = new HashMap<>();
				selectSafely(db, "SELECT " +
						"unique_id," +     // 0
						"item_id," +       // 1
						"resolved_id," +   // 2
						"given_url," +     // 3
						"url," +           // 4  resolved_url
						"title," +         // 5
						"time_added," +    // 6
						"excerpt," +       // 7
						"video," +         // 8
						"image," +         // 9
						"is_article," +    // 10
						"word_count," +    // 11
						"offline_web," +   // 12
						"offline_text," +  // 13
						"encoding," +      // 14
						"mime," +          // 15
						"favorite," +      // 16
						"status," +        // 17
						"badge_group_id," +// 18
						"amp_url," +       // 19
						"top_image_url," + // 20
						"is_index," +      // 21
						"domain_metadata," + // 22
						"listen_duration_estimate " + // 23
						" FROM items", "ORDER BY time_added DESC", cursor -> {
							if (uniqueIdToItem.size() >= maxItems) return;

							Item.Builder item = new Item.Builder();
							// Item fields
							String item_id = cursor.getString(1);
							if (item_id != null) item.item_id(item_id);
							String given_url = cursor.getString(3);
							if (given_url != null) item.given_url(new UrlString(given_url));
							// Personal fields
							long time_added = cursor.getLong(6);
							if (time_added > 0) item.time_added(new Timestamp(time_added));
							item.favorite(cursor.getInt(16) == 1);
							item.status(ItemStatus.find(cursor.getInt(17)));
							// Offline fields
							item.offline_web(OfflineStatus.find(cursor.getInt(12)));
							item.offline_text(OfflineStatus.find(cursor.getInt(13)));
							item.encoding(cursor.getString(14));
							item.mime_type(cursor.getString(15));
							// Resolved
							String resolved_id = cursor.getString(2);
							if (resolved_id != null) {
								item.resolved_id(resolved_id);
								String resolved_url = cursor.getString(4);
								if (!StringUtils.isBlank(resolved_url)) item.resolved_url(new UrlString(resolved_url));
								item.excerpt(cursor.getString(7));
								item.has_video(Videoness.find(cursor.getInt(8)));
								item.has_image(Imageness.find(cursor.getInt(9)));
								item.is_article(cursor.getInt(10) == 1);
								item.word_count(cursor.getInt(11));
								String title = cursor.getString(5);
								if (title != null) item.title(title);
								item.badge_group_id(GroupId.create(cursor.getInt(18)));
								String amp_url = cursor.getString(19);
								if (!StringUtils.isEmpty(amp_url)) item.amp_url(new UrlString(amp_url));
								String top_image_url = cursor.getString(20);
								if (!StringUtils.isEmpty(top_image_url)) item.top_image_url(new UrlString(top_image_url));
								item.is_index(cursor.getInt(21) == 1);
								DomainMetadata domain_metadata = DomainMetadata.from(JsonUtil.stringToObjectNode(cursor.getString(22)), V3Source.JSON_CONFIG);
								if (domain_metadata != null) item.domain_metadata(domain_metadata);
								item.listen_duration_estimate(cursor.getInt(23));
							}
							uniqueIdToItem.put(cursor.getString(0), item.build());
							itemCounter.incrementAndGet();
					});
				processItemMeta(db, uniqueIdToItem, "SELECT unique_id, annotation_id, quote, patch, version FROM item_annotations", (cursor, item) -> {
					List<Annotation> list = Safe.nonNullCopy(item.annotations);
					list.add(new Annotation.Builder()
							.annotation_id(cursor.getString(1))
							.quote(cursor.getString(2))
							.patch(cursor.getString(3))
							.version(cursor.getInt(4))
							.build());
					return item.builder().annotations(list).build();
				});
				processItemMeta(db, uniqueIdToItem, "SELECT unique_id, author_id, name, url FROM item_authors", (cursor, item) -> {
					List<Author> list = Safe.nonNullCopy(item.authors);
					list.add(new Author.Builder()
							.author_id(cursor.getInt(1))
							.name(cursor.getString(2))
							.url(cursor.getString(3))
							.build());
					return item.builder().authors(list).build();
				});
				processItemMeta(db, uniqueIdToItem, "SELECT unique_id, image_id, src, caption, credit, width, height FROM item_images", (cursor, item) -> {
					List<Image> list = Safe.nonNullCopy(item.images);
					list.add(new Image.Builder()
							.image_id(cursor.getInt(1))
							.src(cursor.getString(2))
							.caption(cursor.getString(3))
							.credit(cursor.getString(4))
							.width(cursor.getInt(5))
							.height(cursor.getInt(6))
							.build());
					return item.builder().images(list).build();
				});
				processItemMeta(db, uniqueIdToItem, "SELECT unique_id, video_id, src, vid, width, height, length, type FROM items_videos", (cursor, item) -> {
					List<Video> list = Safe.nonNullCopy(item.videos);
					list.add(new Video.Builder()
							.video_id(cursor.getInt(1))
							.src(cursor.getString(2))
							.vid(cursor.getString(3))
							.width(cursor.getInt(4))
							.height(cursor.getInt(5))
							.length(cursor.getInt(6))
							.type(VideoType.create(cursor.getInt(7)))
							.build());
					return item.builder().videos(list).build();
				});
				processItemMeta(db, uniqueIdToItem, "SELECT unique_id, share_id, from_friend_id, comment, quote, time_shared, time_ignored, status FROM shared_items", (cursor, item) -> {
					List<SharedItem> list = Safe.nonNullCopy(item.shares);
					list.add(new SharedItem.Builder()
							.share_id(cursor.getString(1))
							.from_friend_id(cursor.getString(2))
							.comment(cursor.getString(3))
							.quote(cursor.getString(4))
							.time_shared(new Timestamp(cursor.getInt(5)))
							.time_ignored(new Timestamp(cursor.getInt(6)))
							.status(SharedItemStatus.find(cursor.getInt(7)))
							.build());
					return item.builder().shares(list).build();
				});
				processItemMeta(db, uniqueIdToItem, "SELECT unique_id, json FROM item_posts", (cursor, item) -> {
					List<Post> list = Safe.nonNullCopy(item.posts);
					list.add(Post.from(Modeller.toObjectNode(cursor.getString(1)), V3Source.JSON_CONFIG));
					return item.builder().posts(list).build();
				});
				processItemMeta(db, uniqueIdToItem, "SELECT unique_id, tag FROM item_tags", (cursor, item) -> {
					List<Tag> list = Safe.nonNullCopy(item.tags);
					Tag tag = TagUtil.clean(cursor.getString(1));
					if (tag != null) {
						list.add(TagUtil.clean(cursor.getString(1)));
						return item.builder().tags(list).build();
					} else {
						return item;
					}
				});
				processItemMeta(db, uniqueIdToItem, "SELECT unique_id, view, page, node_index, percent, time_updated, section, time_spent FROM scroll", (cursor, item) -> {
					Map<String, Position> map = Safe.nonNullCopy(item.positions);
					int view = cursor.getInt(1);
					map.put(view+"", new Position.Builder()
							.view(PositionType.find(view))
							.page(cursor.getInt(2))
							.node_index(cursor.getInt(3))
							.percent(cursor.getInt(4))
							.time_updated(new Timestamp(cursor.getLong(5)))
							.section(cursor.getInt(6))
							.time_spent(cursor.getInt(7))
							.build());
					return item.builder().positions(map).build();
				});
				
				// Convert assets
				assets(db, uniqueIdToItem, assets);
				
				// Pass up all items
				for (Thing t : uniqueIdToItem.values()) {
					things.restored(t);
				}
			}
			
			private void assets(SQLiteDatabase db, Map<String, Item> uniqueIdToItem, Assets assets) throws Exception {
				// Rename item markup folders
				AssetDirectory assetDirectory = assets.getAssetDirectory();
				int i = 0;
				for (Map.Entry<String, Item> e : uniqueIdToItem.entrySet()) {
					Item item = e.getValue();
					String unique_id = e.getKey();
					File renameTo = new File(assetDirectory.folderPathFor(item));
					File renameFrom = new File(renameTo.getParentFile(), unique_id);
					if (renameTo.exists()) continue;
					if (!renameFrom.exists()) continue;
					try {
						FileUtils.moveDirectory(renameFrom, renameTo);
					} catch (Throwable t) {
						// In the wild we saw some errors here and it appears that a very small number of devices
						// have their offline cache on a removable device that we have read permission but not write permission.
						// So there is nothing we can do here, we can't rename, delete, etc.  We aren't in a good position to prompt to them to resolve some how.
						// Given this is a very small number of users, we'll just ignore it.  These users will likely encounter storage errors when opening
						// items. But it is likely they were already having errors in previous versions of the app.
						// We will at least continue to report the error so we can see how frequently this occurs and if we need to adjust.
						try {
							if (!reportedAssetError) {
								boolean canCopy = false;
								try {
									FileUtils.copyDirectory(renameFrom, renameTo);
									canCopy = true;
								} catch (Throwable ignore) {}
								String state = "";
								int size = -1;
								try {
									AndroidStorageLocation loc = assetDirectory.getStorageLocation();
									state = loc.getType() + " " + loc.getState() + " " + loc.getPath();
									File[] contents = renameFrom.listFiles();
									size = contents != null ? contents.length : 0;
								} catch (Throwable ignore) {}
								throw new RuntimeException("unable to rename markup dir " + uniqueIdToItem.size() + " " + size + " " + i + " " + state + " " + canCopy, t);
							} else {
								// We are skipping any further issues here since we already reported it.
							}
						} catch (Throwable skip) {
							// Only report it once
							if (!reportedAssetError) {
								reportedAssetError = true;
								errorReporter.reportError(skip);
							}
						}
					}
					i++;
				}
				
				// Copy asset records to the new database
				selectSafely(db, "SELECT asset_id, user, bytes, short_path FROM assets_users JOIN assets USING (asset_id) ", "ORDER BY asset_id", cursor -> {
					String user = cursor.getString(1);
					long bytes = cursor.getLong(2);
					String short_path = cursor.getString(3);
					String path = AssetsDatabase.convertShortPathToFullPath(assetDirectory, short_path);
					
					if (user.equals("temp") || user.equals("temp_ext")) {
						assets.registerAssetUser(path, AssetUser.forSession());
					} else if (user.equals("permanent")) {
						assets.registerAssetUser(path, AssetUser.forApp());
					} else if (NumberUtils.isDigits(user)) {
						Item item = uniqueIdToItem.get(user);
						if (item != null) {
							assets.registerAssetUser(path,
									AssetUser.forItem(item.time_added, item.idkey())
							);
						} else {
							return; // Skip this asset
						}
					} else {
						// This should be a parent asset
						String ref = null;
						Cursor c = db.rawQuery("SELECT short_path FROM assets WHERE asset_id = ?", new String[]{user.substring("asset".length())});
						if (c.moveToNext()) ref = c.getString(0);
						c.close();
						if (ref != null) {
							assets.registerAssetUser(path, AssetUser.forParentAsset(ref));
						} else {
							return; // Skip this asset
						}
					}
					
					assets.written(path, bytes);
				});
				assets.awaitAssetDatabaseChanges();
			}
		};
	}
	
	/** Selects in batches to avoid cursor size issues. */
	private static void selectSafely(SQLiteDatabase db, String query, String orderBy, Select select) throws Exception {
		int batchSize = 100;
		int offset = 0;
		int returned;
		do {
			returned = 0;
			Cursor c = db.rawQuery(query + " " + (orderBy != null ? orderBy : " ORDER BY rowid ") + " LIMIT " + batchSize + " OFFSET " + offset, null);
			while (c.moveToNext()) {
				select.select(c);
				returned++;
			}
			offset += batchSize;
			returned = c.getCount();
			c.close();
		} while (returned == batchSize);
	}
	interface Select {
		void select(Cursor cursor) throws Exception;
	}
	
	/** Helper for processing an item meta table. The queries first column should be the unique_id. */
	private static void processItemMeta(SQLiteDatabase db, Map<String, Item> uniqueIdToItem, String query, ItemMetaRow select) throws Exception {
		selectSafely(db, query, null, cursor -> {
			String unique_id = cursor.getString(0);
			Item item = uniqueIdToItem.get(unique_id);
			if (item != null) uniqueIdToItem.put(unique_id, select.select(cursor, item));
		});
	}
	interface ItemMetaRow {
		Item select(Cursor cursor, Item item);
	}
	
	/**
	 * The database represented by the old DbDelegate, ListOperation, etc.
	 * https://github.com/Pocket/Android/tree/7.12.0.0/Pocket/src/main/java/com/pocket/sdk/db
	 */
	private static class LegacyDatabase extends SQLiteOpenHelper {
		LegacyDatabase(Context context) {
			super(context, "ril", null, Integer.MAX_VALUE);
		}
		@Override public void onCreate(SQLiteDatabase db) {}
		@Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
	}
	
	@Override
	public void onUserPresent() {
		userBecamePresent = true;
	}

	@Override
	public void onUserGone(Context context) {
		showDialogOnActivityResume = true;
	}

	@Override
	public void onActivityResumed(Activity activity) {
		// Only show the upgrade dialog once per app open, don't reshow if they open other screens, only if they leave and return.
		if (dialog == null && showDialogOnActivityResume && !isMigrated.get()) {
			AbsPocketActivity on = AbsPocketActivity.from(activity);
			AlertDialog shown = AlertMessaging.showError(on, null, Safe.get((() -> on.getString(R.string.legacy_upgrading))), true, null, null, null);
			dialog = shown;
			if (dialog != null) {
				showDialogOnActivityResume = false;
				dialog.setOnDismissListener(dialog -> LegacyMigration.this.dialog = null);
				on.addOnLifeCycleChangedListener(new AbsPocketActivity.SimpleOnLifeCycleChangedListener() {
					@Override
					public void onActivityPause(AbsPocketActivity activity) {
						if (shown == dialog) dismissDialog();
						activity.app().threads().getHandler().post(() -> on.removeOnLifeCycleChangeListener(this)); // Post to avoid concurrent mod exceptions. Should modify this remove API to avoid that, but that is more changes to the rest of the app than i want to do in this patch.
					}
				});
			}
		}
	}

	private void dismissDialog() {
		AlertMessaging.dismissSafely(dialog, null);
		dialog = null;
	}

}
