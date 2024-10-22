package com.pocket.ui.view.progress;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.pocket.ui.R;
import com.pocket.ui.view.themed.ThemedConstraintLayout;
import com.pocket.util.android.ViewUtilKt;

/**
 * A fullscreen tinted view which blocks user interactions with an optional {@link RainbowProgressCircleView} and/or message.
 */
public class FullscreenProgressView extends ThemedConstraintLayout {

    private final Binder binder = new Binder();

    private View progressCircle;
    private TextView messageView;

    public FullscreenProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FullscreenProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public FullscreenProgressView(Context context) {
        super(context);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_fullscreen_progress, this, true);
        setClickable(true);
        setFocusable(true);
        setBackgroundColor(Color.parseColor("#88000000"));
        progressCircle = findViewById(R.id.progress_circle);
        messageView = findViewById(R.id.message);
        bind().clear();
    }

    public Binder bind() {
        return binder;
    }

    public class Binder {

        public Binder clear() {
            progressCircle(true);
            message(null);
            visible(false);
            return this;
        }

        public Binder progressCircle(boolean show) {
            progressCircle.setVisibility(show ? View.VISIBLE : View.GONE);
            return this;
        }

        public Binder message(CharSequence message) {
            ViewUtilKt.setTextOrHide(messageView, message);
            return this;
        }

        public Binder visible(boolean show) {
            setVisibility(show ? View.VISIBLE : View.GONE);
            return this;
        }
    }
}
