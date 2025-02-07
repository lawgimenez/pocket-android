package com.pocket.ui.view.item;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.pocket.ui.R;
import com.pocket.ui.util.EnabledUtil;
import com.pocket.ui.view.themed.ThemedTextView;
import com.pocket.ui.view.visualmargin.VisualMarginConstraintLayout;

/**
 * Displays various aspects of an Item's meta data:
 * <ul>
 *     <li>Title</li>
 *     <li>Domain</li>
 *     <li>Time Estimate</li>
 *     <li>Indicator (optional)</li>
 *     <li>Excerpt (optional, and can control max lines)</li>
 *     <li>Badges (Groups, Favorite and Tags) (optional)</li>
 *     <li>Shared By (optional)</li>
 * </ul>
 */
public class ItemMetaView extends VisualMarginConstraintLayout {
	
	private Binder binder = new Binder();
	private ThemedTextView title;
	private TextView domain;
	private TextView time;
	private ImageView indicator;

	public ItemMetaView(Context context) {
		super(context);
		init();
	}
	
	public ItemMetaView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public ItemMetaView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}
	
	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.view_item_meta, this, true);
		title = findViewById(R.id.title);
		domain = findViewById(R.id.domain);
		time = findViewById(R.id.time_estimate);
		indicator = findViewById(R.id.indicator);
		title.setEllipsize(TextUtils.TruncateAt.END);
		
		bind().clear();
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		EnabledUtil.setChildrenEnabled(this, enabled, true);
	}
	
	public Binder bind() {
		return binder;
	}
	
	public class Binder {
		
		public Binder clear() {
			title(null);
			titleMaxLines(10);
			domain(null);
			timeEstimate(null);
			indicator(null);
			return this;
		}
		
		public Binder title(CharSequence value) {
			title.setText(value);
			return this;
		}

		public Binder titleMaxLines(int maxLines) {
			title.setMaxLines(maxLines);
			return this;
		}
		
		public Binder domain(CharSequence value) {
			domain.setText(value);
			return this;
		}
		
		public Binder timeEstimate(CharSequence value) {
			if (TextUtils.isEmpty(value)) {
				time.setText(null);
				time.setVisibility(GONE);
			} else {
				time.setText(TextUtils.concat(" · ", value));
				time.setVisibility(VISIBLE);
			}
			return this;
		}

		public Binder indicator(Drawable drawable) {
			indicator.setImageDrawable(drawable);
			indicator.setVisibility(drawable != null ? View.VISIBLE : View.GONE);
			return this;
		}
	}
}
