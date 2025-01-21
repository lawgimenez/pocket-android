package com.pocket.ui.view.progress.skeleton;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.pocket.ui.view.progress.skeleton.row.SkeletonItemRow;
import com.pocket.ui.view.themed.ThemedRecyclerView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SkeletonList extends ThemedRecyclerView {

    public SkeletonList(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SkeletonList(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private static final int LIST_SIZE = 20;

    public void init() {
        setHasFixedSize(true);
        setClipToPadding(false);

        LayoutManager manager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        setLayoutManager(manager);

        setAdapter(new Adapter());
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View v) {
                super(v);
            }
        }

        @NonNull @Override
        public Adapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            final View v = new SkeletonItemRow(getContext());
            v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            // Nothing to do here.
        }

        @Override
        public int getItemCount() {
            return LIST_SIZE;
        }

    }
}
