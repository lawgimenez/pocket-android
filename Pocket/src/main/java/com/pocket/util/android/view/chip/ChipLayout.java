package com.pocket.util.android.view.chip;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pocket.ui.view.themed.AppThemeUtil;

import org.apache.commons.lang3.StringUtils;
import org.apmem.tools.layouts.FlowLayout;

import java.util.HashMap;

/**
 * A layout that displays text as individual "chips".
 * <p>
 * Based on {@link FlowLayout}, it will display chips along a line, and then wrap to the next when there is no more room in a line.
 * <p>
 * To receive callbacks when a chip is clicked, use {@link #setOnItemClickListener(OnItemClickListener)}.
 * 
 * <h2>Adding Chips</h2>
 * Chips are {@link View}s that represent some text. The view may not always display exactly what the text is in string form. There are two ways to add chips.
 * 
 * <ol>
 * <li>Use a {@link ChipViewCreator}: To use, first call {@link #setAdapter(ChipViewCreator)} so it knows how to build each chip. Then
 * add new chips with {@link #addChip(CharSequence)}. The adapter will be used to convert the text to a view.</li>
 * <li>Use {@link #addChip(Chip)}.</li>
 * </ol>
 * 
 * <h2>Removing Chips</h2>
 * To remove chips use one of the {@link #removeChip(CharSequence)}-like methods. Also can use {@link #removeAllChips()}.
 * 
 * <h2>Developer Notes</h2>
 * Do not use any of the {@link ViewGroup}'s add or remove view methods. That will throw an exception. You must use the addChip or removeChip methods instead. Also use {@link #getChipCount()} instead of {@link #getChildCount()}.
 * 
 */
public class ChipLayout extends FlowLayout implements View.OnClickListener {

	private final HashMap<View, CharSequence> mChipText = new HashMap<View, CharSequence>();
	
	private ChipViewCreator mAdapter;
	private boolean mIsModifyingChildren;
	private OnItemClickListener mOnItemClickListener;

	public ChipLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public ChipLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ChipLayout(Context context) {
		super(context);
	}
	
	/**
	 * @param adapter The {@link ChipViewCreator} that will make new chips as they are added with {@link #addChip(CharSequence)} or {@link #addChip(CharSequence, int)}.
	 */
	public void setAdapter(ChipViewCreator adapter) {
		mAdapter = adapter;
	}
	
	public ChipViewCreator getAdapter() {
		return mAdapter;
	}
	
	/**
	 * <b>Requires {@link #setAdapter(ChipViewCreator)} to be set.</b> Will create a chip view using the {@link ChipViewCreator} and the supplied text.
	 * <p>
	 * Appends to the end the chips.
	 * 
	 * @param text The text of the chip to add. 
	 * @see #addChip(CharSequence, int)
	 * @see #removeChip(CharSequence)
	 * @see #addChip(Chip)
	 */
	public void addChip(CharSequence text) {
		addChip(text, getChipCount());
	}
	
	/**
	 * <b>Requires {@link #setAdapter(ChipViewCreator)} to be set.</b> Will create a chip view using the {@link ChipViewCreator} and the supplied text.
	 * 
	 * @param text The text of the chip to add.
	 * @param position The index of where to insert. 0 would put it as the first chip.
	 * @see #addChip(CharSequence)
	 * @see #getChipCount()
	 * @see #removeChip(CharSequence)
	 */
	public void addChip(CharSequence text, int position) {
		addChipInternal(mAdapter.getView(text, this), text, position);
	}
	
	/**
	 * Adds a new chip to the end of the current chips.
	 * @param chip
	 * @see #addChip(Chip, int)
	 * @see #addChip(CharSequence)
	 */
	public void addChip(Chip chip) {
		addChip(chip, getChipCount());
	}
	
	/**
	 * Adds a new chip to a specific index. 
	 * @param chip
	 * @param position The index of where to insert. 0 would put it as the first chip.
	 * @see #addChip(Chip)
	 * @see #addChip(CharSequence)
	 */
	public void addChip(Chip chip, int position) {
		CharSequence text = chip.getText();
		addChipInternal(chip.getView(text, position, this), text, position);
	}
	
	private void addChipInternal(View view, CharSequence text, int position) {
		if (mOnItemClickListener != null) {
			view.setOnClickListener(this);
		}
		
		mChipText.put(view, text);
		
		setIsModifyingChildren(true);
		addView(view, position);
		setIsModifyingChildren(false);
	}
	
	/**
	 * Remove a chip from this view.
	 * @param text The text of the chip to remove. The text must match what was returned via {@link Chip#getText()} or the text supplied to {@link ChipViewCreator#getView(CharSequence, ViewGroup)}.
	 * @see #removeChipAt(int)
	 */
	public void removeChip(CharSequence text) {
		int size = getChipCount();
		for (int i = 0; i < size; i++) {
			TextView chip = (TextView) getChildAt(i);
			if (StringUtils.equalsIgnoreCase(text, chip.getText())) {
				removeChipAt(i);
				return;
			}
		}
	}
	
	/**
	 * @param position The index of the chip to remove
	 * @see #removeChip(CharSequence)
	 * @see #getChipCount()
	 */
	public void removeChipAt(int position) {
		if (position >= getChipCount() || position < 0) {
			throw new IndexOutOfBoundsException("index of " + position + " is out of bounds. size is " + getChipCount() + ". Use getChipCount() instead of getChildCount()");
		}
		
		View view = getChildAt(position);
		mChipText.remove(view);
		
		setIsModifyingChildren(true);
		removeView(view);
		setIsModifyingChildren(false);
	}
	
	public void removeAllChips() {
		setIsModifyingChildren(true);
		while (getChipCount() > 0) {
			removeChipAt(0);
		}
		mChipText.clear();
		setIsModifyingChildren(false);
	}
	
	/**
	 * @return The number of chips in this layout
	 */
	public int getChipCount() {
		return getChildCount();
	}
	
	/**
     * Register a callback to be invoked when a child has been clicked.
     * <p>
     * Invoking this will change the {@link OnClickListener}'s for all child views of this layout.
     *
     * @param listener The callback that will be invoked.
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
    	mOnItemClickListener = listener;
        
        int children = getChildCount();
        for (int i = 0; i < children; i++) { 
        	View child = getChildAt(i);
        	child.setOnClickListener(listener != null ? this : null);
        }
    }
    
    public CharSequence getChipText(int position) {
    	return getChipText(getChildAt(position));
    }
    
    public CharSequence getChipText(View chip) {
    	return mChipText.get(chip);
    }
   
	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		int[] state = super.onCreateDrawableState(extraSpace + 1);
		mergeDrawableStates(state, AppThemeUtil.getState(this));
		return state;
	}
    
    @Override
    public void onClick(View v) {
    	if (v != this) {
	    	if (mOnItemClickListener != null) {
	    		mOnItemClickListener.onItemClick(this, v, indexOfChild(v));
	    	}
    	}
    }
	
	public interface ChipViewCreator {
		/**
		 * Create/return a chip that represents the provided text.
		 * @param text
		 * @param parent The parent this chip will be added to. Do not add it yourself.
		 * @return
		 */
		public View getView(CharSequence text, ViewGroup parent);
	}
	
	public interface Chip {
    	public View getView(CharSequence text, int position, ViewGroup container);
    	public CharSequence getText();
    }
	
	/**
     * Interface definition for a callback to be invoked when a chip in this
     * AdapterFlowLayout has been clicked.
     */
    public interface OnItemClickListener {

        /**
         * Callback method to be invoked when a chip in this AdapterFlowLayout has
         * been clicked.
         *
         * @param parent The AdapterFlowLayout where the click happened.
         * @param view The view within the AdapterFlowLayout that was clicked (this
         *            will be a view provided by the adapter)
         * @param position The position of the view in the layout.
         */
        public void onItemClick(ChipLayout parent, View view, int position);
    }
    
    /**
     * This allows a subclass to use the add/remove view methods without exceptions. <b>Be very careful
     * using this</b>. Those methods are unavailable because this class manages records of the chips itself.
     * <p>
     * After using it be sure to set back to false.
     * 
     * @param value
     */
    protected void setIsModifyingChildren(boolean value) {
    	mIsModifyingChildren = value;
    }
	
    private boolean allowChildChanges() {
		return mIsModifyingChildren;
	}
	
	@Override
    public void addView(View child) {
        if (allowChildChanges()) {
        	super.addView(child);
        } else {
        	throw new UnsupportedOperationException("addView(View) is not supported. Use the chip specific methods to add and remove..");
        }
    }
	
	@Override
    public void addView(View child, int index) {
		if (allowChildChanges()) {
			super.addView(child, index);
        } else {
        	throw new UnsupportedOperationException("addView(View, int) is not supported. Use the chip specific methods to add and remove..");
        }
    }
	
	@Override
    public void addView(View child, android.view.ViewGroup.LayoutParams params) {
		if (allowChildChanges()) {
			super.addView(child, params);
        } else {
        	throw new UnsupportedOperationException("addView(View, LayoutParams) is not supported. Use the chip specific methods to add and remove..");
        }
    }
	
    @Override
    public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
    	if (allowChildChanges()) {
    		super.addView(child, index, params);
        } else {
        	throw new UnsupportedOperationException("addView(View, int, LayoutParams) is not supported. Use the chip specific methods to add and remove..");
        }
    }

    @Override
    public void removeView(View child) {
    	if (allowChildChanges()) {
    		super.removeView(child);
        } else {
        	throw new UnsupportedOperationException("removeView(View) is not supported. Use the chip specific methods to add and remove..");
        }
    }
    
    @Override
    public void removeViewAt(int index) {
    	if (allowChildChanges()) {
    		super.removeViewAt(index);
        } else {
        	throw new UnsupportedOperationException("removeViewAt(int) is not supported. Use the chip specific methods to add and remove..");
        }
    }

    @Override
    public void removeAllViews() {
    	if (allowChildChanges()) {
    		super.removeAllViews();
        } else {
        	throw new UnsupportedOperationException("removAllViews() is not supported. Use the chip specific methods to add and remove..");
        }
    }
	
}
