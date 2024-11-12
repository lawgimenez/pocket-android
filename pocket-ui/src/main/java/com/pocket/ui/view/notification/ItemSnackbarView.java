package com.pocket.ui.view.notification;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.util.LazyBitmap;
import com.pocket.ui.util.LazyBitmapDrawable;
import com.pocket.ui.util.NestedColorStateList;
import com.pocket.ui.view.button.ButtonBoxDrawable;
import com.pocket.ui.view.item.ItemMetaView;
import com.pocket.ui.view.item.ItemThumbnailView;

public class ItemSnackbarView extends CoordinatorLayout {

    private CardView card;
    private ItemThumbnailView thumbnail;
    private ImageView miniIcon;
    private TextView featureTitle;
    private ItemMetaView meta;

    private PktSwipeDismissBehavior dismissBehavior;

    private Binder binder = new Binder();

    public ItemSnackbarView(Context context) {
        super(context);
        init();
    }

    public ItemSnackbarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ItemSnackbarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_item_snackbar, this, true);
        final int cardRadius = DimenUtil.dpToPxInt(getContext(), 4);

        findViewById(R.id.root_view).setBackground(new ButtonBoxDrawable(getContext(), R.color.pkt_opaque_touchable_area, R.color.pkt_item_snackbar_stroke, cardRadius));

        card = findViewById(R.id.card);
        thumbnail = findViewById(R.id.item_thumbnail);
        miniIcon = findViewById(R.id.icon_mini);
        featureTitle = findViewById(R.id.feature_title);
        meta = findViewById(R.id.item_meta);

        card.setUseCompatPadding(true);
        card.setRadius(cardRadius);
        card.setCardElevation(DimenUtil.dpToPxInt(getContext(), 4));
        card.setCardBackgroundColor(NestedColorStateList.get(getContext(), R.color.pkt_bg));

        dismissBehavior = new PktSwipeDismissBehavior();
        dismissBehavior.setSwipeDirection(PktSwipeDismissBehavior.SWIPE_DIRECTION_ANY);

        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) card.getLayoutParams();
        layoutParams.setBehavior(dismissBehavior);

        setClipToPadding(false);
    }

    public Binder bind() {
        return binder;
    }

    public class Binder {

        public Binder clear() {
            thumbnail(null);
            icon(0);
            featureTitle(null);
            onClick(null);
            onDismiss(null);
            meta().clear()
                    .titleMaxLines(1);
            return this;
        }

        public Binder thumbnail(LazyBitmap value) {
            thumbnail.setImageDrawable(value != null ? new LazyBitmapDrawable(value) : null);
            return this;
        }

        public Binder icon(@DrawableRes int iconRes) {
            miniIcon.setImageResource(iconRes);
            miniIcon.setVisibility(iconRes == 0 ? View.GONE : View.VISIBLE);
            return this;
        }

        public Binder featureTitle(CharSequence title) {
            featureTitle.setText(title);
            return this;
        }

        public Binder onClick(OnClickListener listener) {
            card.setOnClickListener(listener);
            return this;
        }

        public Binder onDismiss(PktSwipeDismissBehavior.OnDismissListener listener) {
            dismissBehavior.setListener(listener);
            return this;
        }

        public ItemMetaView.Binder meta() {
            return meta.bind();
        }
    }

}
