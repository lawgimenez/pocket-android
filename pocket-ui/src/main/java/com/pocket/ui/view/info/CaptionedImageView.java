package com.pocket.ui.view.info;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pocket.ui.R;
import com.pocket.ui.view.visualmargin.VisualMarginConstraintLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/**
 * A commonly used mock up of image, title and short text used in a lot of onboarding and intro like ui.
 * <p>
 * This view is meant to have either a fixed width or a MATCH_PARENT width. It doesn't have an intrinsic width and won't work with WRAP_CONTENT.
 */
public class CaptionedImageView extends VisualMarginConstraintLayout {

    private final Binder binder = new Binder();

    private ImageView image;
    private ViewGroup captionWrap;
    private TextView title;
    private TextView text;

    public CaptionedImageView(Context context) {
        super(context);
        init();
    }

    public CaptionedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CaptionedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_captioned_image, this, true);
        captionWrap = findViewById(R.id.caption_wrap);
        image = findViewById(R.id.image);
        title = findViewById(R.id.title);
        text = findViewById(R.id.text);
    }

    public Binder bind() {
        return binder;
    }

    public class Binder {

        public Binder clear() {
            image(null);
            title(null);
            text(null);
            captionHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            return this;
        }

        public Binder image(Drawable drawable) {
            image.setImageDrawable(drawable);
            return this;
        }

        public Binder image(@DrawableRes int imageId) {
            image.setImageResource(imageId);
            return this;
        }

        public Binder title(@StringRes int resId) {
            title.setText(resId);
            return this;
        }

        public Binder text(@StringRes int resId) {
            text.setText(resId);
            return this;
        }

        public Binder title(CharSequence val) {
            title.setText(val);
            return this;
        }

        public Binder text(CharSequence val) {
            text.setText(val);
            return this;
        }

        /**
         * This view is often used within a horizontal pager, which requires setting a consistent height
         * for all of the views based on the tallest item.  This method allows setting the absolute
         * height of the title and text views.  These views will align to the top if any excess space
         * exists.
         */
        public Binder captionHeight(int height) {
            ViewGroup.LayoutParams params = captionWrap.getLayoutParams();
            params.height = height;
            captionWrap.setLayoutParams(params);
            return this;
        }
    }

}
