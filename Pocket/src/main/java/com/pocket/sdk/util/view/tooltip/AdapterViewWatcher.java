package com.pocket.sdk.util.view.tooltip;

import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;

import com.pocket.util.android.view.ViewTreeObserverHelper;

/**
 * TODO Documentation
 */
public class AdapterViewWatcher {

    private final Object mItem;
    private final AdapterView mAdapterView;
    private final Listener mListener;
    private final ViewTreeObserverHelper mLayoutObserver;

    private View mKnownView;

    public AdapterViewWatcher(Object item, AdapterView adapterView, View view, Listener listener) {
        mItem = item;
        mAdapterView = adapterView;
        mKnownView = view;
        mListener = listener;
        mLayoutObserver = new ViewTreeObserverHelper(mAdapterView, new ViewTreeObserverHelper.Listener() {
            @Override
            public void onGlobalLayout() {
                invalidate();
            }

            @Override
            public void onScrollChanged() {
                invalidate();
            }
        });
    }

    private void invalidate() {
        View newView = findView(mItem, mAdapterView);
        if (newView != mKnownView) {
            mKnownView = newView;
            mListener.onAdapterItemViewChanged(newView);
        }
    }

    public void stop() {
        mLayoutObserver.stop();
    }

    public static View findView(Object item, AdapterView adapterView) {
        Adapter adapter = adapterView.getAdapter();
        int childCount = adapterView.getChildCount();
        int offset = adapterView.getFirstVisiblePosition();

        for (int i = 0; i < childCount; i++) {
            int position = i + offset;
            if (position >= adapter.getCount()) {
                return null;
            } else if (item.equals(adapter.getItem(position))) {
                return adapterView.getChildAt(i);
            }
        }
        return null;
    }

    public interface Listener {
        public void onAdapterItemViewChanged(View view);
    }
}
