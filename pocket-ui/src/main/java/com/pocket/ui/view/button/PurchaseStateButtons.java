package com.pocket.ui.view.button;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.util.NestedColorStateList;

/**
 * A View which manages a set of buttons related to Premium plan purchase.
 *
 * https://www.figma.com/file/FwRIulKGex1BcizU4DuUfIDm/Premium-Page?node-id=843%3A0
 *
 * The View can be in one of 3 states:
 *
 * LOADING - Price data is loading, and the purchase option buttons can not yet be displayed.
 * SHOW_UNKNOWN_PRICE - Price data has encountered an error of some kind.  A single button is displayed.
 * SHOW_PRICES - Price data has loaded, and two buttons are displayed with the plan info.
 *
 * The View can also display an optional circular badge, on the top right corner of the second button.  This badge typically contains a savings percent, e.g. "Save 25%"
 */
public class PurchaseStateButtons extends ConstraintLayout {

    public enum State {
        LOADING, SHOW_UNKNOWN_PRICE, SHOW_PRICES
    }

    private View progress;
    private View optionUnknown;
    private PurchaseButton option1;
    private PurchaseButton option2;
    private TextView badge;

    public PurchaseStateButtons(Context context) {
        super(context);
        init(context);
    }

    public PurchaseStateButtons(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PurchaseStateButtons(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_purchase_state_button, this, true);
        progress = findViewById(R.id.loading_progress);
        optionUnknown = findViewById(R.id.option_unknown);
        option1 = findViewById(R.id.option_1);
        option1.setUiEntityComponentDetail("monthly");
        option2 = findViewById(R.id.option_2);
        option2.setUiEntityComponentDetail("annual");
        badge = findViewById(R.id.badge);
        option1.bind().clear()
                .textColor(getResources().getColorStateList(R.color.pkt_themed_grey_1_clickable))
                .background(new ButtonBoxDrawable(getContext(), R.color.pkt_bg, R.color.pkt_themed_grey_1_clickable));
        option2.bind().clear();
        badge.setBackground(new SaveBadgeDrawable());
    }

    public View optionUnknown() {
        return optionUnknown;
    }

    public PurchaseButton option1() {
        return option1;
    }

    public PurchaseButton option2() {
        return option2;
    }

    public void setBadge(CharSequence badgeText) {
        badge.setText(badgeText);
        updateBadgeVisibility();
    }

    private void updateBadgeVisibility() {
        boolean hasText = !TextUtils.isEmpty(badge.getText());
        boolean option1Visible = option1.getVisibility() == View.VISIBLE;
        boolean option2Visible = option2.getVisibility() == View.VISIBLE;
        if (hasText && option1Visible && option2Visible) {
            badge.setVisibility(View.VISIBLE);
        } else {
            badge.setVisibility(View.INVISIBLE);
        }
        fixBadgeMargins(badge.getVisibility() == View.VISIBLE);
    }

    private void fixBadgeMargins(boolean badgeVisible) {
        Context context = getContext();
        MarginLayoutParams thisParams = (MarginLayoutParams) getLayoutParams();
        if (badgeVisible) {
            int buttonRightMargin = DimenUtil.dpToPxInt(context, 13);
            ((ConstraintLayout.LayoutParams) option2.getLayoutParams()).setMargins(DimenUtil.dpToPxInt(context, 5), DimenUtil.dpToPxInt(context, 27), buttonRightMargin, 0);
            if (thisParams.rightMargin > buttonRightMargin) {
                thisParams.rightMargin = thisParams.rightMargin - buttonRightMargin;
            }
        } else {
            ((ConstraintLayout.LayoutParams) option2.getLayoutParams()).setMargins(DimenUtil.dpToPxInt(context, 5), 0, 0, 0);
            thisParams.rightMargin = context.getResources().getDimensionPixelSize(R.dimen.pkt_side_grid);
        }
        setLayoutParams(thisParams);
    }

    public void setState(State state) {
        switch (state) {
            case SHOW_UNKNOWN_PRICE:
                progress.setVisibility(View.GONE);
                optionUnknown.setVisibility(View.VISIBLE);
                option1.setVisibility(View.INVISIBLE);
                option2.setVisibility(View.INVISIBLE);
                badge.setVisibility(View.INVISIBLE);
                break;
            case SHOW_PRICES:
                progress.setVisibility(View.GONE);
                optionUnknown.setVisibility(View.GONE);
                option1.setVisibility(option1.isSet() ? View.VISIBLE : View.GONE);
                option2.setVisibility(option2.isSet() ? View.VISIBLE : View.GONE);
                updateBadgeVisibility();
                break;
            case LOADING:
            default:
                progress.setVisibility(View.VISIBLE);
                optionUnknown.setVisibility(View.GONE);
                option1.setVisibility(View.INVISIBLE);
                option2.setVisibility(View.INVISIBLE);
                badge.setVisibility(View.INVISIBLE);
        }
    }

    private class SaveBadgeDrawable extends Drawable {

        private final ColorStateList color;
        private final Paint paint = new Paint();

        SaveBadgeDrawable() {
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);
            color = NestedColorStateList.get(getContext(), R.color.pkt_themed_teal_3);
        }

        @Override
        public void draw(Canvas canvas) {
            paint.setColor(color.getColorForState(getState(), Color.TRANSPARENT));
            Rect bounds = getBounds();
            canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), bounds.width() / 2f, paint);
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        protected boolean onStateChange(int[] state) {
            super.onStateChange(state);
            return true;
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            paint.setColorFilter(cf);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
