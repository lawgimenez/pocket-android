package com.pocket.ui.view.button;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.pocket.ui.R;
import com.pocket.ui.text.TextViewUtil;
import com.pocket.ui.view.visualmargin.VisualMarginConstraintLayout;

/**
 * A View which is visually similar to {@link BoxButton}, except that it contains two lines of text, typically
 * used in the purchase screen to display a plan name and price.
 *
 * https://www.figma.com/file/FwRIulKGex1BcizU4DuUfIDm/Premium-Page?node-id=843%3A0
 *
 */
public class PurchaseButton extends VisualMarginConstraintLayout {

    private final Binder binder = new Binder();

    private TextView text;
    private TextView subtext;

    public PurchaseButton(Context context) {
        super(context);
        init(null);
    }

    public PurchaseButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public PurchaseButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        LayoutInflater.from(getContext()).inflate(R.layout.view_purchase_button, this, true);
        setClickable(true);
        text = findViewById(R.id.text);
        subtext = findViewById(R.id.subtext);

        TextViewUtil.setVisualTextPadding(text, R.dimen.pkt_space_sm, R.dimen.pkt_space_md);
        TextViewUtil.setVisualTextPadding(subtext, R.dimen.pkt_space_sm, R.dimen.pkt_space_md);
        text.setPadding(text.getPaddingLeft(), text.getPaddingTop(), text.getPaddingRight(), 0);
        subtext.setPadding(subtext.getPaddingLeft(), 0, subtext.getPaddingRight(), subtext.getPaddingBottom());

        engageable.obtainStyledAttributes(getContext(), attrs);
        engageable.setUiEntityType(Type.BUTTON);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        PktViewsKt.updateEnabledAlpha(this);
    }

    /**
     * @return boolean, whether both lines of text have been set
     */
    public boolean isSet() {
        return !TextUtils.isEmpty(text.getText()) && !TextUtils.isEmpty(subtext.getText());
    }

    public Binder bind() {
        return binder;
    }

    public class Binder {

        public Binder clear() {
            text(0);
            subtext(null);
            background(new ButtonBoxDrawable(getContext(), R.color.pkt_button_box_coral_fill, 0));
            textColor(getResources().getColorStateList(R.color.pkt_button_text));
            return this;
        }

        public Binder text(@StringRes int resId) {
            if (resId != 0) {
                text.setText(resId);
                engageable.updateEnUsLabel(getContext(), resId);
            } else {
                text.setText(null);
                engageable.updateEnUsLabel(null);
            }
            return this;
        }

        public Binder subtext(CharSequence s) {
            subtext.setText(s);
            return this;
        }

        public Binder background(Drawable background) {
            setBackground(background);
            return this;
        }

        public Binder textColor(ColorStateList colors) {
            text.setTextColor(colors);
            subtext.setTextColor(colors);
            return this;
        }

        public Binder onClick(OnClickListener listener) {
            setOnClickListener(listener);
            return this;
        }
    }
}
