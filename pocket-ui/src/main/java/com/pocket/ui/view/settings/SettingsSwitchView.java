package com.pocket.ui.view.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.util.EnabledUtil;
import com.pocket.ui.view.menu.ThemedSwitch;
import com.pocket.ui.view.themed.ThemedTextView;
import com.pocket.ui.view.visualmargin.VisualMarginConstraintLayout;
import com.pocket.util.android.ViewUtilKt;

import org.jetbrains.annotations.Nullable;

import static androidx.core.view.ViewKt.isVisible;

/**
 * A "settings" styled View for use as an action button or optionally a toggle switch.
 */
public class SettingsSwitchView extends VisualMarginConstraintLayout {

    private final Binder binder = new Binder();

    private ThemedTextView title;
    private TextView subtitle;
    private ThemedSwitch toggleSwitch;

    public SettingsSwitchView(Context context) {
        super(context);
        init(context, null);
    }

    public SettingsSwitchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SettingsSwitchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        LayoutInflater.from(context).inflate(R.layout.view_settings_switch, this, true);
        title = findViewById(R.id.title);
        subtitle = findViewById(R.id.subtitle);
        toggleSwitch = findViewById(R.id.toggleSwitch);

        setOnClickListener(v -> toggleSwitch.toggle());

        setMinimumHeight(DimenUtil.dpToPxInt(context, 72));

        subtitle.setVisibility(View.GONE);

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SettingsSwitchView);
            title.setText(ta.getText(R.styleable.SettingsSwitchView_android_title));
            CharSequence sub = ta.getText(R.styleable.SettingsSwitchView_android_text);
            if (sub != null && sub.length() > 0) {
                subtitle.setText(ta.getText(R.styleable.SettingsSwitchView_android_text));
                subtitle.setVisibility(View.VISIBLE);
            }

            setEnabled(ta.getBoolean(R.styleable.SettingsSwitchView_android_enabled, true));

            binder.isToggle(ta.getBoolean(R.styleable.SettingsSwitchView_isToggle, true));

            ta.recycle();
        } else {
            setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            binder.clear();
        }

        setBackground(getResources().getDrawable(R.drawable.cl_pkt_touchable_area));

        engageable.setUiEntityType(Type.BUTTON);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        EnabledUtil.setChildrenEnabled(SettingsSwitchView.this, enabled, false);
    }

    @Nullable @Override public String getUiEntityLabel() {
        return title.getUiEntityLabel();
    }

    @Nullable @Override public String getUiEntityValue() {
        return isVisible(toggleSwitch) ? toggleSwitch.getUiEntityValue() : null;
    }

    @Nullable @Override public String getEngagementValue() {
        return isVisible(toggleSwitch) ? toggleSwitch.getEngagementValue() : null;
    }

    public void setChecked(boolean checked) {
        toggleSwitch.setChecked(checked);
    }

    public boolean isChecked() {
        return toggleSwitch.isChecked();
    }

    public void toggle() {
        toggleSwitch.toggle();
    }

    public Binder bind() {
        return binder;
    }

    public class Binder {

        public Binder clear() {
            isToggle(true);
            title(null);
            subtitle(null);
            checked(false);
            enabled(true);
            onCheckedListener(null);
            return this;
        }

        public Binder isToggle(boolean val) {
            if (!val) {
                toggleSwitch.setVisibility(View.INVISIBLE);
            } else {
                toggleSwitch.setVisibility(View.VISIBLE);
            }
            return this;
        }

        public Binder title(@Nullable CharSequence val) {
            title.setTextAndUpdateEnUsLabel(val, val != null ? val.toString() : null);
            return this;
        }

        public Binder subtitle(CharSequence val) {
            ViewUtilKt.setTextOrHide(subtitle, val);
            return this;
        }

        public Binder checked(boolean val) {
            setChecked(val);
            return this;
        }

        public Binder enabled(boolean val) {
            setEnabled(val);
            return this;
        }

        public Binder onCheckedListener(CompoundButton.OnCheckedChangeListener listener) {
            toggleSwitch.setOnCheckedChangeListener(listener);
            return this;
        }

        public Binder onClickListener(OnClickListener listener) {
            setOnClickListener(listener);
            return this;
        }

    }

}
