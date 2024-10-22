package com.pocket.util.java;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A synchronization aid that allows one or more threads to wait until a set of operations being performed in other threads completes.
 * Similar to {@link CountDownLatch}.
 * <p>
 * Operations can create a hold with {@link #hold(Object)} and then when their work completes, release it with {@link #release(Object)}.
 * <p>
 * A thread can wait until all holds are released by using one of the await methods such as {@link #await(long, TimeUnit)}.
 * <p>
 * The latch will open/release the first time the holds reach zero after await has been called at least once.
 * This means if await is invoked before any holds are added, it will release immediately.
 * <p>
 * Once the latch is released, it will stay open. Make a new instance if you need a new latch.
 */
public class KeyLatch {

	private final Set<Object> holds = new HashSet<>();
	private final CountDownLatch latch = new CountDownLatch(1);
	private final boolean strict;
	private State state = State.PENDING;
	private long lastChange;
	
	enum State {
		/** No await method has been called yet. */
		PENDING,
		/** Await has been called but there are holds it is waiting for. */
		CLOSED,
		/** The latch has been released */
		OPEN
	}
	
	public KeyLatch() {
		this(false);
	}
	
	/**
	 * @param strict if true, this will throw exceptions if you invoke {@link #hold(Object)} or {@link #release(Object)} after the latch has been opened or if release is invoked for a key that doesn't have a hold.
	 *               if false, those events will just be ignored.
	 */
	public KeyLatch(boolean strict) {
		this.strict = strict;
	}
	
	/**
	 * Add an additional hold to keep the latch waiting.
	 * @param key A unique key for this hold, based on its hashcode/equals implementation.
	 */
	public synchronized void hold(Object key) {
		if (strict && latch.getCount() == 0) strictError("Latch has already been released. Attempted hold: " + key);
		boolean added = holds.add(key);
		lastChange = System.currentTimeMillis();
		if (strict && !added) strictError("Duplicate hold: " + key);
		invalidate();
	}
	
	/**
	 * Release a hold. If there are no more holds, the latch will open.
	 * @param key The key/object provided to {@link #hold(Object)}
	 */
	public synchronized void release(Object key) {
		if (strict && latch.getCount() == 0) strictError("Latch has already been released. Attempted release: " + key);
		boolean removed = holds.remove(key);
		lastChange = System.currentTimeMillis();
		if (strict && !removed) strictError("Hold was not active: " + key);
		invalidate();
	}
	
	private void strictError(String log) {
		throw new RuntimeException(log);
	}
	
	private synchronized void activate() {
		if (state == State.PENDING) state = State.CLOSED;
	}
	
	private synchronized void invalidate() {
		if (state == State.CLOSED) {
			if (holds.isEmpty()) {
				state = State.OPEN;
				latch.countDown();
			}
		} else {
			// If already open or pending, nothing to update yet
		}
	}
	
	/**
	 * @return A copied set (modifications here have no effect) of the active holds.
	 */
	public synchronized Set<Object> holds() {
		return new HashSet<>(holds);
	}
	
	/**
	 * @return if the latch is open/released.  This has the same effect as calling an await in that it will switch the latch from pending to with closed or open. It won't block though, just return the current state.
	 */
	public synchronized boolean isOpen() {
		activate();
		invalidate();
		return state == State.OPEN;
	}
	
	/**
	 * Same as {@link CountDownLatch#await(long, TimeUnit)}
	 * If no holds have been created, this will release the latch immediately.
	 */
	public boolean await(long time, TimeUnit unit) throws InterruptedException {
		activate();
		invalidate(); // Count down the launch in the case that no holds were ever created.
		return latch.await(time, unit);
	}
	
	/**
	 * Awaits until the latch is released, invoking the 'checkIn' at an interval of 'frequencyMillis' to see if it should continue to await.
	 * If no holds have been created, this will release the latch immediately.
	 * @return true if the latch was completed and released, false if this returned because {@link CheckIn#checkin(long, Set, long)} returned false.
	 */
	public boolean await(long frequencyMillis, CheckIn checkIn) throws InterruptedException {
		activate();
		invalidate(); // Count down the launch in the case that no holds were ever created.
		long started = System.currentTimeMillis();
		do {
			boolean released = latch.await(frequencyMillis, TimeUnit.MILLISECONDS);
			if (released) return true;
			long now = System.currentTimeMillis();
			if (!checkIn.checkin(now-started, holds(), now-lastChange)) return false;
		} while (true);
		
	}
	
	public interface CheckIn {
		/**
		 * The requested frequency since start or the last check in has elapsed during {@link #await(long, CheckIn)}.
		 * @param elapsedSinceStart The total time in millis since await began
		 * @param held A copy of the current holds
		 * @param elapseSinceChange The total time in millis since the last change to holds (add or release) has occurred
		 * @return true to continue waiting, false to stop.
		 */
		boolean checkin(long elapsedSinceStart, Set<Object> held, long elapseSinceChange);
	}
	
}
