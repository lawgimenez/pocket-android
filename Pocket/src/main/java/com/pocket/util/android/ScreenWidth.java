package com.pocket.util.android;

import android.app.Activity;
import android.graphics.Point;
import android.view.Display;

import com.pocket.app.App;

/**
 * <b>Deprecated</b> Now that Android supports multiple resizable windows, we should avoid using
 * cached values or hard screen sizes. Instead, the window size should be queried as needed
 * so it can be more dynamic. See {@link FormFactor#getWindowWidthDp(Activity)} for a helper method.
 * <p>
 * A helper class for figuring out what the app's width will be in both orientations.  This measurement is used by the Queue html5 code.
 *
 *
 */
@Deprecated
public class ScreenWidth {

	public static ScreenWidth mScreenSize;
	
	public static ScreenWidth get(Activity activity) {
		if (mScreenSize == null)
			mScreenSize = ScreenWidth.newScreenSize(activity);
		
		return mScreenSize;
	}
	
	private static ScreenWidth newScreenSize(Activity activity) {
		return new ScreenWidth(activity);
	}
	
	protected final int mShortestWidth;
	protected final int mLongestWidth;
	
	protected ScreenWidth(Activity activity) {
		int cachedShort = App.getApp().prefs().SCREEN_WIDTH_SHORT.get();
		int cachedLong = App.getApp().prefs().SCREEN_WIDTH_LONG.get();
		Point size = getWidths(activity, activity.getWindowManager().getDefaultDisplay(), new Point(cachedShort, cachedLong));
		size = figureOutLongShort(size);
		mShortestWidth = size.x;
		mLongestWidth = size.y;
		
		if (cachedShort != mShortestWidth || cachedLong != mLongestWidth) {
			// Cache new value
			App.getApp().prefs().SCREEN_WIDTH_SHORT.set(size.x);
			App.getApp().prefs().SCREEN_WIDTH_LONG.set(size.y);
		}
			
	}
	
	/**
	 * Calculates the available widths in both orientations of the device. By available, this means available to the app's UI, so
	 * it is exclusive of system UI.
	 * 
	 * @param activity
	 * @param display
	 * @param point A Point containing the cached widths, or if no cache was available then 0 for both x and y.
	 * @return
	 */
	@SuppressWarnings("deprecation")
	protected Point getWidths(Activity activity, Display display, Point point) {
		// Ignore the cached value passed-in
		Point smallSize = new Point();
		Point largeSize = new Point();
		display.getCurrentSizeRange(smallSize, largeSize);
		// Return the smallest and largest widths. getCurrentSizeRange sets the width in the x and height in the y of the points.
		return new Point(smallSize.x, largeSize.x);
	}
	
	/**
	 * Takes a Point set by Display.getSize() and sets Point.x to the smaller of the dimensions, and Point.y to the larger of the two.
	 * 
	 * @param displaySize
	 * @return
	 */
	private Point figureOutLongShort(Point displaySize) {
		if (displaySize.x > displaySize.y) {
			return new Point(displaySize.y, displaySize.x);
			
		} else {
			return new Point(displaySize.x, displaySize.y);
			
		}
	}
	
	/**
	 * Get the shorter of the two device dimensions.
	 * 
	 * @param dp true will return the dimension as a dp, false as px
	 * @return
	 */
	public int getShortest(boolean dp) {
		if (!dp)
			return mShortestWidth;
		
		return (int) FormFactor.pxToDp(mShortestWidth);
	}
	
	/**
	 * Get the longer of the two device dimensions.
	 * 
	 * @param dp true will return the dimension as a dp, false as px
	 * @return
	 */
	public int getLongest(boolean dp) {
		if (!dp)
			return mLongestWidth;
		
		return (int) FormFactor.pxToDp(mLongestWidth);
	}

	public static void clearCache() {
		App.getApp().prefs().SCREEN_WIDTH_SHORT.set(0);
		App.getApp().prefs().SCREEN_WIDTH_LONG.set(0);
	}
	
}
