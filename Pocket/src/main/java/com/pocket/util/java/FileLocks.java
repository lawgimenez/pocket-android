package com.pocket.util.java;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * On Android, {@link java.nio.channels.FileLock}, is only advisory and doesn't actually
 * do anything to prevent thread safety issues. This class serves as a replacement for that
 * functionality. To use, there should be a shared single instance of this class and any
 * reads or writes should invoke one of the {@link #lock(File)} methods prior to doing
 * anything with the file. When complete, invoke {@link Lock#release()}. Be sure to release
 * in a finally block to ensure it releases even with exceptions.
 *
 * TODO make read and write separate lock types so multiple reads can happen at the same time.
 */
public class FileLocks {

    private final Map<String, ArrayList<Lock>> mLocks = new HashMap<>();
    private final ArrayList<ArrayList<Lock>> mRecycled = new ArrayList<>();

    public FileLocks() {

    }

    /**
     * Obtains a lock on a file. If there is already a lock on this file, it blocks until
     * that lock is released and it is granted a lock. Be sure to release the lock
     * when complete with {@link Lock#release()}.
     *
     * @param path
     * @return The lock
     * @throws InterruptedException
     */
    public Lock lock(String path) throws InterruptedException {
        Lock lock = new Lock(path);
        Lock held;

        synchronized (mLocks) {
            // Find or create a queue for this file.
            ArrayList<Lock> queue = mLocks.get(path);
            if (queue == null) {
                // Create or reuse a queue
                if (mRecycled.isEmpty()) {
                    queue = new ArrayList<>();
                } else {
                    queue = mRecycled.remove(0);
                    queue.clear();
                }
                mLocks.put(path, queue);
            }

            // If there is anyone ahead of us in the queue, we will wait until they release.
            held = !queue.isEmpty() ? queue.get(queue.size()-1) : null;

            // Add this request to the end of the queue.
            queue.add(lock);
        }

        // Wait for anyone ahead of us.
        if (held != null) {
            held.await();
        }

        // At this point, we should be the currently held lock for the file.
        return lock;
    }

    /**
     * @see #lock(String)
     */
    public Lock lock(File file) throws InterruptedException {
        return lock(file.getAbsolutePath());
    }

    /**
     * Invokes release or does nothing if the lock is null.
     */
    public static void releaseQuietly(FileLocks.Lock lock) {
        if (lock != null) {
            lock.release();
        }
    }

    public class Lock {

        private final CountDownLatch mLatch = new CountDownLatch(1);
        public final String file;

        private Lock(String file) {
            this.file = file;
        }

        private void await() throws InterruptedException {
            mLatch.await();
        }

        /**
         * Release control of the file to let others access it.
         */
        public void release() {
            synchronized (mLocks) {
                if (mLatch.getCount() <= 0) {
                    return; // Already released
                }

                // Remove the lock from the queue, recycle the queue if now empty.
                ArrayList<Lock> queue = mLocks.get(file);
                if (queue != null) {
                    queue.remove(this);
                    if (queue.isEmpty()) {
                        mRecycled.add(mLocks.remove(file));
                    }
                }

                mLatch.countDown();
            }
        }
    }

}
