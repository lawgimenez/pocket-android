package com.pocket.ui.view.progress.skeleton;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.pocket.ui.R;
import com.pocket.ui.view.progress.skeleton.row.SkeletonActivityRow;
import com.pocket.ui.view.progress.skeleton.row.SkeletonDiscoverRow;
import com.pocket.ui.view.progress.skeleton.row.SkeletonItemAvatarRow;
import com.pocket.ui.view.progress.skeleton.row.SkeletonItemGridRow;
import com.pocket.ui.view.progress.skeleton.row.SkeletonItemRecsTileRow;
import com.pocket.ui.view.progress.skeleton.row.SkeletonItemRow;
import com.pocket.ui.view.progress.skeleton.row.SkeletonItemTileRow;
import com.pocket.ui.view.progress.skeleton.row.SkeletonSingleLine;
import com.pocket.ui.view.themed.ThemedRecyclerView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SkeletonList extends ThemedRecyclerView {

    public enum Type {
        LIST_ITEM, LIST_ITEM_GRID, LIST_ITEM_TILE, LIST_ITEM_RECS_TILE, LIST_ACTIVITY, LIST_AVATAR, SINGLE_LINE, DISCOVER
    }

    public SkeletonList(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(initAttributes(context, attrs), 0, null);
    }

    public SkeletonList(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(initAttributes(context, attrs), 0, null);
    }

    public SkeletonList(Context context, Type type, ItemDecoration decor) {
        super(context);
        init(type, 0, decor);
    }

    private static final int LIST_SIZE_DEFAULT = 20;

    private int listSize = LIST_SIZE_DEFAULT;

    private Type initAttributes(Context context, AttributeSet attrs) {
        final Type type;
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SkeletonList);
            listSize = ta.getInt(R.styleable.SkeletonList_size, LIST_SIZE_DEFAULT);
            type = Type.values()[ta.getInt(R.styleable.SkeletonList_type, 0)];
            ta.recycle();
        } else {
            type = Type.LIST_ITEM;
        }
        return type;
    }

    public void init(Type type, int spans, ItemDecoration decor) {
        setHasFixedSize(true);
        setClipToPadding(false);

        LayoutManager manager = type == Type.LIST_ITEM_GRID ? new GridLayoutManager(getContext(), spans) : new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        setLayoutManager(manager);

        if (manager instanceof GridLayoutManager) {
            ((GridLayoutManager) manager).setSpanCount(spans);
        }

        if (decor != null) {
            addItemDecoration(decor);
        }

        setAdapter(new Adapter(type));
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        private Type type;

        private Adapter(Type type) {
            this.type = type;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View v) {
                super(v);
            }
        }

        @Override
        public Adapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            final View v;

            switch (type) {
                case LIST_ITEM_GRID:
                    v = new SkeletonItemGridRow(getContext());
                    break;
                case LIST_ACTIVITY:
                    v = new SkeletonActivityRow(getContext());
                    break;
                case LIST_ITEM_TILE:
                    v = new SkeletonItemTileRow(getContext());
                    break;
                case LIST_ITEM_RECS_TILE:
                    v = new SkeletonItemRecsTileRow(getContext());
                    break;
                case LIST_AVATAR:
                    v = new SkeletonItemAvatarRow(getContext());
                    break;
                case SINGLE_LINE:
                    v = new SkeletonSingleLine(getContext());
                    break;
                case DISCOVER:
                    v = new SkeletonDiscoverRow(getContext());
                    break;
                default:
                case LIST_ITEM:
                    v = new SkeletonItemRow(getContext());
                    break;
            }

            v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            //
        }

        @Override
        public int getItemCount() {
            return listSize;
        }

    }
}
