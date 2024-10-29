package com.pocket.sdk.offline.cache;

import com.pocket.app.App;
import com.pocket.app.session.AppSession;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sync.thing.Thing;

/**
 * Represents something that needs an {@link Asset}.
 * Also has several static methods of all known asset users so they are all in one place to choose from.
 * @see Assets
 */
public class AssetUser {
	
	public static final long PRIORITY_LOW = 0;
	public static final long PRIORITY_HIGH = Long.MAX_VALUE;
	static final String PARENT_ASSET_TYPE = "asset";
	
	/**
	 * See {@link AppSession#assetUser()}
	 */
	public static AssetUser forSession() {
		return App.getApp().session().assetUser();
	}

	/**
	 * Permanent app resources.
	 * Will only be removed if the cache is completely cleared or the user logs out.
	 * Be careful with this one...
	 */
	public static AssetUser forApp() {
		return new AssetUser("app", "", PRIORITY_HIGH);
	}

	/**
	 * See {@link com.pocket.app.PocketSingleton#assetUser(Timestamp, String)}
	 */
	public static AssetUser forItem(Timestamp timeAdded, String idKey) {
		return App.getApp().pocketSingleton().assetUser(timeAdded, idKey);
	}
	
	/**
	 * See {@link com.pocket.app.PocketSingleton#assetUser(Thing)}
	 */
	public static AssetUser forThing(Thing thing) {
		return App.getApp().pocketSingleton().assetUser(thing);
	}
	
	/**
	 * For when another asset depends on or needs another asset (like a css file that references an image)
	 */
	public static AssetUser forParentAsset(Asset parent) throws AssetDirectoryUnavailableException {
		return new AssetUser(PARENT_ASSET_TYPE, AssetsDatabase.convertFullPathToShortPath(App.getApp().assets().getAssetDirectory(), parent.local.getAbsolutePath()), PRIORITY_HIGH);
	}
	
	/**
	 * A variant of {@link #forParentAsset(Asset)} that is only needed for {@link com.pocket.sdk2.api.legacy.LegacyMigration}
	 * and shouldn't be used otherwise and can be removed when the migration is.
	 * @param shortPath The short_path (see {@link AssetsDatabase#convertFullPathToShortPath(AssetDirectory, String)}) of the asset which references other assets.
	 */
	public static AssetUser forParentAsset(String shortPath) {
		return new AssetUser(PARENT_ASSET_TYPE, shortPath, PRIORITY_HIGH);
	}
	
	/**
	 * The unique identifier for this user.
	 */
	public final String type;
	public final String user;
	public final long priority;
	
	public AssetUser(String type, String user, long priority) {
		this.type = type;
		this.user = user;
		this.priority = priority;
	}

	@Override
	public String toString() {
		return "AssetUser [type=" + type + ", key=" + user + "]";
	}
}
