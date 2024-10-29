package com.pocket.ui.view.highlight;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.pocket.ui.R;
import com.pocket.ui.view.visualmargin.VisualMargin;
import com.pocket.ui.view.visualmargin.VisualMarginConstraintLayout;

public class HighlightView extends VisualMarginConstraintLayout implements VisualMargin {
	
	private final Binder binder = new Binder(this);
	private HighlightTextView highlight;
	private View share;
	private View delete;
	private View divider;
	
	public HighlightView(Context context) {
		super(context);
		init();
	}
	
	public HighlightView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public HighlightView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}
	
	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.view_highlight, this, true);
		
		highlight = findViewById(R.id.highlight);
		delete = findViewById(R.id.delete);
		share = findViewById(R.id.share_highlight);
		divider = findViewById(R.id.divider);
		
		bind().clear();
	}
	
	public Binder bind() {
		return binder;
	}
	
	public static class Binder {
		
		private final HighlightView view;
		
		private Binder(HighlightView view) {
			this.view = view;
		}
		
		public Binder clear() {
			highlight(null);
			onDeleteHighlightClick(null);
			onShareHighlightClick(null);
			showDivider(false);
			return  this;
		}
		
		public Binder highlight(CharSequence text) {
			view.highlight.setText(text);
			return this;
		}
		
		public Binder showDivider(boolean value) {
			view.divider.setVisibility(value ? VISIBLE : GONE);
			return this;
		}
		
		public Binder onDeleteHighlightClick(OnDeleteHighlightClickListener listener) {
			view.delete.setOnClickListener(listener != null ? v -> listener.onDeleteHighlightClick(view) : null);
			return this;
		}
		
		public interface OnDeleteHighlightClickListener {
			void onDeleteHighlightClick(HighlightView view);
		}
		
		public Binder onShareHighlightClick(OnShareHighlightClickListener listener) {
			view.share.setOnClickListener(listener != null ? v -> listener.onShareHighlightClick(view) : null);
			return this;
		}
		
		public interface OnShareHighlightClickListener {
			void onShareHighlightClick(HighlightView view);
		}
		
	}
	
	@Override
	public boolean prepareVisualAscent() {
		return VisualMargin.removeTopMargin(highlight);
	}
	
	@Override
	public int visualDescent() {
		return share.getPaddingBottom();
	}
	
}
