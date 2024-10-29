package com.pocket.ui.view.menu;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.pocket.ui.R;
import com.pocket.ui.view.visualmargin.VisualMarginConstraintLayout;

public class UpgradeRowView extends VisualMarginConstraintLayout {

    private final Binder binder = new Binder();

    private TextView text;
    private TextView button;

    public UpgradeRowView(Context context) {
        super(context);
        init();
    }

    public UpgradeRowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public UpgradeRowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_upgrade_row, this, true);
        text = findViewById(R.id.text);
        button = findViewById(R.id.button);
        setOnTouchListener((v, event) -> true); // prevent touches from triggering icon touch state
        setClickable(true);
        setFocusable(true);
    }

    public Binder bind() {
        return binder;
    }

    public class Binder {

        public Binder clear() {
            text(null);
            button(null, null);
            return this;
        }

        public Binder text(CharSequence c) {
            text.setText(c);
            return this;
        }

        public Binder button(CharSequence c, OnClickListener l) {
            button.setText(c);
            button.setOnClickListener(l);
            return this;
        }

    }
}
