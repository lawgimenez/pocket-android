package com.pocket.ui.view.button;

import android.content.Context;
import android.util.AttributeSet;

import com.pocket.ui.R;

public class UpgradeButton extends BoxButtonBase {

    public UpgradeButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public UpgradeButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public UpgradeButton(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        setTextColor(getResources().getColor(R.color.white));
        setBackgroundDrawable(new ButtonBoxDrawable(context, R.color.pkt_button_upgrade_fill, 0));
    }
}
