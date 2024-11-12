package com.pocket.sdk.util.data;

import android.os.Handler;
import android.os.HandlerThread;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Attempts to calculate a diff but gives up if it takes too long.
 * <p>
 * Why? Well {@link DiffUtil} in many cases is fast enough to run on the UI thread, but it can in other cases take several milliseconds or for very complex cases,
 * could take long enough to block or freeze the app.  Trying to do async diffing is very complex because you need to keep track of multiple states and
 * only set/apply the new state after diffing, and there is a lot of complexity to handle to get that right.  However, why bother, because if a diff
 * takes that long to calculate you might as well just call {@link RecyclerView.Adapter#notifyDataSetChanged()}. The whole point of diff'ing is to reduce
 * redraws and slow downs, but if diffing takes too long it defeats the purpose.  It also would delay state changes getting to the screen and make the app
 * feel less responsive.
 * <p>
 * To use, see {@link #calculate(List, List)}.
 * <p>
 * Important: Invoke {@link #reset()} when no longer needed so it can tear down its thread.
 * Only use this classes' methods from the ui thread.
 */
public class FastOrDieDiffer<T> {
	
	private final static long TIMEOUT_MILLIS = 2000;
	private final boolean calculateMoves;
	private final IdDiff idDiff;
	private final ContentDiff contentDiff;
	
	private HandlerThread thread;
	private Handler handler;
	
	public FastOrDieDiffer(IdDiff idDiff, ContentDiff contentDiff, boolean calculateMoves) {
		this.idDiff = idDiff;
		this.contentDiff = contentDiff;
		this.calculateMoves = calculateMoves;
	}
	
	/**
	 * Calculates a diff using {@link DiffUtil} but gives up if it takes too long.
	 * (Note: These lists should not change off the ui thread
	 * @param before The list before the change.
	 * @param after The list after the change
	 * @return The diff or null if timed out.
	 */
	public DiffUtil.DiffResult calculate(List<T> before, List<T> after) {
		if (handler == null) {
			thread = new HandlerThread("differ");
			thread.start();
			handler = new Handler(thread.getLooper());
		}
		AtomicInteger status = new AtomicInteger(0); // 0 = running, 1 = completed, -1 = canceled
		handler.postDelayed(() -> status.compareAndSet(0, -1), TIMEOUT_MILLIS);
		try {
			DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
				@Override
				public int getOldListSize() {
					return before.size();
				}
				
				@Override
				public int getNewListSize() {
					return after.size();
				}
				
				@Override
				public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
					if (status.get() == -1 || handler == null) throw new DiffCancelException();
					return idDiff.areItemsTheSame(before.get(oldItemPosition), after.get(newItemPosition));
				}
				
				@Override
				public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
					if (status.get() == -1 || handler == null) throw new DiffCancelException();
					return contentDiff.areContentsTheSame(before.get(oldItemPosition), after.get(newItemPosition));
				}
				
			}, calculateMoves);
			status.compareAndSet(0, 1);
			return result;
		} catch (DiffCancelException e) {
			return null;
		}
	}
	
	public interface IdDiff<T> {
		boolean areItemsTheSame(T oldItem, T newItem);
	}
	
	public interface ContentDiff<T> {
		boolean areContentsTheSame(T oldItem, T newItem);
	}
	
	/**
	 * Cancels any current operations and shuts down its internal thread. Next time {@link #calculate(List, List)} is called it will startup the thread again if needed.
	 */
	public void reset() {
		if (thread != null) {
			thread.quit();
			thread = null;
			handler = null;
		}
	}
	
}
