package com.pocket.ui.view.themed;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.pocket.analytics.api.Engageable;
import com.pocket.analytics.api.EngageableHelper;
import com.pocket.analytics.api.EngagementListener;
import com.pocket.ui.view.visualmargin.VisualMargin;

import org.jetbrains.annotations.Nullable;

/**
 * A themed version of {@link RelativeLayout} for Views which have not yet been fully ported over to pocket-ui.
 *
 * New Views should generally use {@link com.pocket.ui.view.visualmargin.VisualMarginConstraintLayout} rather than RelativeLayout
 * since it supports {@link VisualMargin} and is more flexible to change View positioning in the future.
 */
public class ThemedRelativeLayout extends RelativeLayout implements Engageable {

    protected final EngageableHelper engageable = new EngageableHelper();

    public ThemedRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ThemedRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public ThemedRelativeLayout(Context context) {
        super(context);
        init(null);
    }

    private void init(AttributeSet attrs) {
        engageable.obtainStyledAttributes(getContext(), attrs);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] state = super.onCreateDrawableState(extraSpace + 1);
        mergeDrawableStates(state, AppThemeUtil.getState(this));
        return state;
    }

    @Nullable
    @Override public String getUiEntityIdentifier() {
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

    @Override public void setEngagementListener(@Nullable EngagementListener listener) {
        engageable.setEngagementListener(listener);
    }

    @Override public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(engageable.getWrappedClickListener(l));
    }
}