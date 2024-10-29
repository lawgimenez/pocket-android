package com.pocket.sdk.util.view.tooltip;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.ideashower.readitlater.BuildConfig;
import com.pocket.app.App;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sdk.util.view.tooltip.theme.SimpleTheme;
import com.pocket.sdk.util.view.tooltip.theme.TooltipTheme;
import com.pocket.sdk.util.view.tooltip.view.TooltipView;
import com.pocket.sdk.util.view.tooltip.view.TooltipViewsHolder;
import com.pocket.util.android.ViewBoundsWatcher;
import com.pocket.util.android.ViewUtil;

import java.util.ArrayList;

import androidx.core.view.ViewCompat;

/**
 * A view that points out some aspect of the ui. To use, start with a new {@link com.pocket.sdk.util.view.tooltip.Tooltip.Builder}.
 * <p>
 * To change the existing appearance or make a new style of tooltip, check out {@link TooltipView}.
 * <p>
 * Some convenience methods for showing with default styles are available in {@link DefaultTheme}.
 */
public class Tooltip {

    // REVIEW dismiss tooltips when activities ends

    public static boolean DEBUG = BuildConfig.DEBUG && false;

    public static class DefaultTheme {

        private static final TooltipTheme mTheme = new SimpleTheme();

        public static TooltipController showButton(View button, int text, TooltipListener listener) {
            return mTheme.showButton(button, text, listener);
        }

        public static TooltipController showButton(View button, ViewGroup displayLocation, int text, TooltipListener listener) {
            return mTheme.showButton(button, displayLocation, text, listener);
        }

        public static TooltipController showAdapterItem(Object item, AdapterView adapterView, int text, TooltipListener listener) {
            return mTheme.showAdapterItem(item, adapterView, text, listener);
        }
    }

    public static class Builder {

        private final ArrayList<TooltipView> mViews = new ArrayList<>();
        private final Context mContext;

        private OutsideTouchAction mOutsideTouchAction = new OutsideTouchAction(OutsideTouchAction.Block.NOWHERE, true);
        private TooltipListener mListener;
        private ViewGroup mDisplayLocation;

        /**
         * Create a new tooltip.
         */
        public Builder(Context context) {
            mContext = context;
        }

        /**
         * Add an additional view to be part of this tooltip. Views are layered from bottom up, last view added is on top of previous ones.
         *
         * @param value
         * @return
         */
        public Builder addView(TooltipView value) {
            mViews.add(value);
            return this;
        }

        /**
         * The action to take if the user touches outside of the Tooltip. Defaults to {link OutsideTouch#DISMISS}.
         *
         * @param value
         * @return
         */
        public Builder setOutsideTouchAction(OutsideTouchAction value) {
            mOutsideTouchAction = value;
            return this;
        }

        public Builder setDisplayLocation(ViewGroup value) {
            mDisplayLocation = value;
            return this;
        }

        /**
         * Listen to tooltip events.
         *
         * @param listener
         * @return
         */
        public Builder setTooltipListener(TooltipListener listener) {
            mListener = listener;
            return this;
        }

        /**
         * Show the tooltip, anchored to a view. If the view resizes or moves, the tooltip will automatically
         * adjust to the new position. It will attempt to show it immediately. If the anchor is not attached
         * to a window or if the Activity hasn't fully resumed yet, it will attempt to show it within a few
         * run loops. If the tooltip doesn't fit or the anchor isn't attached within the next few run loops,
         * it will give up and no tooltip will be shown. Use {@link #setTooltipListener(TooltipListener)}
         * to know what happens.
         *
         * @param anchor The view the tooltip is referencing.
         * @param isAnchorScrollable true if the anchor view is within a scrollable parent. false otherwise.
         * @return
         */
        public TooltipController show(View anchor, boolean isAnchorScrollable) {
            ViewGroup parent;
            if (mDisplayLocation != null) {
                parent = mDisplayLocation;
            } else {
                parent = ViewUtil.getContentRoot(anchor);
            }
            return new TooltipController(this, parent).anchor(anchor, isAnchorScrollable);
        }

        /**
         * Same as {@link #show(View, boolean)} where the tooltip targets a view, but
         * the view is found by searching an adapter view matching an object. If the view
         * representing this item is not in view, this will likely fail to show the tooltip.
         *
         * @param item The item that {@link android.widget.Adapter#getItem(int)} will return
         * @param adapterView The adapter view the item's view is in.
         * @return
         */
        public TooltipController showAdapterItem(Object item, AdapterView adapterView) {
            ViewGroup parent;
            if (mDisplayLocation != null) {
                parent = mDisplayLocation;
            } else {
                parent = (ViewGroup) adapterView.getRootView();
            }
            return new TooltipController(this, parent).anchorAdapterItem(item, adapterView);
        }

    }

    /**
     * Controls showing, updating and dismissing the {@link Tooltip}.
     */
    public static class TooltipController {

        private static int DEFAULT_SHOW_ATTEMPTS = 2;

        private final TooltipListener mListener;
        private final TooltipViewsHolder mViews;

        // The following are reused in performance important methods to avoid memory allocation.
        private final int[] mRecycleXY = new int[2];
        private final Rect mRecycleAnchorBounds = new Rect();

        private final AbsPocketActivity.OnBackPressedListener mBackPressedListener = () -> {
            dismiss();
            return true;
        };
        private ViewBoundsWatcher mAnchorBoundsWatcher;
        private AdapterViewWatcher mAdapterViewWatcher;

        private boolean mIsDismissed;
        private boolean mHasShown;

        /** The number of run loops remaining to retry before giving up on a show. */
        private int mShowAttempts = DEFAULT_SHOW_ATTEMPTS;
        private View mAnchor;
        private AdapterView mAnchorsAdapterView;

        private TooltipController(Builder builder, ViewGroup displayLocation) {
            ViewDisplayer window = new ViewGroupDisplayer(displayLocation);
            mViews = new TooltipViewsHolder(builder.mContext, builder.mViews, window);
            mViews.setOutsideTouchAction(builder.mOutsideTouchAction, this);
            for (TooltipView tooltipView : builder.mViews) {
                tooltipView.bind(this);
            }

            mListener = builder.mListener;
        }

        /**
         * @see {@link com.pocket.sdk.util.view.tooltip.Tooltip.Builder#showAdapterItem(Object, AdapterView)}
         */
        private TooltipController anchorAdapterItem(final Object item, final AdapterView adapterView) {
            if (mIsDismissed) {
                return this;
            }

            clearObservers();

            View view = AdapterViewWatcher.findView(item, adapterView);
            if (view != null) {
                // Ok we can show it, keep watching it in case it changes
                mAnchorsAdapterView = adapterView;
                anchor(view, true);

                // REVIEW this will be cleared when anchor() invokes clearObservers
                mAdapterViewWatcher = new AdapterViewWatcher(item, adapterView, view, new AdapterViewWatcher.Listener() {

                    public void onAdapterItemViewChanged(View view) {
                        anchor(view, true);
                    }

                });

            } else {
                // Couldn't find it yet

                if (mShowAttempts > 0) {
                    // Retry on next loop
                    mShowAttempts--;
                    App.getApp().threads().getHandler().post(() -> anchorAdapterItem(item, adapterView));

                } else {
                    // We tried a few times and couldn't get it to show. This is likely because it didn't fit. Give up.
                    if (mListener != null) {
                        mListener.onTooltipFailed();
                    }
                    dismiss();
                }
            }
            return this;
        }

        /**
         * @see com.pocket.sdk.util.view.tooltip.Tooltip.Builder#show(View, boolean).
         */
        private TooltipController anchor(final View anchor, final boolean isAnchorScrollable) {
            if (mIsDismissed) {
                return this;
            }

            clearObservers();

            // Try to show
            boolean shown = applyAnchor(anchor);

            if (shown) {
                // Shown, now we watch for changes to the anchor position and move as needed.
                mHasShown = true;
                mAnchorBoundsWatcher = ViewBoundsWatcher.create(anchor, new ViewBoundsWatcher.OnViewAbsoluteBoundsChangedListener() {
                    @Override
                    public void onViewAbsoluteBoundsChanged(int left, int top, int right, int bottom) {
                        App.getApp().threads().getHandler().post(() -> applyAnchor(anchor));
                    }
                }, isAnchorScrollable);

                AbsPocketActivity activity = AbsPocketActivity.from(mAnchor.getContext());
                if (activity != null) {
                    activity.addOnBackPressedListener(mBackPressedListener);
                }

                if (mListener != null) {
                    mListener.onTooltipShown();
                }

            } else {
                /*
                 Failed to show.
                 This can happen if the style doesn't fit where the anchor is
                 or more likely, the anchor window hasn't attached yet or the anchor hasn't laid out yet
                 By default, we can attempt to retry the show in the next run loop or two.
                 */

                if (mShowAttempts > 0) {
                    // Retry on next loop
                    mShowAttempts--;
                    App.getApp().threads().getHandler().post(() -> anchor(anchor, isAnchorScrollable));

                } else {
                    // We tried a few times and couldn't get it to show. This is likely because it didn't fit. Give up.
                    if (mListener != null) {
                        mListener.onTooltipFailed();
                    }
                    dismiss();
                }
            }
            return this;
        }

        /**
         * Hide the tooltip.
         */
        public void dismiss() {
            dismiss(DismissReason.DISMISS_REQUESTED);
        }
    
        public void dismiss(DismissReason reason) {
            if (mIsDismissed) {
                return;
            }
            mIsDismissed = true;

            clearObservers();

            // Allow animations to complete
            mViews.dismiss();

            if (mListener != null && mHasShown) {
                mListener.onTooltipDismissed(reason);
            }
        }

        private void clearObservers() {
            if (mAnchorBoundsWatcher != null) {
                mAnchorBoundsWatcher.stop();
                mAnchorBoundsWatcher = null;
            }
            if (mAdapterViewWatcher != null) {
                mAdapterViewWatcher.stop();
                mAdapterViewWatcher = null;
            }
            final AbsPocketActivity activity = AbsPocketActivity.from(mAnchor != null ? mAnchor.getContext() : null);
            if (activity != null) {
                activity.removeOnBackPressedListener(mBackPressedListener);
            }
        }

        /**
         * This updates the tooltips position and state based on the new anchor position.
         * @param anchor
         * @return true if updated successfully, false if it couldn't because the anchor isn't attached, invisible or the tooltip couldn't fit on the screen.
         *          If it fails to update, it remains in the previous state unchanged.
         */
        private boolean applyAnchor(View anchor) {
            mAnchor = anchor;
            if (mIsDismissed || !ViewCompat.isAttachedToWindow(anchor) || anchor.getRootView() == null) {
                return false;
            } else if (anchor.getWidth() == 0 || anchor.getHeight() == 0) {
                return false;
            }

            Rect anchorBounds = mRecycleAnchorBounds;
            int[] xy = mRecycleXY;

            // Absolute Anchor bounds
            anchor.getLocationOnScreen(xy);
            anchorBounds.set(xy[0], xy[1], xy[0] + anchor.getWidth(), xy[1] + anchor.getHeight());

            return mViews.showViews(anchorBounds);
        }

        public void clickAnchor(DismissReason reason) {
            if (mAnchor != null) {
                if (!mAnchor.performClick()) {
                    // If adapterview, try an onItemClick
                    if (mAnchorsAdapterView != null && mAnchorsAdapterView.getOnItemClickListener() != null) {
                        int position = mAnchorsAdapterView.getPositionForView(mAnchor);
                        if (position != AdapterView.INVALID_POSITION) {
                            mAnchorsAdapterView.getOnItemClickListener()
                                    .onItemClick(mAnchorsAdapterView, mAnchor, position, mAnchorsAdapterView.getItemIdAtPosition(position));
                        }
                    }
                }
            }
            dismiss(reason);
        }
    }

    public interface TooltipListener {
        void onTooltipShown();
        void onTooltipFailed();
        void onTooltipDismissed(DismissReason reason);
    }

    public enum DismissReason {
        DISMISS_REQUESTED, ANCHOR_CLICKED, BUTTON_CLICKED
    }
}
