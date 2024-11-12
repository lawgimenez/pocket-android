package com.pocket.ui.view.empty;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pocket.ui.R;
import com.pocket.ui.view.themed.ThemedLottieAnimationView;
import com.pocket.ui.view.visualmargin.VisualMarginConstraintLayout;
import com.pocket.util.android.ViewUtilKt;

/**
 * A view for displaying empty or error messages where content loaded empty or had an error loading.
 */
public class EmptyView extends VisualMarginConstraintLayout {
	
	private final Binder binder = new Binder(this);
	private TextView title;
	private TextView message;
	private TextView button;
	private TextView errorButton;
	private TextView details;
	private View detailsDivider;
	private ViewGroup animationContainer;
	
	public EmptyView(Context context) {
		super(context);
		init();
	}
	
	public EmptyView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public EmptyView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}
	
	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.view_empty, this, true);
		title = findViewById(R.id.title);
		message = findViewById(R.id.message);
		button = findViewById(R.id.button);
		errorButton = findViewById(R.id.error_button);
		detailsDivider = findViewById(R.id.detail_divider);
		details = findViewById(R.id.details);
		animationContainer = findViewById(R.id.animation_container);
	}
	
	public Binder bind() {
		return binder;
	}
	
	public static class Binder {
		
		private final EmptyView view;
		
		private Binder(EmptyView view) {
			this.view = view;
		}
		
		public Binder clear() {
			title(null);
			message(null);
			button(null);
			details(null);
			buttonOnClick(null);
			buttonOnLongClick(null);
			animationView(null);
			return this;
		}
		
		public Binder title(CharSequence value) {
			ViewUtilKt.setTextOrHide(view.title, value);
			return this;
		}
		
		public Binder message(CharSequence value) {
			ViewUtilKt.setTextOrHide(view.message, value);
			return this;
		}
		
		public Binder button(CharSequence value) {
			ViewUtilKt.setTextOrHide(view.button, value);
			ViewUtilKt.setTextOrHide(view.errorButton, null);
			return this;
		}
		
		public Binder errorButton(CharSequence value) {
			ViewUtilKt.setTextOrHide(view.errorButton, value);
			ViewUtilKt.setTextOrHide(view.button, null);
			return this;
		}
		
		public Binder buttonOnClick(View.OnClickListener listener) {
			view.button.setOnClickListener(listener);
			view.errorButton.setOnClickListener(listener);
			return this;
		}
		
		public Binder buttonOnLongClick(View.OnLongClickListener listener) {
			view.button.setOnLongClickListener(listener);
			view.errorButton.setOnLongClickListener(listener);
			view.button.setLongClickable(listener != null);
			view.errorButton.setLongClickable(listener != null);
			return this;
		}
		
		public Binder details(CharSequence value) {
			ViewUtilKt.setTextOrHide(view.details, value);
			view.detailsDivider.setVisibility(view.details.getVisibility());
			return this;
		}

		public Binder animationView(ThemedLottieAnimationView animationView) {
			view.animationContainer.removeAllViews();
			if (animationView != null) {
				view.animationContainer.addView(animationView);
				animationView.playAnimation(); // plays the animation as soon as the View is shown
			}
			return this;
		}
	}
	
}
