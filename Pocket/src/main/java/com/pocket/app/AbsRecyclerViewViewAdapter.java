package com.pocket.app;

import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A {@link RecyclerView.Adapter} that also supports adding arbitrary Views, useful for adding headers, footers, buttons, etc.
 *
 * To use, wrap your data types in the {@link Row} class and implement onCreateViewHolder(@NonNull ViewGroup parent, int viewType),
 * being sure to call the superclass implementation in the event that your own ViewHolders are not applicable.
 */
public abstract class AbsRecyclerViewViewAdapter extends RecyclerView.Adapter<AbsRecyclerViewViewAdapter.ViewHolder> {

    private SparseArray<View> viewMap = new SparseArray<>();
    private List<Row> data = new ArrayList<>();

    /**
     * Generates a unique view type for a View added to the adapter.  This value is based on a negative Integer.MAX_VALUE
     * to avoid colliding with the generally used method of incrementing view types for custom ViewHolders.
     */
    private static int getViewViewType(int viewId) {
        return -Integer.MAX_VALUE + viewId;
    }

    public static abstract class Row {
        public abstract int getViewType();
    }

    private static class ViewRow extends Row {

        final View view;

        ViewRow(View v) {
            view = v;
        }

        @Override
        public int getViewType() {
            return getViewViewType(view.getId()); // ensures this ViewHolder is not recycled
        }
    }

    public abstract class ViewHolder<T extends Row> extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
        public abstract void bind(T row);
    }

    private class ViewHolderView extends ViewHolder<ViewRow> {
        private ViewHolderView(View view) {
            super(view);
        }
        @Override
        public void bind(ViewRow row) {
            // nothing to do here
        }
    }

    @NonNull
    @CallSuper
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolderView(viewMap.get(viewType));
    }

    @Override
    public final void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(data.get(position));
    }

    @Override
    public final int getItemCount() {
        return data.size();
    }

    @Override
    public final int getItemViewType(int position) {
        return data.get(position).getViewType();
    }

    public void addItem(int index, Row row) {
        data.add(index, row);
        notifyItemInserted(index);
    }

    public void addItems(int index, List<Row> items) {
        data.addAll(index, items);
        notifyItemRangeInserted(index, items.size());
    }

    public void removeItems(int positionStart, int itemCount)  {
        data.subList(positionStart, positionStart + itemCount).clear();
        notifyItemRangeRemoved(positionStart, itemCount);
    }

    public void addView(View v) {
        addView(data.size(), v);
    }

    public void addView(int index, View v) {
        if (v.getId() == -1) {
            v.setId(View.generateViewId());
        }
        viewMap.put(getViewViewType(v.getId()), v);
        addItem(index, new ViewRow(v));
    }
}
