package com.pocket.util.android.view.chip;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ideashower.readitlater.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.view.badge.TagBadgeView;
import com.pocket.ui.view.button.IconButton;
import com.pocket.ui.view.themed.ThemedRelativeLayout;
import com.pocket.util.android.ViewUtil;
import com.pocket.util.android.view.ViewVisibleManager;
import com.pocket.util.android.view.chip.ChipLayout.Chip;
import com.pocket.util.android.view.chip.ChipLayout.ChipViewCreator;
import com.pocket.util.java.StringValidator;

/**
 *
 * TODO make more of a pocket-ui component?
 *
 * An {@link EditText}-like view that can show "chips" before the input. These chips can be automatically
 * created when a delimiter character is inputed.
 * <p>
 * By default, it acts like an {@link EditText}. Chips can be added before the input area with the {@link #addChip(CharSequence)} like
 * methods.
 * 
 * <h2>Customize</h2>
 * For options see the public methods and the xml attr "ChipEditText".
 * 
 * <h2>Auto Commit/Create Chips</h2>
 * A delimiter character or multiple characters can be added via the xml attribute ChipEditText_chiptext_commitChars. When this is not null
 * it enables auto-committing of new chips. While enabled, the text input will be converted to a chip in the following cases:
 * <p>
 * <ul>
 * <li>If one of the chars in the commitChars string is typed. For example, if commitChars = ",." then any time a , or a . is typed, the text
 * to the left of the commit char will be converted into a chip.</li>
 * <li>If input containing a commit char is pasted into the field</li>
 * <li>If the keyboard done/action/enter button is pressed</li>
 * <li>{@link #commitPending()} is invoked</li>
 * </ul>
 * While using commit chars to auto create chips, <b>you must set an adapter with {@link #setAdapter(ChipViewCreator)}</b>.
 * <p> 
 * You should also set a validator via {@link #setValidator(StringValidator)}. This allows you to validate the text before creating a chip.
 * 
 * <h2>Listeners</h2>
 * There are a few different listeners available for changes to the content of this view.
 * <ul>
 * <li>{@link #setOnChipsChangedListener(ChipInputCommitListener)} For updates to chips caused by user commiting chips or removing them through typing or input. This is not invoked if the chips are added programatically such as through {@link #addChip(Chip)}.</li>
 * <li>{@link #addTextWatcher(TextWatcher)} For updates when the user changes the text input. This does not include chip changes.</li>
 * <li>{@link #addChipEditTextWatcher(ChipEditTextWatcher)} For updates when any of the content (chips or text input) change in someway. Such as typing a character or removing a chip. This catches all changes no matter where they come from.</li>
 * <li>{@link #setOnInputDoneListener(OnInputDoneListener)} For notification when a user hits enter or done.</li>
 * </ul>
 * 
 * <h2>Developer/Implementation Notes</h2>
 * This class is mainly a layout that holds the main components. Most of its public methods are proxies to methods on the internal classes.
 * <p>
 * The chips and EditText field are handled by {@link ChipEditTextInternal} and the clear button is just an {@link IconButton}.
 * <p>
 * All touch events that don't hit a child view are redirected into the EditText. This gives the impression that the whole view
 * is an EditText. That class is {@link ChipEditTextInternalInput}. All of the logic for auto-committing chips is contained
 * in that class.
 * 
 * TODO Max needs to add some developer notes on touch event handling. There are some complexities here.
 * 
 */
public class ChipEditText extends ThemedRelativeLayout {

	protected static class LineMode {
		/** Single line, horizontally scrollable. */
		protected static final int SINGLE = -1;
		/** Mutli line, no maximum. */
		protected static final int MULTI = 0;
	}
	
	private static final int[] STATE_FOCUSED = {android.R.attr.state_focused};
	private static final int[] STATE_ERROR = {R.attr.state_error};
	
	private ChipEditTextInternal mEditText;

	private boolean mIsInvalid;
	private ViewVisibleManager mClearVisibility;

	private boolean mIsClearEnabled;

	/**
	 * In certain modes this will not be used and will be null.
	 */
	private HorizontalScrollView mScrollerView;
	
	public ChipEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}

	public ChipEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public ChipEditText(Context context) {
		super(context);
		init(null);
	}
	
	private void init(AttributeSet attrs) {
		// Defaults
		int lineMode = LineMode.SINGLE;
		String commitChars = null;
		boolean isClearEnabled = false;
		int fieldStyle = 0;
		int fieldHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
		CharSequence hint = null;
		boolean isAddChipAnimationEnabled = false;
		int flowStyle = 0;
		
		// Via XML
		if (attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ChipEditText);
			
			lineMode = a.getInt(R.styleable.ChipEditText_chiptext_lineMode, lineMode);
			commitChars = a.getString(R.styleable.ChipEditText_chiptext_commitChars) != null ? a.getString(R.styleable.ChipEditText_chiptext_commitChars) : commitChars;
			isClearEnabled = a.getBoolean(R.styleable.ChipEditText_chiptext_clearEnabled, isClearEnabled);
			
			fieldStyle = a.getResourceId(R.styleable.ChipEditText_chiptext_fieldStyle, fieldStyle);
			fieldHeight = (int) a.getDimension(R.styleable.ChipEditText_chiptext_fieldHeight, fieldHeight);
			flowStyle = a.getResourceId(R.styleable.ChipEditText_chiptext_flowStyle, flowStyle);
			
			isAddChipAnimationEnabled = a.getBoolean(R.styleable.ChipEditText_chiptext_animateAdditions, false);
			hint = a.getString(R.styleable.ChipEditText_chiptext_hint);
			
			a.recycle();
		}
		
		// Clear
		View clear = LayoutInflater.from(getContext()).inflate(R.layout.view_search_clear, this, false);
		clear.setOnClickListener(v -> clear());
		mClearVisibility = new ViewVisibleManager(clear, View.GONE);
		mClearVisibility.addCondition(() -> mIsClearEnabled);
		mClearVisibility.addCondition(() -> getChipCount() > 0 || getText().length() > 0);
		addView(clear);
		
		// Chip Layout
		mEditText = new ChipEditTextInternal(this, lineMode, commitChars, flowStyle, fieldStyle, fieldHeight);
		mEditText.setHint(hint);
		mEditText.setAddChipAnimationEnabled(isAddChipAnimationEnabled);
		View editTextView;
		LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		if (lineMode == LineMode.SINGLE) {
			mScrollerView = new HorizontalScrollView(getContext()) {
				
				@Override
				public boolean onInterceptTouchEvent(MotionEvent ev) {
					if (mEditText.getWidth() > mScrollerView.getWidth()) {
						return super.onInterceptTouchEvent(ev);
					} else {
						return false;
					}
				}
				
				@Override
				public boolean onTouchEvent(MotionEvent ev) {
					if (mEditText.getWidth() > mScrollerView.getWidth()) {
						return super.onTouchEvent(ev);
					} else {
						ViewUtil.moveTouchEventIntoBounds(ev, mEditText);
						return true;
					}
				}
				
			};
			int border = DimenUtil.dpToPxInt(getContext(), 1);
			mScrollerView.setPadding(getPaddingLeft() - border, getPaddingTop() - border, getPaddingRight() - border, getPaddingBottom() - border);
			setPadding(border, border, border, border);
			mScrollerView.setClipToPadding(false);
			mScrollerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
			mScrollerView.setHorizontalScrollBarEnabled(false);
			mScrollerView.setVerticalScrollBarEnabled(false);
			mScrollerView.addView(mEditText);
			lp.width = RelativeLayout.LayoutParams.WRAP_CONTENT;
			editTextView = mScrollerView;
			lp.addRule(RelativeLayout.CENTER_VERTICAL);
			lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		} else {
			mScrollerView = null;
			editTextView = mEditText;
		}
		lp.addRule(RelativeLayout.LEFT_OF, clear.getId());
		addView(editTextView, lp);
		
		addChipEditTextWatcher((chipCount, inputText) -> mClearVisibility.invalidate());
		
		setClearingEnabled(isClearEnabled);
		setClickable(true);

		engageable.setUiEntityType(Type.BUTTON);
	}
	
	/** Callback from internal classes to notify that the field validility has changed. */
	protected void setIsInputValid(boolean valid) {
		mIsInvalid = !valid;
	}
	
	/** @see ChipEditTextInternal#refocus() */
	public void refocus() {
		mEditText.refocus();
	}
	
	/** @see ChipEditTextInternal#isFieldFocused() */
	public boolean isFieldFocused() {
		return mEditText.isFieldFocused();
	}
	
	/** Release any focus from the field and close the softkeyboard. */
	public void unfocus() {
		mEditText.unfocus();
	}
	
	/**
	 * Whether or not this field can be cleared by tapping an X button on the far right.
	 */
	public void setClearingEnabled(boolean enabled) {
		mIsClearEnabled = enabled;
		mClearVisibility.invalidate();
	}
	
	/**
	 * For more on listeners, see the {@link ChipEditText} doc.
	 * @see OnInputDoneListener
	 */
	public void setOnInputDoneListener(OnInputDoneListener listener) {
		mEditText.setOnInputDoneListener(listener);
	}
	
	/**
	 * For more on listeners, see the {@link ChipEditText} doc.
	 * @see ChipEditTextInternal#addTextWatcher(TextWatcher)
	 */
	public void addTextWatcher(TextWatcher watcher) {
		mEditText.addTextWatcher(watcher);
	}
	
	/**
	 * For more on listeners, see the {@link ChipEditText} doc.
	 * @see ChipEditTextInternal#addChipEditTextWatcher(ChipEditTextWatcher)
	 */
	public void addChipEditTextWatcher(ChipEditTextWatcher watcher) {
		mEditText.addChipEditTextWatcher(watcher);
	}

    /**
     * For more on listeners, see the {@link ChipEditText} doc.
     * @see ChipEditTextInternal#removeChipEditTextWatcher(ChipEditTextWatcher)
     */
    public void removeChipEditTextWatcher(ChipEditTextWatcher watcher) {
        mEditText.removeChipEditTextWatcher(watcher);
    }
	
	/**
	 * For more on listeners, see the {@link ChipEditText} doc.
	 * @see ChipInputCommitListener
	 * @see #addChipEditTextWatcher(ChipEditTextWatcher)
	 */
	public void setOnChipsChangedListener(ChipInputCommitListener listener) {
		mEditText.setOnChipsChangedListener(listener);
	}
	
	public void setOnInputFocusChangedListener(OnInputFocusChangeListener listener) {
		mEditText.setOnInputFocusChangedListener(listener);
	}

	public void showKeyboard() {
		mEditText.showKeyboard();
	}

	public void setFilters(InputFilter[] filters) {
		mEditText.setFilters(filters);
	}
	
	/**
	 * @see ChipEditTextInternal#moveCursorToEnd();
	 */
	public void moveCursorToEnd() {
		mEditText.moveCursorToEnd();
	}
	
	/**
	 * @see ChipEditTextInternal#setText(String)
	 */
	public void setText(String searchText) {
		mEditText.setText(searchText);
	}
	
	/**
	 * @see ChipEditTextInternal#getText()
	 */
	public CharSequence getText() {
		return mEditText.getText();
	}
	
	/**
	 * @see ChipEditTextInternal#setHint(CharSequence)
	 */
	public void setHint(CharSequence hint) {
		mEditText.setHint(hint);
	}
	
	/**
	 * @see ChipEditTextInternal#setValidator(StringValidator)
	 */
	public void setValidator(StringValidator validator) {
		mEditText.setValidator(validator);
	}
	
	/**
	 * @see ChipEditTextInternal#commitPending()
	 */
	public boolean commitPending() {
		return mEditText.commitPending();
	}
	
	/**
	 * @see ChipEditTextInternal#clearText()
	 */
	public void clearText() {
		mEditText.clearText();
	}
	
	/**
	 * @see ChipEditTextInternal#setMimicChipAdapterStyleEnabled(boolean)
	 */
	public void setMimicChipAdapterStyleEnabled(boolean enabled) {
		mEditText.setMimicChipAdapterStyleEnabled(enabled);
	}
	
	/**
	 * @see ChipEditTextInternal#clear()
	 */
	public void clear() {
		mEditText.clear();
	}
	
	/** 
	 * @see ChipLayout#setAdapter(ChipViewCreator) 
	 */
	public void setAdapter(ChipViewCreator adapter) {
		mEditText.setAdapter(adapter);
	}

	public void defaultAdapter() {
		mEditText.setAdapter(((text, parent) -> {
			TextView chip = new TagBadgeView(getContext());
			chip.setText(text);
			chip.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			return chip;
		}));
	}
	
	/**
	 * @see ChipLayout#getChipCount()
	 */
	public int getChipCount() {
		return mEditText.getChipCount();
	}
	
	/**
	 * @see ChipLayout#removeAllChips()
	 */
	public void removeAllChips() {
		mEditText.removeAllChips();
	}
	
	/**
	 * @see ChipLayout#addChip(CharSequence)
	 */
	public void addChip(CharSequence text) {
		mEditText.addChip(text);
	}
	
	/**
	 * @see ChipLayout#addChip(CharSequence, int)
	 */
	public void addChip(CharSequence text, int position) {
		mEditText.addChip(text, position);
	}
	
	/**
	 * @see ChipLayout#addChip(Chip)
	 */
	public void addChip(Chip chip) {
		mEditText.addChip(chip);
	}
	
	/**
	 * @see ChipLayout#addChip(Chip, int)
	 */
	public void addChip(Chip chip, int position) {
		mEditText.addChip(chip, position);
	}
	
	/**
	 * @see ChipEditTextInternal#getChipText(int)
	 */
	public CharSequence getChipText(int position) {
		return mEditText.getChipText(position);
	}
	
	/**
	 * @see ChipLayout#removeChip(CharSequence)
	 */
	public void removeChip(String tag) {
		mEditText.removeChip(tag);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mScrollerView != null) {
			ViewUtil.moveTouchEventIntoBounds(event, mScrollerView);
		} else {
			ViewUtil.moveTouchEventIntoBounds(event, mEditText);
		}
		return true;
	}
	
	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		invalidate();
	}
	
	@Override
    public int[] onCreateDrawableState(int extraSpace) {
		final int[] state = super.onCreateDrawableState(extraSpace + 2);
		if (mIsInvalid) {
			mergeDrawableStates(state, STATE_ERROR);
		}
		if (mEditText != null && mEditText.isFieldFocused()) {
			mergeDrawableStates(state, STATE_FOCUSED);
		}
		return state;
	}
	
	/**
	 * <b>Not supported, will throw exception.</b> Tapping a chip selects it.
	 */
	@Override
	public void setOnClickListener(OnClickListener l) {
		throw new UnsupportedOperationException("setOnClickListener() is not allowed with this class.");
	}
	
	/**
	 * Provides callbacks about changes to the chips caused by user input in the text field.
	 * @see ChipEditTextWatcher
	 */
	public interface ChipInputCommitListener {
		/**
		 * The field attempted to commit text into a chip, but the validator provided via {@link ChipEditTextInternal#setValidator(StringValidator)}
		 * returned an error.
		 * 
		 * @param error The error.
		 */
		void onChipError(String error);
		
		/**
		 * A new chip was created through a commit.
		 * @param text
		 */
		void onChipCommitted(CharSequence text);
		
		/**
		 * A chip was deleted via the backspace key.
		 * @param text
		 */
		void onChipDeleted(CharSequence text);
	}
	
	/**
	 * Provides a callback for when the chips or input text have changed in any way.
	 */
	public interface ChipEditTextWatcher {
		/** The chips, input text or both have changed in some way. */
		void onChipEditTextChanged(int chipCount, CharSequence inputText);
	}
	
	/**
	 * Provides a callback for when the user presses the enter or done key.
	 */
	public interface OnInputDoneListener {
		/** Invoked when the done button or enter key is pressed. */
		void onInputDone();
	}
	
	public interface OnInputFocusChangeListener {
		void onFocusChanged(boolean hasFocus);
	}
	
}
