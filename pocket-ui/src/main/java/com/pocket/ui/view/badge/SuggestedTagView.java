package com.pocket.ui.view.badge;

import android.content.Context;
import android.util.AttributeSet;

import com.pocket.ui.R;
import com.pocket.ui.view.button.ButtonBoxDrawable;

import androidx.core.content.ContextCompat;

/**
 * A badge that shows a tag name, for use in "suggested tags". Use normal text view setText methods to set the tag.
 *
 * https://www.figma.com/file/Qqwh8xKl4Gy4YMv6mzw2gCO9/CLEAN?node-id=93%3A633
 */
public class SuggestedTagView extends TextBadgeView {

    public SuggestedTagView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SuggestedTagView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SuggestedTagView(Context context) {
        super(context);
    }

    @Override
    protected void init() {
        super.init();
        // using setBackground directly instead of TextBadgeView.setBadgeColor in order to add the stroke outline with ButtonBoxDrawable
        setBackground(new ButtonBoxDrawable(getContext(), R.color.pkt_opaque_touchable_area, R.color.pkt_themed_grey_5));
        setTextColor(ContextCompat.getColorStateList(getContext(), R.color.pkt_themed_grey_1));
    }

}