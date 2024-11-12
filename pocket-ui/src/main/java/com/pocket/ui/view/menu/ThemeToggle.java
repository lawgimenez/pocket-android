package com.pocket.ui.view.menu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.IdRes;

import com.pocket.ui.R;
import com.pocket.ui.view.themed.ThemedLinearLayout;

/**
 * A picker for for app theme choices
 */
public class ThemeToggle extends ThemedLinearLayout {

    public enum ThemeChoice {

        LIGHT(R.id.theme_light),
        DARK(R.id.theme_dark),
        AUTO(R.id.theme_auto);

        final int resId;

        ThemeChoice(@IdRes int resId) {
            this.resId = resId;
        }
    }

    private final Binder binder = new Binder();

    private OnThemeSelectedListener onThemeSelectedListener;
    private SparseArray<View> themeChoiceViews;
    private Drawable selector;
    private View currentSelection;

    public ThemeToggle(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThemeToggle(Context context) {
        super(context);
        init();
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {

        boolean val = super.drawChild(canvas, child, drawingTime);

        if (child == currentSelection) {
            selector.setBounds(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
            selector.draw(canvas);
            selector.setState(child.getDrawableState());
        }

        return val;
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_theme_toggle, this, true);

        setOrientation(HORIZONTAL);

        selector = new ThemeToggleSelectionDrawable(getContext());

        themeChoiceViews = new SparseArray<>();

        for (ThemeChoice choice : ThemeChoice.values()) {
            View v = findViewById(choice.resId);
            v.setOnClickListener(v1 -> {
                if (onThemeSelectedListener != null) {
                    onThemeSelectedListener.onThemeSelected(v1, choice);
                }
                setCurrentSelection(v1);
            });
            themeChoiceViews.put(choice.resId, v);
        }

        bind().clear();
    }

    private void setCurrentSelection(View v1) {
        currentSelection = v1;
        invalidate();
    }

    public Binder bind() {
        return binder;
    }

    public class Binder {

        public Binder clear() {
            listener(null);
            availableThemes(ThemeChoice.values());
            theme(null);
            setCurrentSelection(themeChoiceViews.valueAt(0));
            return this;
        }

        public Binder availableThemes(ThemeChoice... choices) {
            // clear all views
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).setVisibility(View.GONE);
            }
            // set available views visible
            for (ThemeChoice choice : choices) {
                themeChoiceViews.get(choice.resId).setVisibility(View.VISIBLE);
            }
            return this;
        }

        public Binder theme(ThemeChoice value) {
            if (value != null) {
                setCurrentSelection(themeChoiceViews.get(value.resId));
            }
            return this;
        }

        public Binder listener(OnThemeSelectedListener listener) {
            onThemeSelectedListener = listener;
            return this;
        }

    }

    public interface OnThemeSelectedListener {
        void onThemeSelected(View view, ThemeChoice value);
    }

}
