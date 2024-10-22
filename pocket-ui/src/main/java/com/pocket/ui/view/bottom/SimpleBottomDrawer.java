package com.pocket.ui.view.bottom;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.pocket.ui.R;

import java.util.ArrayList;
import java.util.List;

public class SimpleBottomDrawer extends BottomDrawer {

    private final Binder binder = new Binder();

    // keep a list of views/title text so we can add them on inflation and not have to break lazy loading
    private List<View> views = new ArrayList<>();
    private CharSequence title;

    private LinearLayout list;

    public SimpleBottomDrawer(Context context) {
        super(context);
    }

    public SimpleBottomDrawer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SimpleBottomDrawer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLazyInflated() {
        super.onLazyInflated();
        setLayout(R.layout.view_drawer_simple);
        list = findViewById(R.id.list);

        // clip off the bottommost divider
        MarginLayoutParams params = (MarginLayoutParams) list.getLayoutParams();
        params.setMargins(0, 0, 0, -getResources().getDimensionPixelSize(R.dimen.pkt_thin_divider_height));
        list.setLayoutParams(params);

        setScrimAlpha(0, 0.5f, 0.5f);
        getBehavior().setSkipCollapsed(true);
        getBehavior().setHideable(true);
        setHideOnOutsideTouch(true);

        bind().title(title);
        for (View v : views) {
            bind().addView(v);
        }
        views.clear();
    }

    public int getDrawerSize() {
        if (list != null) {
            return list.getChildCount();
        } else {
            return views.size();
        }
    }

    public View getDrawerView(int index) {
        if (list != null) {
            return list.getChildAt(index);
        } else {
            return views.get(index);
        }
    }

    public int getDrawerIndex(View view) {
        if (list != null) {
            return list.indexOfChild(view);
        } else {
            return views.indexOf(view);
        }
    }

    public ViewGroup getList() {
        return list;
    }

    public Binder bind() {
        return binder;
    }

    public class Binder {

        public Binder clear() {
            title(null);
            views.clear();
            if (list != null) {
                list.removeAllViews();
            }
            return this;
        }

        public Binder title(CharSequence title) {
            if (getTitle() != null) {
                getTitle().setText(title);
                getTitle().setVisibility(TextUtils.isEmpty(title) ? View.GONE : View.VISIBLE);
            } else {
                SimpleBottomDrawer.this.title = title;
            }
            return this;
        }

        public Binder addView(View v) {
            if (list != null) {
                list.addView(v);
            } else {
                views.add(v);
            }
            return this;
        }

        public Binder addTextOptionRow(
                @DrawableRes int icon,
                @StringRes int text,
                OnClickListener listener
        ) {
            SimpleDrawerRow view = new SimpleDrawerRow(getContext());
            view.bind()
                    .icon(icon)
                    .text(text)
                    .onClick(v -> {
                        listener.onClick(v);
                        hide();
                    });
            addView(view);
            return this;
        }

        public Binder addTextOptionRow(
                @DrawableRes int icon,
                @StringRes int text,
                String uiEntityIdentifier,
                OnClickListener listener
        ) {
            SimpleDrawerRow view = new SimpleDrawerRow(getContext());
            view.bind()
                    .icon(icon)
                    .text(text)
                    .uiEntityIdentifier(uiEntityIdentifier)
                    .onClick(v -> {
                        listener.onClick(v);
                        hide();
                    });
            addView(view);
            return this;
        }

        /**
         * @return the newly created SimpleDrawerRow
         */
        public SimpleDrawerRow addTextOptionRowSeparate(
                @DrawableRes int icon,
                @StringRes int text,
                String uiEntityIdentifier,
                OnClickListener listener
        ) {
            SimpleDrawerRow view = new SimpleDrawerRow(getContext());
            view.bind()
                    .icon(icon)
                    .text(text)
                    .uiEntityIdentifier(uiEntityIdentifier)
                    .onClick(v -> {
                        listener.onClick(v);
                        hide();
                    });
            addView(view);
            return view;
        }
    }
}
