package com.pocket.ui.view.edittext;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.util.NestedColorStateList;
import com.pocket.ui.view.button.ButtonBoxDrawable;
import com.pocket.ui.view.themed.ThemedEditText;

/**
 * A Pocket themed EditText, with a thin grey oval background.
 *
 * https://www.figma.com/file/Qqwh8xKl4Gy4YMv6mzw2gCO9/CLEAN?node-id=417%3A590
 */
public class PktEditText extends ThemedEditText {

    public PktEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public PktEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PktEditText(Context context) {
        super(context);
        init();
    }

    private void init() {
        setTextAppearance(getContext(), R.style.Pkt_Text_EditText);
        final int paddingHor = (int) getResources().getDimension(R.dimen.pkt_space_md);
        final int paddingVer = DimenUtil.dpToPxInt(getContext(), 14);
        setPadding(paddingHor, paddingVer, paddingHor, paddingVer);
        setHintTextColor(NestedColorStateList.get(getContext(), R.color.pkt_themed_grey_3));
        setBackgroundDrawable(new ButtonBoxDrawable(getContext(), R.color.pkt_bg, R.color.pkt_focusable_grey_4));
        setGravity(Gravity.TOP);
    }

}
