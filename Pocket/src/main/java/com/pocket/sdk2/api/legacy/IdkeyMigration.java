package com.pocket.sdk2.api.legacy;

import com.pocket.sdk.dev.ErrorHandler;
import com.pocket.sdk.offline.cache.Assets;
import com.pocket.sync.space.persist.DumbStorage;
import com.pocket.sync.space.persist.MigrationStorage;
import com.pocket.sync.space.persist.SqliteBinaryStorage;
import com.pocket.sync.spec.Spec;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.binary.ByteWriter;

import org.apache.commons.io.filefilter.DirectoryFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.Set;

/**
 * Late April 2020, between version 7.25 and 7.27 we realized there was a bug in the {@link Thing#idkey()} logic that caused idkeys to change between versions if a field was added to that thing in figment.
 * More context can be found here https://github.com/Pocket/Android/pull/1523.
 * <p>
 * Ultimately we realized that {@link Thing#compress(ByteWriter)} was inherently not suitable for idkeys, we had to switch to a different idkey algorithm.
 * This means that we had to migrate any persisted idkeys to the new ones. Thankfully {@link SqliteBinaryStorage} stored the idkeys alongside the thing data,
 * so we are able to read the old idkeys from that table, generate the new idkeys and using that map, propagate that info to all the parts of the app that had idkeys
 * persisted and swap them from old to new. That's what this migration will do.
 * <p>
 * This can be removed when we eventually stop supporting upgrading from versions before 7.27. See {@link com.pocket.app.PocketSingleton} for more on when this is included.
 */
public class IdkeyMigration implements MigrationStorage.Access {

    private final Assets assets;
    private final ErrorHandler errorReporter;
    private boolean reportedError;

    public IdkeyMigration(Assets assets, ErrorHandler errorReporter) {
        this.assets = assets;
        this.errorReporter = errorReporter;
    }

    @Override
    public void transform(Spec spec, DumbStorage storage) {
        try {
            if (!(storage instanceof SqliteBinaryStorage)) throw new RuntimeException("Unexpected storage type, are you sure this migration is needed?");

            // Extract things and their old keys
            SqliteBinaryStorage.IdkeyMigrator migrator = ((SqliteBinaryStorage) storage).migrateIdkeys(spec);

            // Fix markup directory names
            File markups = assets.getAssetDirectory().getMarkupDirectory();
            Set<String> markupOldKeys = new HashSet<>();
            File[] markupDirs = markups.listFiles((FileFilter) DirectoryFileFilter.INSTANCE); // This should be a list of markup folders that are named with the old idkeys
            if (markupDirs != null && markupDirs.length > 0) { // Can be null if the directory hasn't been created yet (no offline content yet)
                for (File old : markupDirs) {
                    String oldkey = old.getName();
                    String newkey = migrator.oldkeysToNew.get(oldkey);
                    if (newkey == null) continue; // Might have already been renamed in a previous attempt if the last one failed. Or maybe it is a directory that needs to just be cleaned up.
                    markupOldKeys.add(oldkey);
                    old.renameTo(new File(markups, newkey)); // Ignoring failures...
                }
            }

            // Fix the asset database
            assets.fixIdKeys(migrator.oldkeysToNew, markupOldKeys);

            // Finally, if all successful, update storage (after this, we won't have the old idkeys anymore, no going back)
            migrator.commit();

        } catch (Exception e) {
            if (!reportedError) {
                reportedError = true;
                errorReporter.reportError(e);
            }
            throw new RuntimeException(e);
        }
    }
}