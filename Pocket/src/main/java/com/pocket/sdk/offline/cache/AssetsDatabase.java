package com.pocket.sdk.offline.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.pocket.app.AppThreads;
import com.pocket.sdk.dev.ErrorHandler;
import com.pocket.util.android.thread.TaskPool;
import com.pocket.util.android.thread.TaskRunnable;
import com.pocket.util.java.Logs;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Manages a database of {@link Asset} files stored on disk, including their {@link AssetUser}s and the size in bytes.
 */
public class AssetsDatabase {
	
	private static final String NAME = "assets";
	
	private final Helper dbHelper;
	private final Object lock = new Object();
	private final List<Transaction> pending = new ArrayList<>();
	private final TaskPool pool;
	private final ErrorHandler errorHandler;
	private final Assets assets;
	
	private int session = 1;
	private boolean isSizeInvalidated;
	
	AssetsDatabase(Context context, AppThreads threads, ErrorHandler errorHandler, Assets assets) {
		this.dbHelper = new Helper(context, 2);
		this.errorHandler = errorHandler;
		this.assets = assets;
		this.pool = threads.newWakefulPool("asset_db", 1, 1, 0L, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * For thread safety this and its fields/methods should only be accessed from within a {@link #pool} thread.
	 */
	private static class Helper extends SQLiteOpenHelper {
		
		private final Context context;
		private SQLiteStatement addAsset;
		private SQLiteStatement addUser;
		private SQLiteStatement addUserParentAsset;
		private SQLiteStatement removeUser;
		private SQLiteStatement setBytes;
		
		Helper(Context context, int versionCode) {
			super(context, NAME, null, versionCode);
			this.context = context;
		}
		
		public File getPath() {
			return context.getDatabasePath(getDatabaseName());
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.beginTransaction();
			
			db.execSQL("CREATE TABLE assets (" +
					"asset_id INTEGER PRIMARY KEY," +
					"bytes INTEGER NOT NULL," +
					"short_path VARCHAR NOT NULL," + // `short_path` is the local path to the file, shortened by {@link AssetQuery#convertFullPathToShortPath(String)}. To restore the full absolute path, pass its value to {@link AssetQuery#convertShortPathToFullPath(String)}.
					"UNIQUE (short_path) ON CONFLICT IGNORE" +
					")");
			
			db.execSQL("CREATE TABLE asset_users (" +
					"asset_id INTEGER NOT NULL," +
					"type VARCHAR NOT NULL," +
					"user VARCHAR NOT NULL," +
					"priority INTEGER NOT NULL," +
					"PRIMARY KEY (asset_id, type, user))");

			db.execSQL("CREATE INDEX userindex ON asset_users (user)"); // See note in onUpgrade
			
			db.setTransactionSuccessful();
			db.endTransaction();
		}
		
		@Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// version 2
			db.execSQL("CREATE INDEX IF NOT EXISTS userindex ON asset_users (user)"); // Added in version 2 to greatly speed up an idkey migration, but it should be useful outside of that anyways because we do other lookups by user frequently.
		}
		
		@Override
		public synchronized void close() {
			addAsset = close(addAsset);
			addUser = close(addUser);
			addUserParentAsset = close(addUserParentAsset);
			removeUser = close(removeUser);
			setBytes = close(setBytes);
			super.close();
		}
		
		private static SQLiteStatement close(SQLiteStatement statement) {
			if (statement != null) statement.close();
			return null;
		}
	}
	
	/**
	 * Delete the database.
	 * This is a blocking file/disk operation.
	 */
	public void clear(Context context) {
		FutureTask task;
		
		synchronized (lock) {
			session++;
		}
		pool.cancelAllUntilEmpty();
		// Close on the worker thread to ensure thread safety and avoid race conditions
		task = pool.submit(TaskRunnable.simple(() -> {
			dbHelper.close();
			context.deleteDatabase(NAME);
			assets.setCacheSize(new Size(0,0));
		}));
		// Await the close
		try {
			task.get();
		} catch (Exception ignore) {}
	}
	
	/**
	 * Add an asset and asset user.
	 * If the asset already exists, this just adds the new user for it.
	 * This is asynchronous.
	 *
	 * @param user The user of the asset
	 * @param path The file path to the asset
	 */
	public void add(AssetUser user, String path) {
		if (user == null || user.user == null) {
			errorHandler.reportOnProductionOrThrow(new RuntimeException("missing user"));
			return;
		}
		
		transaction(db -> {
			String shortPath = convertFullPathToShortPath(assets.getAssetDirectory(), path);
			
			addAsset(db, shortPath);
			addUser(db, shortPath, user);
		});
	}
	
	private void addAsset(SQLiteDatabase db, String shortPath) {
		if (dbHelper.addAsset == null) dbHelper.addAsset = db.compileStatement("INSERT INTO assets (short_path, bytes) VALUES (?,?)"); // Can be in INSERT because the schema will do an IGNORE on CONFLICTs
		SQLiteStatement sql = dbHelper.addAsset;
		sql.clearBindings();
		sql.bindString(1, shortPath);
		sql.bindLong(2, 0);
		sql.executeInsert();
	}
	
	private void addUser(SQLiteDatabase db, String shortPath, AssetUser user) {
		SQLiteStatement sql;
		if (dbHelper.addUser == null) dbHelper.addUser = db.compileStatement(
				"REPLACE INTO asset_users (asset_id, type, user, priority) " +
						"VALUES ((SELECT asset_id FROM assets WHERE short_path = ?), ?, ?, ?)");
		sql = dbHelper.addUser;
		sql.clearBindings();
		sql.bindString(1, shortPath);
		sql.bindString(2, user.type);
		sql.bindString(3, user.user);
		sql.bindLong(4, user.priority);
		sql.executeInsert();
	}
	
	/**
	 * Remove this asset user.
	 * This is asynchronous.
	 */
	public void removeUser(AssetUser user) {
		transaction(db -> {
			if (dbHelper.removeUser == null) dbHelper.removeUser = db.compileStatement("DELETE FROM asset_users WHERE type = ? AND user = ?");
			SQLiteStatement sql = dbHelper.removeUser;
			sql.clearBindings();
			sql.bindString(1, user.type);
			sql.bindString(2, user.user);
			sql.executeUpdateDelete();
		});
	}
	
	/**
	 * Set the size, in bytes, of an asset. This is asynchronous.
	 * @param path The full, absolute file path of the asset.
	 */
	public void setBytes(String path, long bytes) {
		transaction(db -> {
			final String shortPath = convertFullPathToShortPath(assets.getAssetDirectory(), path);
			addAsset(db, shortPath);
			
			if (dbHelper.setBytes == null) dbHelper.setBytes = db.compileStatement("UPDATE assets SET bytes = ? WHERE short_path = ?");
			SQLiteStatement sql = dbHelper.setBytes;
			sql.clearBindings();
			sql.bindLong(1, bytes);
			sql.bindString(2, shortPath);
			sql.executeUpdateDelete();
			synchronized (lock) {
				isSizeInvalidated = true;
			}
		});
	}
	
	public List<AssetUser> getAssetUsers(String assetUserType) {
		List<AssetUser> users = new ArrayList<>();
		try {
			transaction(db -> {
				Cursor c = db.rawQuery("SELECT DISTINCT type, user, priority FROM asset_users WHERE type = ?", new String[]{assetUserType});
				while (c.moveToNext()) {
					users.add(new AssetUser(c.getString(0), c.getString(1), c.getLong(2)));
				}
				c.close();
			}).get();
		} catch (Throwable t) {
			throw new RuntimeException(t); // Not really expected, so don't force callers to handle.
		}
		return users;
	}
	
	/**
	 * Delete any {@link AssetUser#forParentAsset(Asset)} that reference unused users/assets.
	 */
	public void cleanUnusedParentAssets() {
		transaction(db -> {
			int deletes;
			SQLiteStatement statement = db.compileStatement(
					// Find any asset users that are assets themselves, and no longer exist.
					"DELETE FROM asset_users WHERE type = ? AND user IN " +
					" (SELECT user FROM asset_users " +
						" LEFT OUTER JOIN assets ON assets.short_path = asset_users.user " +
						" WHERE asset_users.type = ? AND assets.asset_id IS NULL" +
					" )");
			do {
				statement.clearBindings();
				statement.bindString(1, AssetUser.PARENT_ASSET_TYPE);
				deletes = statement.executeUpdateDelete();
			} while (deletes > 0);
			dropUnusedTable(db);
		});
	}
	
	/**
	 * Starting at the provided root directory, scan every file and sub file, looking for files that
	 * do not have any registered asset users. For such files, they will be removed from the asset database
	 * and returned in the callback as unused files that can be deleted safely. This process could be slow
	 * and expensive for large caches. If another database transaction is added while this is running, it will
	 * interrupt this process to allow the other transaction to run. It will not reschedule itself, but will
	 * indicate it was interrupted in the callback.
	 * @param root
	 * @param callback
	 * @return
	 */
	private FutureTask cleanManually(File root, CleanCallback callback) {
		return transaction(db -> {
			String[] args = new String[1];
			List<File> unused = new ArrayList<>();
			Iterator<File> it = FileUtils.iterateFiles(root, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
			while (it.hasNext()) {
				boolean interrupt;
				synchronized (lock) {
					interrupt = !pending.isEmpty();
				}
				if (interrupt) {
					isSizeInvalidated = !unused.isEmpty();
					callback.onCleaned(unused, true);
					return;
				}
				
				File file = it.next();
				if (file.isDirectory()) {
					if (file.list().length == 0) {
						unused.add(file);
					}
					continue;
				}
				
				args[0] = convertFullPathToShortPath(assets.getAssetDirectory(), file.getAbsolutePath());
				Cursor c = db.rawQuery("SELECT EXISTS(SELECT 1 FROM asset_users WHERE short_path = ? LIMIT 1)", args);
				if (!c.moveToNext() || c.getInt(0) == 0) {
					unused.add(file);
					db.execSQL("DELETE FROM assets WHERE short_path = ?", args);
				}
				c.close();
			}
			isSizeInvalidated = !unused.isEmpty();
			callback.onCleaned(unused, false);
		});
	}
	
	/**
	 * Drop the `temp_remove_unused` table
	 */
	private void dropUnusedTable(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS temp_remove_unused");
	}
	
	public interface CleanCallback {
		void onCleaned(List<File> unused, boolean interrupted);
	}
	
	/**
	 * Remove unused assets. This should be faster than {@link #cleanManually(File, CleanCallback)} but won't catch files that ended up on disk by accident.
	 * @return A list of a unused assets that were removed from the database (but their files may still remain on disk)
	 */
	public List<File> cleanUnusedAssets() throws ExecutionException, InterruptedException {
		List<File> unused = new ArrayList<>();
		transaction(db -> unused.addAll(cleanUnusedAssets(db))).get();
		return unused;
	}
	
	/**
	 * Removes any asset entries that do not have asset users.
	 * @return A list of assets that were removed.
	 */
	private List<File> cleanUnusedAssets(SQLiteDatabase db) {
		dropUnusedTable(db);
		db.execSQL("CREATE TEMPORARY TABLE temp_remove_unused (" +
				" asset_id INTEGER NOT NULL, " +
				" short_path VARCHAR NOT NULL)");
		db.execSQL(
				"INSERT INTO temp_remove_unused " +
						" SELECT DISTINCT asset_id, short_path" +
						" FROM assets " +
						" LEFT OUTER JOIN asset_users" +
						" USING (asset_id)" +
						" WHERE asset_users.user IS NULL");
		
		// Now extract the list of assets that can be deleted
		final List<File> unused = new ArrayList<>();
		final int limit = 400; // Avoids large cursors which can cause exceptions on Android
		int offset = 0;
		boolean doAnotherBatch;
		do {
			boolean interrupt;
			synchronized (lock) {
				interrupt = !pending.isEmpty();
			}
			if (interrupt) {
				return unused;
			}
			
			int count = 0;
			Cursor cursor = db.rawQuery("SELECT short_path FROM temp_remove_unused ORDER BY short_path ASC LIMIT ? OFFSET ? ",
					new String[]{String.valueOf(limit), String.valueOf(offset)});
			while (cursor.moveToNext()) {
				count++;
				try {
					String path = convertShortPathToFullPath(assets.getAssetDirectory(), cursor.getString(0));
					unused.add(new File(path));
				} catch (Throwable ignore) {} // If we fail to convert the file, ignore it and keep going.
			}
			cursor.close();
			offset += count;
			doAnotherBatch = count == limit;
		} while (doAnotherBatch);
		
		// Now actually remove them from the database.
		db.execSQL("DELETE FROM assets WHERE asset_id IN (SELECT asset_id FROM temp_remove_unused)");
		
		dropUnusedTable(db);
		
		synchronized (lock) {
			isSizeInvalidated = !unused.isEmpty();
		}
		return unused;
	}
	
	/**
	 * Remove the lowest priority asset users and their assets until at least `bytes` has been freed.
	 * @param bytes The minimum number of bytes to remove
	 * @return Assets whose files can be deleted and are no longer tracked in this database.
	 */
	Trimmed trim(long bytes) throws ExecutionException, InterruptedException {
		final Trimmed trimmed = new Trimmed();
		transaction(db -> {
			long start = calculateSize(db).assets;
			long target = start - bytes;
			// Remove the lowest priority asset user, one at a time, until we've freed enough space.
			// In order to be able to tell listeners their user was trimmed, we have to do a select as well...
			SQLiteStatement delete = db.compileStatement("DELETE FROM asset_users WHERE type = ? AND user = ?");
			do {
				AssetUser found = null;
				Cursor c = db.rawQuery("SELECT type, user, priority FROM asset_users ORDER BY priority ASC LIMIT 1", null);
				if (c.moveToNext()) {
					found = new AssetUser(c.getString(0), c.getString(1), c.getLong(2));
					trimmed.users.add(found);
				}
				c.close();
				if (found != null) {
					delete.clearBindings();
					delete.bindString(1, found.type);
					delete.bindString(2, found.user);
					delete.executeUpdateDelete();
					trimmed.assets.addAll(cleanUnusedAssets(db));
				} else {
					break; // Nothing further to remove.
				}
			} while (calculateSize(db).assets > target);
		}).get();
		return trimmed;
	}
	
	/**
	 * Waits for any pending work to be written to the database.
	 */
	public void await() throws InterruptedException {
		try {
			pool.submit(() -> {}).get();
		} catch (ExecutionException e) {
			// This is not expected since the runnable above does nothing, if it happens, ignore it.
		}
	}

	/**
	 * Find all asset_users.user that match the provided keys to update them to the provided values.
	 * Find any text within all assets.short_path text and replace them with the provided values.
	 * Only intended for a data migration fix needed for idkeys ({@link com.pocket.sdk2.api.legacy.IdkeyMigration}, could eventually be removed
	 * @param oldToNew Old values as the keys, new as the values.
	 * @param markupOldKeys A list of old idkeys that were found in {@link AssetDirectory#getMarkupDirectory()}, this helps this be faster by knowing what markup pages are expected to be found in the `assets` table.
	 */
	@Deprecated
	public void fixIdKeys(Map<String, String> oldToNew, Set<String> markupOldKeys) {
		// Grab list of thing and item users so we can filter to only these for performance
		Set<AssetUser> users = new HashSet<>();
		users.addAll(getAssetUsers("thing")); // PocketSingleton.ASSET_USER_THING_TYPE
		users.addAll(getAssetUsers("item")); // PocketSingleton.ASSET_USER_ITEM_TYPE  Note: opt'd not to use the constants because this is used as part of a migration, so this is what it was called at the time of the migration
		transaction(db -> {
			SQLiteStatement sql = db.compileStatement("UPDATE asset_users SET user = ? WHERE user = ?");
			for (AssetUser user : users) {
				String newkey = oldToNew.get(user.user);
				if (newkey == null) continue;
				sql.bindString(1, newkey);
				sql.bindString(2, user.user);
				sql.executeUpdateDelete();
			}
			sql.close();

			sql = db.compileStatement("UPDATE assets SET short_path = ? WHERE short_path = ?");
			for (String oldkey : markupOldKeys) {
				String newkey = oldToNew.get(oldkey);
				if (newkey == null) continue;
				sql.bindString(1, "RIL_pages/"+newkey+"/text.html");
				sql.bindString(2, "RIL_pages/"+oldkey+"/text.html");
				sql.executeUpdateDelete();
				sql.bindString(1, "RIL_pages/"+newkey+"/web.html");
				sql.bindString(2, "RIL_pages/"+oldkey+"/web.html");
				sql.executeUpdateDelete();
			}
			sql.close();
		});
	}

	/**
	 * Add a change to the queue. The change will be run on the {@link #pool} thread
	 * sometime in the future.
	 */
	private FutureTask transaction(Transaction transaction) {
		synchronized (lock) {
			// Add to the queue
			pending.add(transaction);
			
			// Kick off a thread to process the queue.
			// Since offline downloading can trigger hundreds of asset database changes in rapid succession
			// This will loop on the processing thread until the queue is empty.
			// This helps batch multiple changes into a single database transaction.
			int session = this.session;
			return pool.submit(TaskRunnable.simple(() -> {
				synchronized (lock) {
					if (session != this.session || pending.isEmpty()) return;
				}
				
				SQLiteDatabase db = dbHelper.getWritableDatabase();
				db.beginTransaction();
				
				try {
					Transaction t;
					do {
						synchronized (lock) {
							t = pending.isEmpty() ? null : pending.remove(0);
						}
						if (t != null) {
							t.transact(db);
						}
					} while (t != null);
					
					boolean calculateSize;
					synchronized (lock) {
						calculateSize = isSizeInvalidated;
					}
					if (calculateSize) {
						assets.setCacheSize(calculateSize(db));
					}
					
					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
				}
			}));
		}
	}
	
	private Size calculateSize(SQLiteDatabase db) {
		long bytes;
		Cursor cursor = db.rawQuery("SELECT SUM(bytes) as total FROM assets", null);
		if (cursor.moveToNext()) {
			bytes = cursor.getLong(cursor.getColumnIndexOrThrow("total"));
		} else {
			bytes = 0;
		}
		cursor.close();
		
		long dbSize;
		try {
			dbSize = FileUtils.sizeOf(dbHelper.getPath());
		} catch (Throwable throwable) {
			// If missing or other error, just return 0
			Logs.printStackTrace(throwable);
			dbSize = 0;
		}
		return new Size(bytes, dbSize);
	}
	
	static class Size {
		final long assets;
		final long db;
		Size(long assets, long db) {
			this.assets = assets;
			this.db = db;
		}
	}
	
	static class Trimmed {
		final List<AssetUser> users = new ArrayList<>();
		final List<File> assets = new ArrayList<>();
	}
	
	interface Transaction {
		/** Perform a change in the database. */
		void transact(SQLiteDatabase db) throws Exception;
	}
	
	public static String convertFullPathToShortPath(AssetDirectory assetDirectory, String path) {
		return path.substring(assetDirectory.getOfflinePath().length() + 1); // +1 to remove the leading slash
	}
	
	public static String convertShortPathToFullPath(AssetDirectory assetDirectory, String path) {
		return assetDirectory.getOfflinePath() + File.separator + path;
	}
	
	/** A simpler interface of {@link Assets} to limit the scope of what this class needs and has access to. */
	interface Assets {
		void setCacheSize(Size calculateSize);
		AssetDirectory getAssetDirectory()throws AssetDirectoryUnavailableException;
	}
	
}
