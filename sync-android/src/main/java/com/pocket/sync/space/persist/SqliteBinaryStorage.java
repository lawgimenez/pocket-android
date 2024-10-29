package com.pocket.sync.space.persist;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteBlobTooBigException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.pocket.sync.action.Action;
import com.pocket.sync.source.JsonConfig;
import com.pocket.sync.source.result.RemotePriority;
import com.pocket.sync.source.threads.ThreadPools;
import com.pocket.sync.space.Holder;
import com.pocket.sync.spec.Spec;
import com.pocket.sync.spec.Syncable;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.binary.ByteReader;
import com.pocket.sync.value.binary.ByteWriter;
import com.pocket.sync.value.protect.StringEncrypter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An implementation using sqlite. Stores things as blobs of byte data from {@link Thing#compress(ByteWriter)}.
 */
public class SqliteBinaryStorage implements DumbStorage {

	/** No special Json parsing configuration rules are required here */
	private static final JsonConfig JSON_CONFIG = Syncable.NO_ALIASES;

	private static final long SAFE_CURSOR_LIMIT = (long) (2L*1024L*1024L * 0.75); // Android cursor window is 2mb, but just for safety, only use up to 0.75% of it. See comments below where this is used for more info.

	private final Helper helper;
	private final ObjectMapper json;
	private final StringEncrypter encrypter;
	/** A single threaded pool for primary use and for writes when we need to keep the order they were received. */
	private final ThreadPools.Pool core;
	/** An object to lock on during restore(). */
	private final Object restoreLock = new Object();
	private final Object transactionLock = new Object();
	/** A cache of SqliteStatements */
	private final Statements statements = new Statements();
	/** If we know for sure the database is empty. Can help skip extra work on app start. */
	private boolean isEmpty;
	
	public SqliteBinaryStorage(Context context, String name, ThreadPools threads, ObjectMapper json, StringEncrypter encrypter) {
		this.helper = new Helper(context, name);
		this.json = json;
		this.encrypter = encrypter;
		this.core = threads.newPool(1, 1, 0L, TimeUnit.MILLISECONDS, false);
	}
	
	private class Helper extends SQLiteOpenHelper {
		
		private Helper(Context context, String name) {
			super(context, name, null, 1);
			setWriteAheadLoggingEnabled(true);
		}
		
		@Override
		public void onConfigure(SQLiteDatabase db) {
			super.onConfigure(db);
			db.execSQL("PRAGMA synchronous=FULL");  // Going with the safest, least likely to corrupt, option when using write ahead logging (WAL).
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			isEmpty = true;
			db.execSQL("CREATE TABLE things (" +
					"idkey VARCHAR NOT NULL PRIMARY KEY, " +
					"type VARCHAR NOT NULL, " +
					"data BLOB NOT NULL " +
					")");
			
			db.execSQL("CREATE TABLE actions (" +
					"data VARCHAR NOT NULL, " +
					"priority VARCHAR NOT NULL " +
					")");
			
			db.execSQL("CREATE TABLE invalids (" +
					"idkey VARCHAR NOT NULL" +
					")");
			
			db.execSQL("CREATE TABLE holders (" +
					"holder VARCHAR NOT NULL, " +
					"hold VARCHAR NOT NULL, " +
					"type VARCHAR NOT NULL, " +
					"data VARCHAR NOT NULL " +
					")");
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
		}
	}
	
	interface Transaction {
		void transact(SQLiteDatabase db) throws Exception;
	}
	
	private void transaction(WriteSuccess onSuccess, WriteFailure onFailure, Transaction transaction) {
		synchronized (transactionLock) {
			SQLiteDatabase db = helper.getWritableDatabase();
			db.beginTransactionNonExclusive();
			try {
				transaction.transact(db);
				db.setTransactionSuccessful();
				db.endTransaction();
				if (onSuccess != null) onSuccess.onSuccess();
				
			} catch (Throwable t) {
				db.endTransaction();
				if (onFailure != null) {
					onFailure.onFailure(t);
				} else {
					throw new RuntimeException(t);
				}
			}
		}
	}
	
	@Override
	public void restore(Spec spec, ThingCallback things, HolderCallback holders, ActionCallback actions, InvalidCallback invalids) {
		transaction(null, null, db -> {
			if (isEmpty) return; // Nothing to load
			
			/*
				In profiling, it was seen that about half the time was spent parsing things and another half was invoking
				the thing callback and whatever that callback does to set up those things internally.
				
				So this uses two threads and is able to split up the work so that callback processing and the thing parsing
				can be done in parallel in separate threads. This can nearly double the speed of restoring a large 3k item account.
				
				We have the following threads:
				1. The calling thread, the one running this code
				2. The worker thread in `core`
				
				We'll use two queues in this process:
				1. `work` A queue of callables waiting to be run, this contains work like parsing data into Things or doing sqlite calls to get non-thing data.
				2. `parsed` A queue of things that are parsed and waiting to be sent to the callback
				
				We use ArrayLists and synchronized(restoreLock) to synchronize those queues instead of a BlockingQueue because it seemed to give a minor performance benefit.
				Not 100% why, but perhaps BlockingQueue is doing a bit more than we need in this case.
			 */
			
			List<Callable> work = new ArrayList<>();
			List<Thing> parsed = new ArrayList<>();
			AtomicBoolean complete = new AtomicBoolean(false);
			AtomicReference<Throwable> error = new AtomicReference<>();
			AtomicInteger remaining = new AtomicInteger(0);
			
			// Start the worker thread, it loops processing the queue until we are complete.
			core.submit(() -> {
				do {
					Callable worker;
					synchronized (restoreLock) {
						worker = work.isEmpty() ? null : work.remove(0);
					}
					if (worker != null) runRestoreWork(worker, remaining, error);
				} while (!complete.get());
			});

			// Grab thing data and send each to the queue, the worker thread will start working on them
			//
			// Note: Android's SQLite CursorWindow has a maximum size of 2mb.
			// In some extreme cases, a compressed thing could over this size. (ie. a LocalItems thing with 5000+ long urls)
			// The implementation below will check for this case and load it in chunks if needed.
			// TODO consider options that can avoid these large things in the first place.
			{
				/// This block of code is only needed for an idkey migration that had to happen, eventually it can be removed when nothing is passing in the idkeys callback anymore.
				IdkeyMigrator idkeyMigrator = things instanceof IdkeyMigrator ? (IdkeyMigrator) things : null;
				if (idkeyMigrator != null) idkeyMigrator.prep(db);

				// The vast vast majority of cases/users won't need to deal with oversized blobs, so use the fastest query assuming no oversizing, and fallback if needed.
				Cursor c = null;
				int read = 0; // Keeps track of how many we've read, so if it hits an oversized blob, we know where to pick up
				try {
					c = db.rawQuery("SELECT data, type FROM things ORDER BY rowid", null);
					while (c.moveToNext()) {
						ByteReader in = new ByteReader(c.getBlob(0));
						String type = c.getString(1);
						addRestoreWork(in, type, remaining, work, parsed, spec, read, idkeyMigrator);
						read++;
					}
				} catch (SQLiteBlobTooBigException e) {
					// For the remaining things, fallback to a slightly slower query that safely handles large blobs
					IOUtils.closeQuietly(c);
					c = db.rawQuery("SELECT substr(data,0,"+SAFE_CURSOR_LIMIT+"), type, length(data), rowid FROM things ORDER BY rowid LIMIT -1 OFFSET " + read, null);
					while (c.moveToNext()) {
						ByteReader in = new ByteReader(c.getBlob(0));
						String type = c.getString(1);
						long length = c.getLong(2);
						if (length > SAFE_CURSOR_LIMIT) {
							// Need to grab the remainder in chunks.
							long rowid = c.getLong(3);
							long offset = SAFE_CURSOR_LIMIT;
							int remainingChunks = (int) Math.ceil((length - offset) / (double)SAFE_CURSOR_LIMIT);
							while (remainingChunks > 0) {
								Cursor c2 = db.rawQuery("SELECT substr(data,"+offset+","+ SAFE_CURSOR_LIMIT+") FROM things WHERE rowid = " + rowid, null);
								c2.moveToNext();
								in.load(c2.getBlob(0)); // appends it to the byte stream
								c2.close();
								remainingChunks--;
								offset += SAFE_CURSOR_LIMIT;
							}
						}
						addRestoreWork(in, type, remaining, work, parsed, spec, read, idkeyMigrator);
					}
				} finally {
					// Other exceptions just fail/throw normally
					IOUtils.closeQuietly(c);
				}
			}

			// Add the the rest of the data types as work to process
			remaining.incrementAndGet();
			synchronized (restoreLock) {
				work.add(() -> {
					Multimap<Holder, Object> outHolders = MultimapBuilder.hashKeys().arrayListValues().build();
					Cursor c = db.rawQuery("SELECT holder, hold, type, data FROM holders", null);
					while (c.moveToNext()) {
						Holder holder = Holder.from(c.getString(1), c.getString(0));
						String data = c.getString(3);
						String type = c.getString(2);
						Object o;
						if (type != null && type.length() > 0) {
							o = spec.things().thing(type, json.getFactory().createParser(data), JSON_CONFIG);
						} else {
							o = data;
						}
						outHolders.put(holder, o);
					}
					c.close();
					holders.restored(outHolders);
					return null;
				});
			}
			remaining.incrementAndGet();
			synchronized (restoreLock) {
				work.add(() -> {
					Map<Action, RemotePriority> outActions = new HashMap<>();
					Cursor c = db.rawQuery("SELECT data, priority FROM actions", null);
					while (c.moveToNext()) {
						Action action = spec.actions().action((ObjectNode) json.readTree(c.getString(0)), JSON_CONFIG);
						RemotePriority priority = RemotePriority.fromKey(c.getString(1));
						outActions.put(action, priority);
					}
					c.close();
					actions.restored(outActions);
					return null;
				});
			}
			remaining.incrementAndGet();
			synchronized (restoreLock) {
				work.add(() -> {
					Set<String> outInvalids = new HashSet<>();
					Cursor c = db.rawQuery("SELECT idkey FROM invalids", null);
					while (c.moveToNext()) {
						outInvalids.add(c.getString(0));
					}
					c.close();
					invalids.restored(outInvalids);
					return null;
				});
			}
			
			// At this point, all work has been added to `work`
			// Use this calling thread to primarily focus on sending things to the callback
			// but if that queue is empty, it can be used to do 'work' as well.
			do {
				Thing send;
				Callable worker;
				synchronized (restoreLock) {
					send = parsed.isEmpty() ? null : parsed.remove(0);
					worker = send != null ? null : (work.isEmpty() ? null : work.remove(0));
				}
				if (send != null) {
					things.restored(send);
				} else if (worker != null) {
					runRestoreWork(worker, remaining, error);
				} else if (remaining.get() <= 0) {
					synchronized (restoreLock) {
						// One last check of the queue to avoid race conditions
						if (parsed.isEmpty()) complete.set(true);
					}
				}
			} while (!complete.get() && error.get() == null);
			
			if (error.get() != null) throw new RuntimeException(error.get());
		});
	}

	private void addRestoreWork(ByteReader data, String type, AtomicInteger remaining, List<Callable> work, List<Thing> parsed, Spec spec, int index, IdkeyMigrator idkeyMigrator) {
		remaining.incrementAndGet();
		synchronized (restoreLock) {
			work.add(() -> {
				Thing thing;
				try {
					thing = spec.things().thing(type, data);
				} catch (RuntimeException e) {
					Log.e(SqliteBinaryStorage.class.getSimpleName(), "sync engine failure: " + e.getMessage());
					return null;
				}
				if (thing != null) {
					thing = thing.unredact(encrypter);
					synchronized (restoreLock) {
						parsed.add(thing);
						if (idkeyMigrator != null) idkeyMigrator.parsed(index, thing);
					}
				}
				return null;
			});
		}
	}

	private void runRestoreWork(Callable worker, AtomicInteger countdown, AtomicReference<Throwable> errorOut) {
		try {
			worker.call();
			countdown.decrementAndGet();
		} catch (Exception e) {
			errorOut.compareAndSet(null, e);
		}
	}
	
	@Override
	public void store(
			Collection<Thing> addThings, Collection<Thing> removeThings,
			Collection<Pair<Holder, Object>> addHolders, Collection<Pair<Holder, Object>> removeHolders,
			Map<Action, RemotePriority> addActions, Collection<Action> removeActions,
			Collection<String> addInvalids, Collection<String> removeInvalids,
			WriteSuccess onSuccess, WriteFailure onFailure) {
		core.submit(() -> {
			transaction(onSuccess, onFailure, db -> {
				isEmpty = false;
				// Things
				// NOTE: There are some cases that could benefit from bulk statements (where you insert/remove many rows at once)
				// But that does complicate code so leaving it as a TODO if we want to further optimize those cases later.
				if (addThings != null) {
					ByteWriter buffer = new ByteWriter();
					SQLiteStatement sql = statements.insertThings != null ? statements.insertThings : (statements.insertThings = db.compileStatement("INSERT OR REPLACE INTO things (idkey, type, data) VALUES (?,?,?)"));
					for (Thing thing : addThings) {
						sql.bindString(1, thing.idkey());
						sql.bindString(2, thing.type());
						thing.flat().redact(encrypter).compress(buffer);
						sql.bindBlob(3, buffer.readByteArray());
						sql.executeInsert();
					}
				}
				if (removeThings != null) {
					SQLiteStatement sql = statements.removeThings != null ? statements.removeThings : (statements.removeThings = db.compileStatement("DELETE FROM things WHERE idkey = ?"));
					for (Thing thing : removeThings) {
						sql.bindString(1, thing.idkey());
						sql.executeUpdateDelete();
					}
				}
				
				if (addHolders != null) {
					SQLiteStatement sql = statements.insertHolders != null ? statements.insertHolders : (statements.insertHolders = db.compileStatement("INSERT INTO holders (holder, hold, type, data) VALUES (?,?,?,?)"));
					for (Pair<Holder, Object> e : addHolders) {
						sql.bindString(1, e.getKey().key());
						sql.bindString(2, e.getKey().hold().key);
						Object o = e.getValue();
						Thing match = o instanceof Thing ? ((Thing) o) : null;
						sql.bindString(3, match != null ? match.type() : "");
						sql.bindString(4, match != null ? match.toJson(JSON_CONFIG).toString() : (String) o); // TODO could use binary here instead of json?
						sql.executeInsert();
					}
				}
				if (removeHolders != null) {
					SQLiteStatement sql = statements.deleteHolders != null ? statements.deleteHolders : (statements.deleteHolders = db.compileStatement("DELETE FROM holders WHERE holder = ? AND type = ? AND data = ?"));
					for (Pair<Holder, Object> e : removeHolders) {
						sql.bindString(1, e.getKey().key());
						Object o = e.getValue();
						Thing match = o instanceof Thing ? ((Thing) o) : null;
						sql.bindString(2, match != null ? match.type() : "");
						sql.bindString(3, match != null ? match.toJson(JSON_CONFIG).toString() : (String) o);
						sql.executeUpdateDelete();
					}
				}
				
				if (addActions != null) {
					SQLiteStatement sql = statements.insertActions != null ? statements.insertActions : (statements.insertActions = db.compileStatement("INSERT INTO actions (data, priority) VALUES (?,?)"));
					for (Map.Entry<Action, RemotePriority> e : addActions.entrySet()) {
						sql.bindString(1, e.getKey().toJson(JSON_CONFIG).toString());
						sql.bindString(2, e.getValue().key);
						sql.executeInsert();
					}
				}
				if (removeActions != null) {
					SQLiteStatement sql = statements.deleteActions != null ? statements.deleteActions : (statements.deleteActions = db.compileStatement("DELETE FROM actions WHERE data = ?"));
					for (Action e : removeActions) {
						sql.bindString(1, e.toJson(JSON_CONFIG).toString());
						sql.executeInsert();
					}
				}
				
				if (addInvalids != null) {
					SQLiteStatement sql = statements.insertInvalids != null ? statements.insertInvalids : (statements.insertInvalids = db.compileStatement("INSERT INTO invalids (idkey) VALUES (?)"));
					for (String idkey : addInvalids) {
						sql.bindString(1, idkey);
						sql.executeInsert();
					}
				}
				if (removeInvalids != null) {
					SQLiteStatement sql = statements.deleteInvalids != null ? statements.deleteInvalids : (statements.deleteInvalids = db.compileStatement("DELETE FROM invalids WHERE idkey = ?"));
					for (String idkey : removeInvalids) {
						sql.bindString(1, idkey);
						sql.executeInsert();
					}
				}
			});
		});
	}
	
	@Override
	public void clear(WriteSuccess onSuccess, WriteFailure onFailure) {
		core.submit(() -> {
			transaction(onSuccess, onFailure, db -> {
				db.execSQL("DELETE FROM things");
				db.execSQL("DELETE FROM actions");
				db.execSQL("DELETE FROM holders");
				db.execSQL("DELETE FROM invalids");
				isEmpty = true;
			});
		});
	}
	
	@Override
	public void release() {
		core.stop(1, TimeUnit.MINUTES);
		helper.close();
	}
	
	private class Statements {
		SQLiteStatement insertThings;
		SQLiteStatement removeThings;
		SQLiteStatement deleteActions;
		SQLiteStatement insertActions;
		SQLiteStatement deleteHolders;
		SQLiteStatement insertHolders;
		SQLiteStatement deleteInvalids;
		SQLiteStatement insertInvalids;
	}

	/**
	 * Converts all idkeys currently in the database to whatever the new {@link Thing#idkey()} produces when invoked at runtime.
	 * @return An instance of the migrator. It only preps, it does not actually commit it yet. Use the {@link IdkeyMigrator#oldkeysToNew} if you need to do outside conversions and then invoke {@link IdkeyMigrator#commit()} to have the database update to the new idkeys.
	 * @deprecated Only intended for an idkey migration the pocket app had to do, can be removed later.
	 */
	@Deprecated
	public IdkeyMigrator migrateIdkeys(Spec spec) {
		IdkeyMigrator migrator = new IdkeyMigrator();
		restore(spec, migrator, migrator, migrator, migrator);
		return migrator;
	}

	/** See {@link #migrateIdkeys(Spec)} */
	@Deprecated
	public class IdkeyMigrator implements ThingCallback, HolderCallback, ActionCallback, InvalidCallback {

		private final List<String> idkeysByRowid = new ArrayList<>();
		public final Map<String, String> oldkeysToNew = new HashMap<>();

		// These indexes keep track of what is actually in the tables so we can skip converting idkeys that aren't in these tables
		private final Set<String> oldkeysInHolders = new HashSet<>();
		private final Set<String> oldkeysInInvalids = new HashSet<>();

		private void prep(SQLiteDatabase db) {
			// Since the restore code reads in things sorted by rowid, we can build an index by order index that we can reference later in parsed()
			Cursor c = db.rawQuery("SELECT idkey FROM things ORDER BY rowid", null);
			while (c.moveToNext()) idkeysByRowid.add(c.getString(0));
			c.close();
		}

		/**
		 * @param index This index should correspond to the index in {@link #idkeysByRowid} for finding its old idkey
		 */
		public void parsed(int index, Thing thing) {
			oldkeysToNew.put(idkeysByRowid.get(index), thing.idkey());
		}

		@Override
		public void restored(Multimap<Holder, Object> holders) {
			for (Object held : holders.values()) if (held instanceof String) oldkeysInHolders.add((String) held);
		}

		@Override
		public void restored(Collection<String> idkeys) {
			oldkeysInInvalids.addAll(idkeys);
		}

		// Don't need these callbacks
		@Override public void restored(Thing value) {}
		@Override public void restored(Map<Action, RemotePriority> actions) {}

		public void commit() {
			transaction(() -> {}, e -> {throw new RuntimeException(e);}, db -> {
				SQLiteStatement sql = db.compileStatement("UPDATE OR REPLACE things SET idkey = ? WHERE idkey = ?"); // The REPLACE is needed for a special case that occurs for those that had a pre 7.25 version, upgraded to 7.25 (LocalItems was duplicated here due to idkey issues). For more: https://pocket.slack.com/archives/CD4QWGCD6/p1588838524003000
				for (Map.Entry<String, String> e : oldkeysToNew.entrySet()) {
					String old = e.getKey();
					String newkey = e.getValue();
					sql.bindString(1, newkey);
					sql.bindString(2, old);
					sql.executeUpdateDelete();
				}
				sql.close();
				sql = db.compileStatement("UPDATE invalids SET idkey = ? WHERE idkey = ?");
				for (String old : oldkeysInInvalids) {
					String newkey = oldkeysToNew.get(old);
					if (newkey == null) continue; // Not around anymore, so just ignore it. Let another process clean it up.
					sql.bindString(1, newkey);
					sql.bindString(2, old);
					sql.executeUpdateDelete();
				}
				sql.close();
				sql = db.compileStatement("UPDATE holders SET data = ? WHERE data = ?");
				for (String old : oldkeysInHolders) {
					String newkey = oldkeysToNew.get(old);
					if (newkey == null) continue; // Not around anymore, so just ignore it. Let another process clean it up.
					sql.bindString(1, newkey);
					sql.bindString(2, old);
					sql.executeUpdateDelete();
				}
				sql.close();
			});
		}

	}
	
}
