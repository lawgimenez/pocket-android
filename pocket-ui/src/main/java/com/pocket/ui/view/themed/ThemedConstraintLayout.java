package com.pocket.ui.view.themed;

import android.content.Context;
import android.util.AttributeSet;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.pocket.analytics.api.Engageable;
import com.pocket.analytics.api.EngageableHelper;
import com.pocket.analytics.api.EngagementListener;

import org.jetbrains.annotations.Nullable;

public class ThemedConstraintLayout extends ConstraintLayout implements Engageable {
	protected final EngageableHelper engageable = new EngageableHelper();

	public ThemedConstraintLayout(Context context) {
		this(context, null);
	}

	public ThemedConstraintLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ThemedConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		engageable.obtainStyledAttributes(context, attrs);
	}
	
	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		final int[] state = super.onCreateDrawableState(extraSpace + 1);
		mergeDrawableStates(state, AppThemeUtil.getState(this));
		return state;
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

	@Nullable @Override public String getUiEntityLabel() {
		return engageable.getUiEntityLabel();
	}
	
	@Override public void setEngagementListener(@Nullable EngagementListener listener) {
		engageable.setEngagementListener(listener);
	}
	
	@Override public void setOnClickListener(@Nullable OnClickListener l) {
		super.setOnClickListener(engageable.getWrappedClickListener(l));
	}

	public void setUiEntityComponentDetail(String detail) {
		engageable.setUiEntityComponentDetail(detail);
	}

	public void setUiEntityType(Type type) {
		engageable.setUiEntityType(type);
	}
}
