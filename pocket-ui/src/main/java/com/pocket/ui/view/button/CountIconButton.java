package com.pocket.ui.view.button;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.TooltipCompat;

import com.pocket.ui.R;
import com.pocket.ui.util.CheckableHelper;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.util.IntrinsicSizeHelper;
import com.pocket.ui.util.NestedColorStateList;
import com.pocket.ui.view.checkable.CheckableConstraintLayout;
import com.pocket.util.android.ViewUtilKt;

/**
 * An IconButton with a counter next to it.
 * Attributes available:  countIconSrc, countIconCheckedSrc, countIconColor
 * <p>
 * Do not use the view's onClick, onCheckedChange, setChecked methods directly, instead use {@link #bind()} to control listeners and state.
 * <p>
 * A natural width is provided when using a wrap_content, this should be enough to avoid size changing when counts change.
 * Natural height gives enough to have a good touch area.
 */
public class CountIconButton extends CheckableConstraintLayout {

	private final IntrinsicSizeHelper sizeHelper = new IntrinsicSizeHelper(-1, DimenUtil.dpToPxInt(getContext(), 42)); // Make the height default to sat least a nice tappable size.
	private final Binder binder = new Binder(this);
	private CheckableHelper.OnCheckedChangeListener checkedListener;
	
	private IconButton icon;
	private TextView count;
	private final int countPadding = DimenUtil.dpToPxInt(getContext(), 4);
	
	public CountIconButton(Context context) {
		super(context);
		init(null);
	}

	public CountIconButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public CountIconButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
	}
	
	private void init(AttributeSet attrs) {
		LayoutInflater.from(getContext()).inflate(R.layout.view_count_icon, this, true);
		icon = findViewById(R.id.icon);
		count = findViewById(R.id.count);
		
		icon.setLongClickable(false); // By default, IconButton's have tooltips on long press, we don't want this icon to take touches.

		setCheckable(true);

		if (attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CountIconButton);
			icon.setImageResource(a.getResourceId(R.styleable.CountIconButton_countIconSrc, 0));
			ColorStateList colors = NestedColorStateList.get(getContext(), a.getResourceId(R.styleable.CountIconButton_countIconColor, 0));
			icon.setDrawableColor(colors);
			count.setTextColor(colors);
			a.recycle();
		}
		
		checkedListener = (view, isChecked) -> {
			if (isChecked != binder.listener.onCountButtonClicked(CountIconButton.this, isChecked)) {
				bind().checked(!isChecked); // Don't change.
			}
			// TODO change the count immediately on their behalf?
		};
		setOnCheckedChangeListener(checkedListener);
		bind().clear();
	}

	/**
	 * Some {@link CountIconButton}s can be clickable, but not checkable, e.g. Repost opens the Repost screen, but should not immediately toggle its state.
	 * In these cases, this button will revert to using its {@link Binder.OnCountButtonClickListener} as a regular {@link android.view.View.OnClickListener}.
	 * @param checkable
	 */
	@Override
	public void setCheckable(boolean checkable) {
		super.setCheckable(checkable);
		if (checkable) {
			setOnClickListener(null);
		} else {
			setOnClickListener(v -> binder.listener.onCountButtonClicked(CountIconButton.this, CountIconButton.this.isChecked()));
		}
	}

	@Override
	public void setContentDescription(CharSequence contentDescription) {
		super.setContentDescription(contentDescription);
		TooltipCompat.setTooltipText(this, contentDescription);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		widthMeasureSpec = sizeHelper.applyWidth(widthMeasureSpec);
		heightMeasureSpec = sizeHelper.applyHeight(heightMeasureSpec);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	public Binder bind() {
		return binder;
	}
	
	public static class Binder {
		
		private static final OnCountButtonClickListener NO_OP_LISTENER = (view, checked) -> checked;
		
		private final CountIconButton view;
		private OnCountButtonClickListener listener;
		
		public Binder(CountIconButton view) {
			this.view = view;
		}
		
		public Binder clear() {
			checked(false);
			enabled(true);
			count(0);
			setOnCountIconButtonClickedListener(null);
			return this;
		}
		
		public Binder checked(boolean isSaved) {
			view.setOnCheckedChangeListener(null); // Clear the listener while changing bindings, so it only triggers from actual clicks.
			view.setChecked(isSaved);
			view.setOnCheckedChangeListener(view.isCheckable() ? view.checkedListener : null);
			return this;
		}
		
		public Binder enabled(boolean value) {
			view.setEnabled(value);
			return this;
		}
		
		public Binder count(int count) {
			String text;
			if (count <= 0)  {
				text = null;
			} else if (count < 1000) {
				text = String.valueOf(count);
			} else if (count < 1000000) {
				text = view.getResources().getString(R.string.counts_thousands, (int) Math.floor(count / 1000));
			} else {
				text = view.getResources().getString(R.string.counts_millions, (int) Math.floor(count / 1000000));
			}
			ViewUtilKt.setTextOrHide(view.count, text, INVISIBLE);
			view.count.setPadding(view.count.getVisibility() == View.VISIBLE ? view.countPadding : 0, 0, 0, 0);
			return this;
		}
		
		public Binder setOnCountIconButtonClickedListener(OnCountButtonClickListener listener) {
			this.listener = listener != null ? listener : NO_OP_LISTENER;
			return this;
		}
		
		public interface OnCountButtonClickListener { // TODO SaveButton uses something similar, if we find we use this pattern elsewhere, we could make it part of CheckableHelper somehow?
			/**
			 * @param checked true if the button was clicked and wants to move to the saved state. false if wants to become not saved.
			 * @return the state the button should be in
			 */
			boolean onCountButtonClicked(CountIconButton view, boolean checked);
		}
		
	}
	
	
}
