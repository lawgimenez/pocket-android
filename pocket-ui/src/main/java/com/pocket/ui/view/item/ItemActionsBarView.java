package com.pocket.ui.view.item;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.pocket.ui.R;
import com.pocket.ui.util.EmptiableView;
import com.pocket.ui.util.EmptiableViewHelper;
import com.pocket.ui.util.EnabledUtil;
import com.pocket.ui.view.button.CountIconButton;
import com.pocket.ui.view.visualmargin.VisualMarginConstraintLayout;

/**
 * Displays various actions available on an Item, all can be optionally shown.
 * <ul>
 *     <li>Save</li>
 *     <li>Social Actions (Like and Repost)</li>
 *     <li>Menu</li>
 * </ul>
 *
 * The save and menu buttons have default side paddings that align the edges of their icons with {@link R.dimen#pkt_side_grid}
 * and still allow their touch areas to extend beyond those guidelines.  If your parent view has a different grid alignment
 * you can use the {@link R.attr#itemActionBarSidePadding} attr to override it.
 */
public class ItemActionsBarView extends VisualMarginConstraintLayout implements EmptiableView {
	
	private final Binder binder = new Binder(this);
	private final EmptiableViewHelper emptiableViewHelper = new EmptiableViewHelper(this, EmptiableViewHelper.GONE_WHEN_EMPTY);
	
	private SaveButton save;
	private View menu;
	private CountIconButton like;
	private CountIconButton repost;
	
	public ItemActionsBarView(Context context) {
		super(context);
		init(null);
	}
	
	public ItemActionsBarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}
	
	public ItemActionsBarView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
	}
	
	private void init(AttributeSet attrs) {
		LayoutInflater.from(getContext()).inflate(R.layout.view_item_actions_bar, this, true);
		save = findViewById(R.id.save);
		menu = findViewById(R.id.menu);
		like = findViewById(R.id.like);
		repost = findViewById(R.id.repost);
		
		if (attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ItemActionsBarView);
			int padding = a.getDimensionPixelSize(R.styleable.ItemActionsBarView_itemActionBarSidePadding, -1);
			if (padding >= 0) {
				save.setPadding(padding, save.getPaddingTop(), save.getPaddingRight(),  save.getPaddingBottom());
				menu.setPadding(menu.getPaddingLeft(), menu.getPaddingTop(), padding,  menu.getPaddingBottom());
			}
			a.recycle();
		}

		repost.setCheckable(false);
		
		bind().clear();
	}
	
	public Binder bind() {
		return binder;
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		EnabledUtil.setChildrenEnabled(this, enabled, true);
	}
	
	@Override
	public void setOnEmptyChangedListener(OnEmptyChangedListener listener) {
		emptiableViewHelper.setOnEmptyChangedListener(listener);
	}
	
	public static class Binder {
		
		private final ItemActionsBarView view;
		
		private Binder(ItemActionsBarView itemView) {
			this.view = itemView;
		}
		
		public Binder clear() {
			onMenuClick(null);
			
			save().clear();
			saveVisible(false);
			
			like().clear();
			repost().clear();
			socialVisible(false);
			return this;
		}
		
		public Binder onMenuClick(OnClickListener listener) {
			view.menu.setOnClickListener(listener);
			view.menu.setVisibility(listener != null ? View.VISIBLE : View.GONE);
			invalidateEmpty();
			return this;
		}
		
		public SaveButton.Binder save() {
			return view.save.bind();
		}
		
		public Binder socialVisible(boolean visible) {
			view.repost.setVisibility(visible ? View.VISIBLE : View.GONE);
			view.like.setVisibility(view.repost.getVisibility());
			invalidateEmpty();
			return this;
		}
		
		public CountIconButton.Binder like() {
			return view.like.bind();
		}
		
		public CountIconButton.Binder repost() {
			return view.repost.bind();
		}
		
		public Binder saveVisible(boolean visible) {
			view.save.setVisibility(visible ? View.VISIBLE : View.GONE);
			invalidateEmpty();
			return this;
		}
		
		
		private void invalidateEmpty() {
			view.emptiableViewHelper.setEmpty(!EmptiableViewHelper.hasVisibleChildren(view));
		}
		
	}
	
	@Override
	public int visualAscent() {
		return Math.max(
				save.getVisibility() == VISIBLE ? save.getPaddingTop() : 0,
				menu.getVisibility() == VISIBLE ? menu.getPaddingTop() : 0);
	}
	
	@Override
	public int visualDescent() {
		return Math.max(
				save.getVisibility() == VISIBLE ? save.getPaddingBottom() : 0,
				menu.getVisibility() == VISIBLE ? menu.getPaddingBottom() : 0);
	}
}
