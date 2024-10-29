package com.pocket.ui.view.button;

import android.content.Context;
import android.util.AttributeSet;

import com.pocket.ui.R;

public class CheckBox extends IconButton {

    public CheckBox(Context context) {
        super(context);
        init();
    }

    public CheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CheckBox(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setImageResource(R.drawable.ic_pkt_check);
        setScaleType(ScaleType.CENTER);
        setCheckable(true);
        setDrawableColor(R.color.pkt_checkbox);
    }
}
