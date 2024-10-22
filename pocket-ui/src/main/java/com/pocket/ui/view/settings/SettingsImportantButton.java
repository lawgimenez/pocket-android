package com.pocket.ui.view.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.pocket.ui.R;
import com.pocket.ui.view.themed.ThemedConstraintLayout;

/**
 * This View represents a settings button that contains a large divider and red link text, for use
 * as an "important" settings button (e.g. logging out).
 */
public class SettingsImportantButton extends ThemedConstraintLayout {

    private final Binder binder = new Binder();

    private TextView text;

    public SettingsImportantButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public SettingsImportantButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SettingsImportantButton(Context context) {
        super(context);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_settings_important, this, true);
        text = findViewById(R.id.text);
        engageable.setUiEntityType(Type.BUTTON);
    }

    public Binder bind() {
        return binder;
    }

    public class Binder {

        public Binder clear() {
            text(null);
            return this;
        }

        public Binder text(CharSequence val) {
            text.setText(val);
            return this;
        }

    }

}
