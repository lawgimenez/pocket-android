package com.pocket.ui.util;

import android.graphics.Bitmap;

/**
 * An asynchronously loading {@link Bitmap}.
 * Mostly for use in {@link LazyBitmapDrawable}.
 */
public interface LazyBitmap {
	void fill(int widthPx, int heightPx, Loaded loaded, Canceller canceller);
//	void fillRatio(int portWidth, int portHeight, int squareWidth, int squareHeight, int landWidth, int landHeight, Loaded loaded, Canceller canceller);
	interface Loaded {
		void onBitmapLoaded(Bitmap bitmap);
	}
	
	class Canceller {
		private boolean cancelled;
		public boolean isCancelled() {
			return cancelled;
		}
		public void cancel() {
			cancelled = true;
		}
		
		public static Canceller cancelAndRenew(Canceller canceller) {
			if (canceller != null) {
				canceller.cancel();
			}
			return new Canceller();
		}
	}
}
