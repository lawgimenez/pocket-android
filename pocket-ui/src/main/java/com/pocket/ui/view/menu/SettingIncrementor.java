package com.pocket.ui.view.menu;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.pocket.ui.R;
import com.pocket.ui.view.themed.ThemedConstraintLayout;
import com.squareup.phrase.Phrase;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

public class SettingIncrementor extends ThemedConstraintLayout {

    private final Binder binder = new Binder();

    private View up;
    private View down;
    private ImageView icon;

    public SettingIncrementor(Context context) {
        super(context);
        init();
    }

    public SettingIncrementor(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SettingIncrementor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_setting_incrementor, this, true);
        up = findViewById(R.id.setting_up);
        down = findViewById(R.id.setting_down);
        icon = findViewById(R.id.setting_icon);

        bind().clear();
    }

    public Binder bind() {
        return binder;
    }

    public class Binder {

        public Binder clear() {
            upListener(null);
            downListener(null);
            icon(0);
            upEnabled(true);
            downEnabled(true);
            label(0);
            return this;
        }

        public Binder upListener(OnClickListener listener) {
            up.setOnClickListener(listener);
            return this;
        }

        public Binder downListener(OnClickListener listener) {
            down.setOnClickListener(listener);
            return this;
        }

        public Binder icon(@DrawableRes int drawableRes) {
            icon.setImageResource(drawableRes);
            return this;
        }

        public Binder upEnabled(boolean enabled) {
            up.setEnabled(enabled);
            return this;
        }

        public Binder downEnabled(boolean enabled) {
            down.setEnabled(enabled);
            return this;
        }

        public Binder label(@StringRes int settingName) {
            if (settingName != 0) {
                CharSequence setting = getResources().getString(settingName);
                up.setContentDescription(Phrase.from(getResources(), R.string.setting_incrementor)
                        .put("setting", setting)
                        .format());
                down.setContentDescription(Phrase.from(getResources(), R.string.setting_decrementor)
                        .put("setting", setting)
                        .format());
            } else {
                up.setContentDescription(null);
                down.setContentDescription(null);
            }
            return this;
        }
    }

}
