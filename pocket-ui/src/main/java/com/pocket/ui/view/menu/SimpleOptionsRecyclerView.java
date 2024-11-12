package com.pocket.ui.view.menu;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pocket.ui.R;
import com.pocket.ui.view.themed.ThemedRecyclerView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A simple options list which takes a String[] of options and a list item click listener, intended for use in DialogViews.
 */
public class SimpleOptionsRecyclerView extends ThemedRecyclerView {

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    private OnItemClickListener listener;

    public SimpleOptionsRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public SimpleOptionsRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SimpleOptionsRecyclerView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setBackgroundResource(R.drawable.cl_pkt_bg);
        setLayoutManager(new LinearLayoutManager(getContext()));
        // hide the top divider
        setPadding(0, -(int) getContext().getResources().getDimension(R.dimen.pkt_thin_divider_height), 0, 0);
    }

    public void setOptions(String[] options) {
        setAdapter(options == null ? null : new Adapter(options));
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        private String[] data;

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final TextView textView;
            public ViewHolder(View v) {
                super(v);
                textView = v.findViewById(android.R.id.text1);
            }
        }

        public Adapter(String[] data) {
            this.data = data;
        }

        @Override
        public Adapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.view_pkt_simple_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.textView.setText(data[position]);
            holder.textView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(holder.textView, position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.length;
        }
    }

}
