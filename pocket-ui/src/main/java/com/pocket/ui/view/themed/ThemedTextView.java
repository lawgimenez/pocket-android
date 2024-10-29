package com.pocket.ui.view.themed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.Touch;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatTextView;

import com.pocket.analytics.api.Engageable;
import com.pocket.analytics.api.EngageableHelper;
import com.pocket.analytics.api.EngagementListener;
import com.pocket.ui.R;
import com.pocket.ui.text.Fonts;
import com.pocket.ui.text.PressableSpan;
import com.pocket.ui.text.TextViewUtil;
import com.pocket.ui.util.NestedColorStateList;
import com.pocket.ui.view.visualmargin.VisualMargin;

import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class ThemedTextView extends AppCompatTextView implements VisualMargin, Engageable {

	private final EngageableHelper engageable = new EngageableHelper();

	public ThemedTextView(Context context) {
		this(context, null);
	}

	public ThemedTextView(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.textViewStyle);
	}

	public ThemedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		setPaintFlags(getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);

		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ThemedTextView, defStyleAttr, 0);
		applyTextAppearanceFromAttributes(a);
		a.recycle();

		if (!isInEditMode()) {
			engageable.obtainStyledAttributes(getContext(), attrs);
			engageable.setUiEntityType(Type.BUTTON);
		}
	}
	
	@Override
	public void setTextAppearance(Context context, int resid) {
		super.setTextAppearance(context, resid);
		
		TypedArray a = getContext().obtainStyledAttributes(resid, R.styleable.ThemedTextView);
		applyTextAppearanceFromAttributes(a);
		a.recycle();
	}
	
	private void applyTextAppearanceFromAttributes(TypedArray a) {
		if (a.hasValue(R.styleable.ThemedTextView_typeface) && !isInEditMode()) {
			setTypeface(Fonts.get(getContext(), a.getInt(R.styleable.ThemedTextView_typeface, 0)));
		}
		
		if (a.getBoolean(R.styleable.ThemedTextView_visualPadding, false)) {
			TextViewUtil.setVisualTextPadding(this, getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
		} else if (a.getBoolean(R.styleable.ThemedTextView_paddingVerticalCenter, false)) {
			TextViewUtil.verticallyCenterPadding(this);
		}
		
		int colors = a.getResourceId(R.styleable.ThemedTextView_compatTextColor, 0);
		if (colors != 0) {
			if (!isInEditMode()) {
				var colorStateList = NestedColorStateList.get(getContext(), colors);
				setTextColor(colorStateList);
				setLinkTextColor(colorStateList);
			}
		}
	}
	
	@Override
    public int[] onCreateDrawableState(int extraSpace) {
		final int[] state = super.onCreateDrawableState(extraSpace + 1);
		mergeDrawableStates(state, AppThemeUtil.getState(this));
		return state;
	}
	
	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		invalidate(); // Force paints to update, sometimes with a TextView if the text color didn't change, it skips invalidating the background which might have a different state.
	}

	public void setBold(boolean bold) {
		if (bold) {
			setTypeface(Fonts.get(getContext(), Fonts.Font.GRAPHIK_LCG_BOLD));
		} else {
			setTypeface(Fonts.get(getContext(), Fonts.Font.GRAPHIK_LCG_REGULAR));
		}
	}

	public void setMovementMethodForLinks() {
		setMovementMethod(new FixedLinkMethod());
	}
	
	@SuppressLint("ClickableViewAccessibility") // Super is always called, so this lint warning is too paranoid.
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (getMovementMethod() instanceof FixedLinkMethod) {
			// Only consume the event if on a link
			FixedLinkMethod method = (FixedLinkMethod) getMovementMethod();
			super.onTouchEvent(event);
			boolean isLinkTouch = method.isTouching;
			switch (event.getAction()) {
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					method.isTouching = false;
					method.clearPressedSpans();
					break;
			}
			return isLinkTouch;
		} else {
			return super.onTouchEvent(event);
		}
	}
	
	@Override
	public boolean prepareVisualDescent() {
		return false;
	}
	
	@Override
	public boolean prepareVisualAscent() {
		return false;
	}
	
	@Override
	public int visualAscent() {
		return (int) Math.ceil(TextViewUtil.ascent(this)) + getPaddingTop();
	}
	
	@Override
	public int visualDescent() {
		return (int) Math.ceil(TextViewUtil.descent(this)) + getPaddingBottom();
	}
	
	@Nullable @Override public String getUiEntityIdentifier() {
		return engageable.getUiEntityIdentifier();
	}

	@Override public void setUiEntityIdentifier(@Nullable String uiEntityIdentifier) {
		engageable.setUiEntityIdentifier(uiEntityIdentifier);
	}
	
	@Nullable @Override public Type getUiEntityType() {
		return engageable.getUiEntityType();
	}
	
	@Nullable @Override public String getUiEntityComponentDetail() {
		return engageable.getUiEntityComponentDetail();
	}
	
	@Override public void setUiEntityComponentDetail(@Nullable String value) {
		engageable.setUiEntityComponentDetail(value);
	}

	@Nullable @Override public String getUiEntityLabel() {
		return engageable.getUiEntityLabel();
	}

	@Override public void setEngagementListener(EngagementListener listener) {
		engageable.setEngagementListener(listener);
	}

	@Override public void setOnClickListener(@Nullable OnClickListener listener) {
		super.setOnClickListener(engageable.getWrappedClickListener(listener));
	}

	/**
	 * This is a workaround to allow touches to pass through to the views behind
	 * if they aren't touching a link within the text view. The normal LinkMovementMethod
	 * eats all touch events even if it isn't on a ClickableSpan.
	 * <p>
	 * This also provides support for {@link PressableSpan}.
	 */
	private class FixedLinkMethod extends LinkMovementMethod {
		
		private Set<PressableSpan> pressing = new HashSet<>();
		private boolean isTouching;
		
		@Override
		public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
			int action = event.getAction();
			
			if (action == MotionEvent.ACTION_UP ||
					action == MotionEvent.ACTION_DOWN ||
					action == MotionEvent.ACTION_CANCEL) {
				int x = (int) event.getX();
				int y = (int) event.getY();

				x -= widget.getTotalPaddingLeft();
				y -= widget.getTotalPaddingTop();

				x += widget.getScrollX();
				y += widget.getScrollY();

				Layout layout = widget.getLayout();
				int line = layout.getLineForVertical(y);
				int off = layout.getOffsetForHorizontal(line, x);
				
				// Support for PressableSpan
				PressableSpan[] pressables = buffer.getSpans(off, off, PressableSpan.class);
				if (pressables.length != 0) {
					for (PressableSpan span : pressables) {
						switch (action) {
							case MotionEvent.ACTION_UP:
							case MotionEvent.ACTION_CANCEL:
								clearPressedSpans();
								break;
							case MotionEvent.ACTION_DOWN:
								span.setPressed(true);
								if (pressing.add(span)) {
									refreshDrawableState();
								}
								break;
						}
					}
				}

				// ClickableSpan workaround
				ClickableSpan[] links = buffer.getSpans(off, off, ClickableSpan.class);
				if (links.length != 0) {
					ClickableSpan link = links[0];
					if (action == MotionEvent.ACTION_UP) {
						link.onClick(widget);

					} else if (action == MotionEvent.ACTION_DOWN) {
						Selection.setSelection(buffer,
								buffer.getSpanStart(link),
								buffer.getSpanEnd(link));
					}
					isTouching = true;
					return true;

				} else {
					Selection.removeSelection(buffer);
					return false;
				}
			}

			return Touch.onTouchEvent(widget, buffer, event);
		}
		
		private void clearPressedSpans() {
			if (!pressing.isEmpty()) {
				for (PressableSpan span : pressing) {
					span.setPressed(false);
				}
				pressing.clear();
				refreshDrawableState();
			}
		}
	}

	public void setTextAndUpdateEnUsLabel(@StringRes int resId) {
		if (resId != 0) {
			setText(resId);
			if (!isInEditMode()) {
				engageable.updateEnUsLabel(getContext(), resId);
			}
		} else {
			setText(null);
			engageable.updateEnUsLabel(null);
		}
	}

	public void setTextAndUpdateEnUsLabel(CharSequence displayText, String label) {
		setText(displayText);
		engageable.updateEnUsLabel(label);
	}
}
