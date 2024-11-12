package com.pocket.util.android.webkit;


import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AbsoluteLayout;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sdk.util.view.RainbowBar;
import com.pocket.ui.view.themed.ThemedView;
import com.pocket.util.android.ViewUtil;
import com.pocket.util.android.WebViewUtil;
import com.pocket.util.android.animation.AnimatorEndListener;
import com.pocket.util.android.animation.Interpolators;
import com.pocket.util.android.view.ManuallyUpdateTheme;
import com.pocket.util.android.view.RecyclingImageView;
import com.pocket.util.android.view.ScrollTracker;

import java.util.ArrayList;


@SuppressWarnings("deprecation")
public class BaseWebView extends WebView implements ManuallyUpdateTheme {

	protected ResizeListener mResizeListener;

	protected OnInteractionListener mInteractionListener;
	
	private RelativeLayout mContainerFrame;
	
	private boolean mScrollingLocked;

	private RainbowBar mProgressBar;
	private boolean mIsProgressBarVisible;
	private int mProgress;

	protected OnContentDisplayedListener mOnContentDisplayedListener;
	protected boolean mHasContentDisplayed = false;
	
	private RecyclingImageView mFrozenView;
	private AlphaAnimation mSwipedAnimation;
	
	private Runnable mDelayUntilNextDraw;
	private ThemedView mArticleLoadingBlocker;
	private boolean mFrozen;
	private final ArrayList<View> mFixedHorizontally = new ArrayList<View>();
	private final ArrayList<View> mFixedVertically = new ArrayList<View>();
	private ScrollTracker mScrollTracker;
	
    public BaseWebView(Context context) {
		super(context);
		init();
	}

	public BaseWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public BaseWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	@SuppressLint("SetJavaScriptEnabled")
	protected void init() {
		WebSettings settings = getSettings();
		settings.setJavaScriptEnabled(true);
		setFileAccessEnabled(false); // Default off for security.
		
		setOverScrollMode(View.OVER_SCROLL_NEVER);
		setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		
		updateThemeManually();
		
		registerForThemeUpdates();
		
		mScrollTracker = new ScrollTracker(this, ViewConfiguration.get(getContext()).getScaledTouchSlop());
	}

	public void setFileAccessEnabled(boolean enabled) {
		WebSettings settings = getSettings();
		settings.setAllowFileAccess(enabled);
		settings.setAllowUniversalAccessFromFileURLs(enabled);
	}

	private void checkContentDisplay() {
		if (getContentHeight() > 0) {
			if (!mHasContentDisplayed) {
				mHasContentDisplayed = true;
				if (mOnContentDisplayedListener != null) {
					mOnContentDisplayedListener.onContentFirstDisplayedSinceLoad();
				}
			}
			
			if (mOnContentDisplayedListener != null) {
				mOnContentDisplayedListener.onContentDisplayed();
			}
			
		} else if (getContentHeight() <= 0) {
			mHasContentDisplayed = false;
			
		}
	}
	
	@Override
	public void invalidate() {
		super.invalidate();
		checkContentDisplay();
	}

	public boolean hasContentDisplayed() {
		return mHasContentDisplayed;
	}
	
	// Interaction Detection

	public interface OnInteractionListener{
		public void onInteraction();
		/**
		 * Called anytime a touch down, or a key down is made.
		 */
		public void onPossibleSelect(); // REVIEW these methods are a bit redunant. onInteraction covers scrolls, but why?
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
			if (mInteractionListener != null) {
				mInteractionListener.onPossibleSelect();
			}
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (ev == null) {
			return false;
		}
		
		mScrollTracker.onTouchEvent(ev);
		
		if (mInteractionListener != null) {
			mInteractionListener.onInteraction();
			mInteractionListener.onPossibleSelect();
		}
		
		switch (ev.getAction()) {
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			break;
			
		case MotionEvent.ACTION_DOWN:
			break;
			
		case MotionEvent.ACTION_MOVE:
			if (mScrollingLocked) {
				return true;
			}
			break;
		}
		
		return super.onTouchEvent(ev);
	}
	
	public void setOnInteractionListener(OnInteractionListener listener) {
		mInteractionListener = listener;
	}
	
	
	// Scroll Detection
	
	
	
	@Override
	protected void onScrollChanged(int x, int y, int oldx, int oldy) {
		mScrollTracker.onScrollChanged(x, y, oldx, oldy);
		
		if (mInteractionListener != null) {
			mInteractionListener.onInteraction();
		}
		
		super.onScrollChanged(x, y, oldx, oldy);
	}
	
	public void setOnScrollListener(ScrollTracker.OnScrollListener listener) {
		mScrollTracker.setOnScrollListener(listener);
	}
	
	@Override
	public void scrollTo(int x, int y){
		if (y < 0)
			y = 0;
		
		super.scrollTo(x, y);
	}
	
	
	// Resize Detection
	
	public void setOnResizeListener(ResizeListener listener) {
		mResizeListener = listener;
	}
	
	public interface ResizeListener{
		public void onComputeVerticalScrollExtent();
		public void onSizeChanged(int newWidth, int newHeight, int oldWidth, int oldHeight);
	}
	
	@Override
	protected void onSizeChanged(int newWidth, int newHeight, int oldWidth, int oldHeight) {
		super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
		if(mResizeListener != null)
			mResizeListener.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
	}
	
	@Override
	protected int computeVerticalScrollExtent() {
		if(mResizeListener != null)
			mResizeListener.onComputeVerticalScrollExtent();
		return super.computeVerticalScrollExtent();
	}
	
	/**
	 * Freezing will screen cap the WebView and layer it over itself. While frozen it will block touches. This simply overlays, it does not stop the WebView from drawing or working.
	 * Unfreezing will hide the screen cap and again show the WebView below it.
	 *  
	 * @param freeze 
	 * @param animate ignored when freezing.  if true and unfreezing, it will fade the screen cap away.
	 */
	public void setFrozen(boolean freeze, final boolean animate) {
		boolean invalidate = false;
		
		if (freeze) {
			if (mFrozenView == null) {
				mFrozenView = new RecyclingImageView(getContext());
				mFrozenView.setScaleType(ScaleType.CENTER);
				mFrozenView.setClickable(true);
				/* TEST 
				mFrozenView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						execJS("article.didFreeze();"); 
					}
				});
				*/
				mFrozenView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
				addFixedView(mFrozenView);
			}
			mFrozenView.setVisibility(View.GONE);
			
			// Screencap the webview
			Bitmap screencap = null;
			setDrawingCacheEnabled(true);
			Bitmap cache = getDrawingCache();
			if (cache != null){
				screencap = Bitmap.createBitmap(cache);
			}
			setDrawingCacheEnabled(false);
			
			if (screencap == null)
				return; // QUESTION throw?
			
			mFrozenView.setImageBitmap(screencap);
			//mFrozenView.getDrawable().setAlpha(240); // TEST  
			//mFrozenView.setBackgroundColor(Color.GREEN); // TEST
			mFrozenView.bringToFront();
			mFrozenView.setVisibility(View.VISIBLE);
			
		} else if (mFrozenView != null) {
			mDelayUntilNextDraw = new Runnable() {

				@Override
				public void run() {
					if (animate) {
						// Animate
						if (mSwipedAnimation == null) {
							mSwipedAnimation = new AlphaAnimation(1, 0);
							mSwipedAnimation.setDuration(120);
							mSwipedAnimation.setAnimationListener(new AnimationListener() {
								
								@Override
								public void onAnimationStart(Animation animation) {}
								
								@Override
								public void onAnimationRepeat(Animation animation) {}
								
								@Override
								public void onAnimationEnd(Animation animation) {
									mFrozenView.setVisibility(View.GONE);
									mFrozenView.setImageBitmap(null); // This will remove the bitmap from memory
								}
							});
						}
						mFrozenView.startAnimation(mSwipedAnimation);
						
					} else {
						mFrozenView.setVisibility(View.GONE);
					}
				}
				
			};
			invalidate = true;
		}
		mFrozen = freeze;
		if (invalidate) {
			invalidate();
		}
	}
	
	@Override
	public void draw(Canvas canvas) {
		if (!mFrozen) {
			super.draw(canvas);
		}
	}
	
	@SuppressLint("WrongCall")
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		mScrollTracker.onDraw();
		
		if (mDelayUntilNextDraw != null) {
			mDelayUntilNextDraw.run();
			mDelayUntilNextDraw = null;
		}
	}
	
	
	/**
	 * Shows/Hides an overlay of the current theme's background color. In some cases (when loading html5 pages or articles) the page will load white for a moment.
	 * This is pretty annoying in night mode (a bright flash of white) when loading.  This allows you to turn on a block for this.
	 * 
	 * @param visible
	 */
	public void setContentVisible(final boolean visible) { 
		if (mArticleLoadingBlocker == null) {
			mArticleLoadingBlocker = new ThemedView(getContext());
			mArticleLoadingBlocker.setBackgroundResource(com.pocket.ui.R.drawable.cl_pkt_bg);
			addPositionedView(mArticleLoadingBlocker, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0, 0, 0);
			setChildFixedVertically(mArticleLoadingBlocker, true);
			setChildFixedHorizontally(mArticleLoadingBlocker, true);
		}
		
		if (!visible) {
			mArticleLoadingBlocker.requestLayout();
		}
		
		mArticleLoadingBlocker.setVisibility(!visible ? View.VISIBLE : View.GONE);
	}
	
	
	/**
	 * Add a view that will stay in a fixed position, rather than scrolling with the WebView.
	 * 
	 * A RelativeLayout will be created to hold these views (and the WebView) so use RelativeLayout.LayoutParams where needed.
	 * 
	 * @param view
	 */
	public void addFixedView(View view) {
		if (mContainerFrame == null) {
			mContainerFrame = new RelativeLayout(getContext());
			mContainerFrame.setLayoutParams(getLayoutParams());
			
			// Take this webview out of its parent and put it into the new frame, which is added to the parent
			ViewGroup parent = ((ViewGroup) getParent());
			parent.addView(mContainerFrame);
			parent.removeView(this);
			mContainerFrame.addView(this, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
		}
		
		mContainerFrame.addView(view);
	}
	
	
	public void addPositionedView(View view, int width, int height, int x, int y, int index) {
		AbsoluteLayout.LayoutParams lp = new AbsoluteLayout.LayoutParams(width, height, x, y);
		addPositionedView(view, lp, index);
	}
	
	public void addPositionedView(View view, AbsoluteLayout.LayoutParams lp, int index) {
		view.setLayoutParams(lp);
		if (index >= 0) {
			addView(view, index);
		} else {
			addView(view, getChildCount());
		}
	}
	
	// Javascript Commands
	
	/**
	 * Deprecated in favor of {@link JavascriptFunction}.
	 * <p>
	 * Convenience method that ensures the javascript will be executed with "javascript: " at the beginning, and from the UI Thread.
	 * @param query
	 */
	@Deprecated
	public void execJS (final String query) {
		App.from(getContext()).threads().runOrPostOnUiThread(() -> {
			String js = "javascript: ".concat(query);
			loadUrl(js);
		});
	}
	
	public void setProgressBarVisibility(boolean show) {
        if (mIsProgressBarVisible == show) {
            return;
        }

		if (show) {
            mIsProgressBarVisible = true;
            if (mProgressBar == null) {
                int height = (int) getResources().getDimension(R.dimen.webview_progress_height);
                mProgressBar = new RainbowBar(getContext());

                LayoutParams lp = new AbsoluteLayout.LayoutParams(
                        AbsoluteLayout.LayoutParams.MATCH_PARENT,
                        height,
                        0,0);

                mProgressBar.setLayoutParams(lp);
                mProgressBar.setMinimumHeight(height);

                setChildFixedHorizontally(mProgressBar, true);
                addView(mProgressBar);
            }
            mProgressBar.getRainbow().startProgressAnimation();

            ViewUtil.setVisible(mProgressBar, true);
            mProgressBar.animate()
                    .alpha(1)
                    .setDuration(333)
                    .setInterpolator(Interpolators.DECEL)
                    .setListener(null);

        } else {
            mIsProgressBarVisible = false;

            if (mProgressBar != null) {
                mProgressBar.animate()
                        .alpha(0)
                        .setDuration(333)
                        .setInterpolator(Interpolators.DECEL)
                        .setListener(new AnimatorEndListener() {
                            @Override
                            public void onAnimationEnd(Animator animator) {
                                mProgressBar.getRainbow().stopProgressAnimation();
                                ViewUtil.setVisible(mProgressBar, false);
                            }
                        });
            }
        }
	}

    /**
     * This no longer has a visual effect. It only serves to provide a value for {@link #getProgress()}
     * @param progress
     */
	public void setProgress(int progress) {
		mProgress = progress;
	}
	
	@Override
	public int getProgress() {
       return mProgress;
    }
	
	public void invalidateProgressBar() {
		if (mProgressBar == null)
			return;
		
		mProgressBar.invalidate();
	}
	
	@Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (mFixedHorizontally.contains(child)) {
        	child.offsetLeftAndRight(getScrollX() - child.getLeft());
        }
        
        if (mFixedVertically.contains(child)) {
        	child.offsetTopAndBottom(getScrollY() - child.getTop());
        }
		
		if (child == mProgressBar) {
        	mProgressBar.offsetTopAndBottom(getScrollY() - mProgressBar.getTop());
        }
        return super.drawChild(canvas, child, drawingTime);
    }
	
	/**
	 * Flag a child view to be fixed horizontally. It will still scroll with the WebView vertically, but not horizontally.
	 * @param child
	 */
	public void setChildFixedHorizontally(View child, boolean fixed) {
		if (fixed) {
			mFixedHorizontally.add(child); // OPT views won't garbage collect. use weak references.
		} else {
			mFixedHorizontally.remove(child); // OPT views won't garbage collect. use weak references.
		}
		invalidate();
	}
	
	/**
	 * Flag a child view to be fixed vertically. It will still scroll with the WebView vertically, but not horizontally.
	 * @param child
	 */
	public void setChildFixedVertically(View child, boolean fixed) {
		if (fixed) {
			mFixedVertically.add(child); // OPT views won't garbage collect. use weak references.
		} else {
			mFixedVertically.remove(child); // OPT views won't garbage collect. use weak references.
		}
		invalidate();
	}
	
	public float webPixelsPerScreenPixels(){
		return 100f / (getScale() * 100f);
	}
	
	public int screenPxToWebPx(float screenPx) {
		return (int) (screenPx * webPixelsPerScreenPixels());
	}
	
	/**
	 * Convert a dimension in web to how many Android pixels it represents.
	 * 
	 * For example, if an element in the webpage is "100px" tall, 
	 * the number of how many actual physical pixels it takes up,
	 * depends on the density of the device and the zoom/scale of the page.
	 * 
	 * This tells you how many screen pixels are used to display a pixel value
	 * in the page's css.
	 * 
	 * @param webPx
	 * @return
	 */
	public int webPxToScreenPx(int webPx) {
		return Math.round(webPx / webPixelsPerScreenPixels());
	}
	
	/**
	 * This returns in web pixels, the maximum scroll Y that web can have.
	 * 
	 * @return
	 */
	public int getMaxContentScrollY() {
		return webPxToScreenPx(getContentHeight()) - getHeight();
	}

	public int getContentHeightInViewPixels() {
		return webPxToScreenPx(getContentHeight());
	}

	public void lockScrolling(boolean lock) {
		mScrollingLocked = lock;
	}
	
	public void setOnContentDisplayedListener(OnContentDisplayedListener listener) {
		mOnContentDisplayedListener = listener;
	}
	
	/**
	 * An listener for when a BaseWebView first displays content while loading.
	 * @author max
	 *
	 */
	public interface OnContentDisplayedListener {
		/**
		 * Called when the WebView goes from not having any content displayed to having something displayed.
		 */
		public void onContentFirstDisplayedSinceLoad();

		/**
		 * Called whenever the WebView's Picture is updated and has content. Will be called after onContentFirstDisplayedSinceLoad()
		 */
		public void onContentDisplayed();
	}
	
	@Override
	public void registerForThemeUpdates() {
		AbsPocketActivity activity = AbsPocketActivity.from(getContext());
		if (activity != null) activity.registerViewForThemeChanges(this);
	}

	@Override
	public void updateThemeManually() {
		setBackgroundColor(App.from(getContext()).theme().getThemeBGColor(getContext()));
	}
	
	public void getSelectedText(WebViewUtil.SelectedTextCallback callback) {
		// REVIEW why do we stop text selection immediately here?
		try {
			WebViewUtil.getSelectedText(this, selectedText -> {
				if (selectedText != null) {
					stopTextSelection();
				}
				callback.onTextSelectionRetrieved(selectedText);
			});
				
		} catch (Throwable t) {
			App.from(getContext()).errorReporter().reportError(t);
		}
	}
	
	public void stopTextSelection() {
		loadUrl("javascript:"); // loadUrl clears action modes.
		invalidate();
	}
	
	public boolean isFlinging() {
		return mScrollTracker.isFlinging();
	}
	
}
