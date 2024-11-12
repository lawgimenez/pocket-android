package com.pocket.ui.view.empty;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.pocket.ui.R;
import com.pocket.ui.util.PocketUIViewUtil;
import com.pocket.ui.view.visualmargin.VisualMarginConstraintLayout;
import com.pocket.ui.view.progress.RainbowProgressCircleView;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * A view that can display a loading, loaded or error state.
 */
public class LoadableLayout extends VisualMarginConstraintLayout {
	
	private final Binder binder = new Binder(this);
	private EmptyView empty;

	private RainbowProgressCircleView defaultProgressView;
	private View currentProgressView;

	public LoadableLayout(Context context) {
		super(context);
		init();
	}
	
	public LoadableLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public LoadableLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}
	
	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.view_loadable, this, true);
		defaultProgressView = findViewById(R.id.progress);
		currentProgressView = defaultProgressView;
		empty = findViewById(R.id.empty);
	}
	
	public Binder bind() {
		return binder;
	}
	
	public static class Binder {
		
		private final LoadableLayout view;
		
		private Binder(LoadableLayout view) {
			this.view = view;
		}
		
		public Binder clear() {
			view.empty.bind().clear();
			customProgressIndicator(view.defaultProgressView);
			showProgressIndeterminate();
			return this;
		}
		
		public EmptyView.Binder showEmptyOrError() {
			view.empty.setVisibility(VISIBLE);
			view.currentProgressView.setVisibility(GONE);
			return view.empty.bind();
		}
		
		public Binder showProgressIndeterminate() {
			view.defaultProgressView.setProgressIndeterminate(true);
			showProgress();
			return this;
		}

		// TODO custom progress views don't currently support determinate progress
		public Binder showProgress(float progress) {
			view.defaultProgressView.setProgress(progress);
			showProgress();
			return this;
		}

		/**
		 * Adds a custom View as the progress indicator.
		 *
		 * @param v The custom progress indicator View
		 */
		public Binder customProgressIndicator(View v) {
			if (v == view.currentProgressView) {
				return this;
			}
			if (v == null) {
				if (view.currentProgressView != view.defaultProgressView) {
					view.defaultProgressView.setVisibility(view.currentProgressView.getVisibility());
					PocketUIViewUtil.replaceView(view.currentProgressView, view.defaultProgressView);
					constraintParamsWrapContent();
				}
			} else {
				v.setVisibility(view.currentProgressView.getVisibility());
				PocketUIViewUtil.replaceView(view.currentProgressView, v);
				view.currentProgressView = v;
				constraintParamsFillWidth();
			}
			return this;
		}
		
		private void showProgress() {
			view.empty.setVisibility(GONE);
			view.currentProgressView.setVisibility(VISIBLE);
		}

		// This is a hack for fixing constraintlayout params.
		// The default progress circle wraps its content and those params get copied in
		// replaceView.  Here we make the custom view match the parent size with width / height = 0.
		private void constraintParamsFillWidth() {
			view.currentProgressView.getLayoutParams().width = 0;
			view.currentProgressView.getLayoutParams().height = 0;
		}

		private void constraintParamsWrapContent() {
			view.currentProgressView.getLayoutParams().width = WRAP_CONTENT;
			view.currentProgressView.getLayoutParams().height = WRAP_CONTENT;
		}
		
	}
	
}
