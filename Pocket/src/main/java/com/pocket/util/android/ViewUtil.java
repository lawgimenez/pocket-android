package com.pocket.util.android;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.ideashower.readitlater.BuildConfig;
import com.pocket.util.java.Logs;
import com.pocket.util.java.Range;
import com.pocket.util.java.function.Function;

import org.apache.commons.lang3.ArrayUtils;
import org.joor.Reflect;

import java.util.ArrayList;
import java.util.List;

public class ViewUtil {

    /** A shared instance rect to be used by methods in this class, but only on the uithread to ensure things to break */
    private static final Rect mUiThreadRect = new Rect();

	/**
	 * Helper for ensuring absolutely that the soft keyboard opens/closes when focusing/unfocusing.
	 * @param focus whether
	 * @param view
	 */
	public static boolean forceFocus(boolean focus, View view) {
		if (focus) {
			view.requestFocus();
		} else {
			view.clearFocus();
		}
		
		return forceSoftKeyboard(focus, view);
	}
	
	/**
	 * Force a soft keyboard open or closed for a view.
	 *
	 * @param open true if force open, false if force close
	 * @param view the view focused or being unfocused
	 */
	public static boolean forceSoftKeyboard(boolean open, View view) {
		InputMethodManager mgr = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		if (open) {
			return mgr.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
		} else {
			return mgr.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}
	
	public static void fadeView(final View view, final boolean visible, long duration) {
		float fromAlpha = visible ? 0 : 1;
		float toAlpha = visible ? 1 : 0;
		AlphaAnimation ani = new AlphaAnimation(fromAlpha, toAlpha);
		ani.setDuration(duration);
		//ani.setFillEnabled(true);
		//ani.setFillAfter(true);
		ani.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				if (!visible)
					view.setVisibility(View.GONE);
			}
		});
		
		view.setVisibility(View.VISIBLE);
		view.startAnimation(ani);
	}

	public static boolean isVisible(View view) {
		return view != null && view.getVisibility() == View.VISIBLE;
	}

	/**
	 * Set the bottom padding of a view in px.
	 * @param view
	 * @param padding
	 */
	public static void setPaddingBottom(View view, int padding) {
		view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
				view.getPaddingRight(), padding);
	}
	
	/**
	 * Set the left padding of a view in px.
	 * @param view
	 * @param padding
	 */
	public static void setPaddingLeft(View view, int padding) {
		view.setPadding(padding, view.getPaddingTop(),
				view.getPaddingRight(), view.getPaddingBottom());
	}
	
	/**
	 * Set the right padding of a view in px.
	 * @param view
	 * @param padding
	 */
	public static void setPaddingRight(View view, int padding) {
		view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
				padding, view.getPaddingBottom());
	}
	
	/**
	 * Set the left and right padding of a view in px.
	 */
	public static void setPaddingHorizontal(View view, int leftAndRight) {
		view.setPadding(
				leftAndRight, view.getPaddingTop(),
				leftAndRight, view.getPaddingBottom());
	}

	/**
	 * Convenience for showing {@link View#VISIBLE} and hiding {@link View#GONE} a view.
	 * <p>
	 * Safe to pass a null view. Nothing will happen in that case.
	 * @param view
	 * @param visible
	 */
	public static void setVisible(View view, boolean visible) {
		if (view == null) {
			return;
		}
		setVisible(visible, view);
	}
	
	public static void setVisible(boolean visible, View... views) {
		setVisibility(visible ? View.VISIBLE : View.GONE, views);
	}
	
	public static void setVisibility(int visibility, View... views) {
		for (View view : views) {
			if (view != null) {
				view.setVisibility(visibility);
			}
		}
	}

	/**
	 * Searches a {@link ViewGroup} for a visible child at the provided screen coordinates.
	 * If the parent is scrollable, adjust the x and y to be within the scrollable area if needed.
	 * Otherwise this is just made with non scrollable views in mind.
	 *
	 * @param parent The parent to search within, will only return direct children of this view.
	 * @param downX The x position on screen.
	 * @param downY The y position on screen.
	 * @return The child {@link View} or null if parent was not a {@link ViewGroup} or if no child was found.
	 */
	public static View getChildViewForCoord(View parent, float downX, float downY) {
		if (!(parent instanceof ViewGroup)) {
			return null;
		}

		ViewGroup viewGroup = (ViewGroup) parent;
		Rect rect = new Rect();
		int[] parentLocation = new int[2];

		int size = viewGroup.getChildCount();
		parent.getLocationOnScreen(parentLocation);

		int x = (int) downX - parentLocation[0];
		int y = (int) downY - parentLocation[1];

		for (int i = size - 1; i >= 0; i--) { // Reverse order so it finds the top of the z index first
			View child = viewGroup.getChildAt(i);
			if (child.getVisibility() != View.VISIBLE) {
				continue;
			}
			child.getHitRect(rect);
			if (rect.contains(x, y)) {
				return child;
			}
		}
		return null;
	}

	/**
	 * Searches a View to see if it or a sub View is a specific View.
	 * Recursively traverses children.
	 * 
	 * @param parent
	 *            The view to search in.
	 * @param view
	 *            The view to search for.
	 * @return True if the view is a child of (or is) the parent.
	 */
	public static boolean containsView(View parent, View view) {
		if (view == parent) {
			return true;
		}

		if (parent instanceof ViewGroup) {
			ViewGroup viewGroup = (ViewGroup) parent;
			int children = viewGroup.getChildCount();
			// Search children
			for (int i = 0; i < children; i++) {
				if (containsView(viewGroup.getChildAt(i), view)) {
					return true;
				}
			}
		}

		return false;
	}
	
	public static void setCancelOnOutsideTouch(PopupWindow popup) {
		popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		popup.setOutsideTouchable(true);
		popup.setFocusable(true);
	}

	public static void refreshDrawableStateDeep(View view) {
		view.refreshDrawableState();
		view.invalidate();
		if (view instanceof ViewGroup) {
			ViewGroup parent = (ViewGroup) view;
			int children = parent.getChildCount();
			// Refresh children
			for (int i = 0; i < children; i++) {
				refreshDrawableStateDeep(parent.getChildAt(i));
			}
		}
	}
	
	/**
	 * Set a {@link ProgressBar}'s progress with a percent 0-1.
	 * 
	 * @param bar
	 * @param percent
	 */
	public static void setProgress(ProgressBar bar, float percent) {
		bar.setProgress((int) (bar.getMax() * percent));
	}
	
	public static void setLayoutWidth(View view, int width) {
		ViewGroup.LayoutParams lp = view.getLayoutParams();
		if (lp == null) {
			lp = new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
		}
		lp.width = width;
		view.setLayoutParams(lp);
	}
	
	public static void setLayoutHeight(View view, int height) {
		ViewGroup.LayoutParams lp = view.getLayoutParams();
		if (lp == null) {
			lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, height);
		}
		lp.height = height;
		view.setLayoutParams(lp);
	}

	/**
	 * Changes touch events so they fall within another view instead. Changes if needed so it is within the bounds
	 * of the view, trying to get as close to the original point as possible. Dispatches the event to the child.
	 * <p>
	 * As an example imagine a parent who invokes this on a child view. The child view has a left, top, right and bottom of
	 * 10,10,20,20 with its parent. The touch event has a x of 5 and a y of 15.
	 * <p>
	 * The x falls out of the bounds of the child view (x is to the left).
	 * <p>
	 * This method will change the touch event so that x=10,y=15.
	 *  
	 * @param view
	 */
	public static void moveTouchEventIntoBounds(MotionEvent event, View view) {
		float x = event.getX();
		float y = event.getY();
		
		x = Range.limit(view.getLeft(), view.getRight(), x);
		y = Range.limit(view.getTop(), view.getBottom(), y);
		
		event.setLocation(x, y);
		
		view.dispatchTouchEvent(event);
	}
	
	/**
	 * Gets a parent up the chain. steps = 1 gets the direct parent. steps = 2 gets the parent of the parent and so on.
	 * <p>
	 * Handles nulls safely so if there are only 2 parents and you ask for 5, it will just return null.
	 * 
	 * @param child null safe, will just return null
	 * @param steps
	 * @return
	 */
	public static View getParent(View child, int steps) {
		while (child != null && steps > 0) {
			if (child.getParent() instanceof View) {
				child = (View) child.getParent();
			} else {
				child = null;
			}
			steps--;
		}
		
		return child;
	}

    /**
     * Removes a view from its parent and returns true. If it does not have a ViewGroup parent, it will
     * do nothing and return false.
	 * @return true if removed
     */
    public static boolean remove(View view) {
        return remove(view, true);
    }

	/**
	 * For Views in ViewGroups using the android:animateLayoutChanges parameter, this provides the option to
	 * remove the View from its parent without triggering layout transition animations.
	 */
	public static boolean remove(View view, boolean animate) {
		if (view != null && view.getParent() instanceof  ViewGroup) {
			ViewGroup parent = ((ViewGroup) view.getParent());
			LayoutTransition transition = null;
			if (!animate) {
				transition = parent.getLayoutTransition();
				parent.setLayoutTransition(null);
			}
			parent.removeView(view);
			if (transition != null) {
				parent.setLayoutTransition(transition);
			}
			return true;
		} else {
			return false;
		}
	}

	public static float getPercentVisible(View view) {
		// First check basic visibility flags, starting from inexpensive to more expensive checks.
		if (view == null
			|| view.getVisibility() != View.VISIBLE
			|| view.getWidth() <= 0 || view.getHeight() <= 0
			|| view.getWindowVisibility() != View.VISIBLE
			|| !view.isShown()
			|| !view.isAttachedToWindow()) {
				return 0;
		}

		if (!view.getGlobalVisibleRect(mUiThreadRect)) {
			// Careful, this always returns true if the view isn't attached to a window. 'false' is the only state you can safely trust. True still needs to be double checked!
			return 0;
		} else {
			float visibleArea = mUiThreadRect.width() * mUiThreadRect.height();
			float totalArea = view.getWidth() * view.getHeight();
			return visibleArea / totalArea;
		}
	}

    /**
     * Is this view visible to the user?
     *
     * @param view The view to check, if null it will return false.
     * @param minVisiblePercent [0-1] The minimum percentage of the view's area that must be seen in order to consider it visible.
     *                          0 means even a single pixel will be considered visible. 1 means every pixel must be visible.
     * @return
     */
    public static boolean isVisibleToUserCompat(View view, float minVisiblePercent) {
        boolean debug = BuildConfig.DEBUG && false;

		if (view == null
		|| !view.isShown()
        || view.getWindowVisibility() != View.VISIBLE
        || view.getVisibility() != View.VISIBLE
        || view.getWidth() <= 0
        || view.getHeight() <= 0
        || !isAttachedToViewRoot(view)) {
			if (debug) {
				String reason;
				if (view == null) {
					reason = "null";
				} else if (!view.isShown()) {
					reason = "not shown";
				} else if (view.getWindowVisibility() != View.VISIBLE) {
					reason = "window vis";
				} else if (view.getVisibility() != View.VISIBLE) {
					reason = "view vis";
				} else if (view.getWidth() <= 0 || view.getHeight() <= 0) {
					reason = "view size";
				} else if (!isAttachedToViewRoot(view)) {
					reason = "view root";
				} else {
					reason = "unknown";
				}
				WIP.l("VISCHECK ~ HIDDEN ~ " + reason);
			}
            return false;

        } else {
            boolean visible = view.getGlobalVisibleRect(mUiThreadRect); // Note this always returns true when the view is not attached to a window, hence the isAttachedToViewRoot() check above.
            if (!visible) {
				if (debug) WIP.l("VISCHECK ~ HIDDEN ~ not visible in global rect");
                return false;
            } else if (minVisiblePercent <= 0) {
				if (debug) WIP.l("VISCHECK ~ VISIBLE ~ any percent allowed");
                return true;
            } else {
                float visibleArea = mUiThreadRect.width() * mUiThreadRect.height();
                float totalArea = view.getWidth() * view.getHeight();
				float percent = visibleArea / totalArea;
				if (percent >= minVisiblePercent) {
					if (debug) WIP.l("VISCHECK ~ VISIBLE ~ " + percent);
					return true;
				} else {
					if (debug) WIP.l("VISCHECK ~ HIDDEN ~ " + percent);
					return false;
				}
            }
        }
    }

	/**
     * @return The last parent returned by continually invoking {@link android.view.View#getParent()} before it returned null. This can return null if
     *          the view itself has no parent.
     */
    public static ViewParent getRootParent(View view) {
        ViewParent last = null;
        ViewParent current = view.getParent();
        while (current != null) {
            last = current;
            current = current.getParent();
        }
        return last;
    }
	
	/**
	 * {@link View#getRootView()} returns the DecorView but the DecorView is typically the full window size
	 * and includes all of the screen decor like on screen nav, status bars, etc. If you need access instead
	 * to the root view that is actually inset and the size of the visible area of your view/activity,
	 * this method will try to find the content view. If it can't it will default to the decor view.
	 */
	public static ViewGroup getContentRoot(View of) {
		View decor = of.getRootView();
		View content = decor.findViewById(android.R.id.content);
		return (ViewGroup) (content != null ? content : decor);
	}

    /**
     * Is this view added to a view in the Activity?
     * // REVIEW some how make this work for additional windows. This is used because View.getGlobalVisibleRect() always returns true for views that
     * are detached for some reason. This method helps detect that case to avoid trusting its return value.
     *
     * @param view
     * @return true if this view is in the activity, false if it or its parents are detached from the activity views.
     */
    public static boolean isAttachedToViewRoot(View view) {
        if (view.getRootView() == null) {
            return false;
        }
        ViewParent rootParent = getRootParent(view);
        if (rootParent == null) {
            return false;
        }
        Activity activity = ContextUtil.getActivity(view);
        if (activity == null) {
            return false;
        }
        Window window = activity.getWindow();
        if (window == null) {
            return false;
        }
        View decor = window.getDecorView();
        if (decor == null) {
            return false;
        }
        if (decor == rootParent) {
            return true;
        } else if (decor.getParent() == rootParent) {
            return true;
        } else {
            return false;
        }
    }

	public static int makeMeasureSpec(int size, int max) {
		if (size >= 0) {
			return View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY);
		} else if (max > 0) {
			return View.MeasureSpec.makeMeasureSpec(max, View.MeasureSpec.AT_MOST);
		} else {
			return View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
		}
	}

	public static void disableFocusAndClicks(View view) {
		if (view == null) {
			return;
		}

		view.setClickable(false);
		view.setFocusable(false);
		view.setFocusableInTouchMode(false);
	}

	public static List<View> getAllViews(View view, List<View> output) {
		if (output == null) {
			output = new ArrayList<>();
		}
		if (view != null) {
			output.add(view);
			if (view instanceof ViewGroup) {
				ViewGroup group = (ViewGroup) view;
				int children = group.getChildCount();
				for (int i = 0; i < children; i++) {
					getAllViews(group.getChildAt(i), output);
				}
			}
		}
		return output;
	}

	@SuppressWarnings("unused")
	public static void logViewHierarchy(View view) {
		List<View> hierarchy = getAllViews(view, null);
		for (View v : hierarchy) {
			Log.v("View Hierarchy", "W:" + v.getMeasuredWidth() + " H:" + v.getMeasuredHeight() + " L:" + v.getLeft() + " T:" + v.getTop() + " " + v);
		}
	}
	
	/**
	 * Log motion events in a view hierarchy to help determine where it is going and what view is consuming it.
	 * Do not use outside of debugging.
	 * @param view
	 */
	@SuppressWarnings("unused")
	public static void debugTouch(View view) {
		View.OnTouchListener listener = Reflect.on(view).call("getListenerInfo").field("mOnTouchListener").get();
		view.setOnTouchListener((v, event) -> {
			Logs.v("TouchDebug", event.getAction() + " " + view);
			if (listener != null) {
				return listener.onTouch(v, event);
			} else {
				return false;
			}
		});
		if (view instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) view;
			for (int i = 0; i < group.getChildCount(); i++) {
				debugTouch(group.getChildAt(i));
			}
		}
	}
}
