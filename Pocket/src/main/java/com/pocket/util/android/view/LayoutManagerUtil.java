package com.pocket.util.android.view;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Wrapper for accessing common methods like findFirstVisibleItemPosition from a variety of layout managers.
 */
public abstract class LayoutManagerUtil {

    public static int findFirstVisibleItemPosition(RecyclerView view) {
        return findFirstVisibleItemPosition(view.getLayoutManager());
    }

    public static int findFirstVisibleItemPosition(RecyclerView.LayoutManager layout) {
        if (layout == null) {
            return 0;
        }
        if (layout instanceof GridLayoutManager) {
            return ((GridLayoutManager) layout).findFirstVisibleItemPosition();
        } else if (layout instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layout).findFirstVisibleItemPosition();
        } else {
            throw new RuntimeException("unknown layout type " + layout);
        }
    }

    public static int findLastVisibleItemPosition(RecyclerView view) {
        return findLastVisibleItemPosition(view.getLayoutManager());
    }

    public static int findLastVisibleItemPosition(RecyclerView.LayoutManager layout) {
        if (layout instanceof GridLayoutManager) {
            return ((GridLayoutManager) layout).findLastVisibleItemPosition();
        } else if (layout instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layout).findLastVisibleItemPosition();
        } else {
            throw new RuntimeException("unknown layout type " + layout);
        }
    }

}
