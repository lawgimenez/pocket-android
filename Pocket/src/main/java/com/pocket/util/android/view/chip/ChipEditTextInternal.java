package com.pocket.util.android.view.chip;

import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import com.ideashower.readitlater.R;
import com.pocket.ui.util.Interpolators;
import com.pocket.util.android.SimpleTextWatcher;
import com.pocket.util.android.ViewUtil;
import com.pocket.util.android.view.chip.ChipEditText.ChipEditTextWatcher;
import com.pocket.util.android.view.chip.ChipEditText.ChipInputCommitListener;
import com.pocket.util.android.view.chip.ChipEditText.LineMode;
import com.pocket.util.android.view.chip.ChipEditText.OnInputDoneListener;
import com.pocket.util.android.view.chip.ChipEditText.OnInputFocusChangeListener;
import com.pocket.util.android.view.chip.ChipEditTextInternalInput.OnChipCommitListener;
import com.pocket.util.java.StringValidator;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

/**
 * Part of the {@link ChipEditText} widget that handles the chips and text input. The text input is managed by a internal {@link ChipEditTextInternalInput}.
 * <p>
 * This is a {@link ChipLayout} that inserts the EditText as the last view in the layout so it always is displayed after the chips.
 * <p>
 * This class does most of the heavy lifting for the {@link ChipEditText} in terms of updating the chip views.
 */
class ChipEditTextInternal extends ChipLayout implements ChipLayout.OnItemClickListener {
	
	private final ArrayList<ChipEditTextWatcher> mChipEditTextWatchers = new ArrayList<ChipEditTextWatcher>();
	private final ChipEditText mParent;
	private final ChipEditTextInternalInput mEditText;
	
	private View mSelected;
	
	private StringValidator mValidator;
	private ChipInputCommitListener mOnChipsChangedListener;
	private CharSequence mHint;
	private boolean mAddAnimationEnabled;
	private boolean mEditTextMimicChipAdapterStyleEnabled;
	private OnInputFocusChangeListener mOnInputFocusChangeListener;
	private OnHierarchyChangeListener mOnHierarchyChangeListener;
	
	public ChipEditTextInternal(ChipEditText parent, int lineMode, String commitChars, int flowStyle, int fieldStyle, int fieldHeight) {
		super(parent.getContext());
		
		mParent = parent;
		applyStyle(flowStyle);
		
		mEditText = inflateInnerEditText(commitChars, fieldStyle, fieldHeight);
		
		if (lineMode == ChipEditText.LineMode.SINGLE) {
			setMaxLines(-1);
			mEditText.setMaxLines(1);
			mEditText.setSingleLine(true);
		} else {
			setMaxLines(lineMode);
		}
		
		setHintVisible(true);
		
		super.setOnClickListener(this);
		super.setOnItemClickListener(this);
		super.setOnHierarchyChangeListener(new OnHierarchyChangeListener() {
			
			@Override
			public void onChildViewRemoved(View parent, View child) {
				unselectChips();
				setHintVisible(getChipCount() == 0);
				scrollParentToBottom();
				invokeWatchers();

				if (mOnHierarchyChangeListener != null) {
					mOnHierarchyChangeListener.onChildViewRemoved(parent, child);
				}
			}
			
			@Override
			public void onChildViewAdded(View parent, View child) {
				unselectChips();
				setHintVisible(false);
				scrollParentToBottom();
				
				if (mAddAnimationEnabled) {
					ScaleAnimation anim = new ScaleAnimation(
							0.0f, 1.0f,
							1.0f, 1.0f,
							Animation.RELATIVE_TO_SELF,0.0f,
							Animation.RELATIVE_TO_SELF, 0.5f);
					anim.setDuration(200);
					anim.setInterpolator(Interpolators.DECEL);
					child.startAnimation(anim);
				}
				
				invokeWatchers();

				if (mOnHierarchyChangeListener != null) {
					mOnHierarchyChangeListener.onChildViewAdded(parent, child);
				}
			}
		});
		
		setClickable(true); // Absorb touches to pass onto the edit text
	}
	
	@Override public void setOnHierarchyChangeListener(OnHierarchyChangeListener listener) {
		// Don't just forward to super, because we rely on an internal hierarchy change listener.
		// Instead we'll call this external listener from our internal one.
		mOnHierarchyChangeListener = listener;
	}

	/**
	 * Initialize the internal EditText that handles text input from the user.
	 * 
	 * @param commitChars
	 * @param style
	 * @param height
	 * @return
	 */
	private ChipEditTextInternalInput inflateInnerEditText(String commitChars, int style, int height) {
		ChipEditTextInternalInput view = ChipEditTextInternalInput.inflate(getContext(), commitChars, style);
		view.setOnChipCommitListener(new OnChipCommitListener() {
			
			@Override
			public String validate(String tag) {
				return mValidator.validate(tag);
			}
			
			@Override
			public void onChipsCommitted(ArrayList<CharSequence> chips) {
				for (CharSequence text : chips) {
					onChipCommitted(text);
				}
			}
			
			@Override
			public void onChipCommitted(CharSequence text) {
				addChip(text);
				if (mOnChipsChangedListener != null) {
					mOnChipsChangedListener.onChipCommitted(text);
				}
				mParent.setIsInputValid(true);
				forceStateUpdate();
			}

			@Override
			public void deleteSelectedChip() {
				int removeAt = -1;
				if (mSelected != null) {
					removeAt = indexOfChild(mSelected);
				} else if (getChipCount() > 0) {
					removeAt = getChipCount()-1;
				}
				mSelected = null;
				
				if (removeAt >= 0) {
					CharSequence text = getChipText(removeAt);
					
					removeChipAt(removeAt);
					
					if (mOnChipsChangedListener != null) {
						mOnChipsChangedListener.onChipDeleted(text);
					}
				}
				
				mParent.setIsInputValid(true);
				forceStateUpdate();
				
			}
			
			@Override
			public void onInvalidInput(ArrayList<String> errors) {
				if (mOnChipsChangedListener != null) {
					mOnChipsChangedListener.onChipError(errors.get(0)); // Just show first error)
				}
				mParent.setIsInputValid(false);
				forceStateUpdate();
			}

		});
		
		view.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				scrollParentToBottom();
				forceStateUpdate();
				if (mOnInputFocusChangeListener != null) {
					mOnInputFocusChangeListener.onFocusChanged(hasFocus);
				}
			}
		});
		
		view.addTextChangedListener(new SimpleTextWatcher() {
			
			@Override
			public void afterTextChanged(Editable s) {
				unselectChips();
				setHintVisible(getChipCount() == 0);
				invokeWatchers();
				
				checkInputValidity(getText().toString());
				
			}
		});
		
		setIsModifyingChildren(true);
		addView(view);
		setIsModifyingChildren(false);
		
		ViewUtil.setLayoutHeight(view, height);
		return view;
	}
	
	/**
	 * Release any focus from the field and close the softkeyboard.
	 */
	public void unfocus() {
		if (mEditText.isFocused()) {
			mEditText.clearFocus();
			ViewUtil.forceSoftKeyboard(false, mEditText);
			forceStateUpdate();
		}
	}
	
	/**
	 * Whether or not to automatically animate a new chip as it is added.
	 * @param enabled
	 */
	public void setAddChipAnimationEnabled(boolean enabled) {
		mAddAnimationEnabled = enabled;
	}
	
	/**
	 * Set the text input. Does not effect chips. Note, if there are commit chars within the text, some may automatically
	 * be converted to chips.
	 * 
	 * @param searchText
	 */
	public void setText(String searchText) {
		mEditText.setText(searchText);
	}
	
	/**
	 * @return The input text. Does not include chips.
	 */
	public CharSequence getText() {
		return mEditText.getText();
	}
	
	/**
	 * Set the hint text to appear when there are no chips and no input text.
	 * @param hint
	 */
	public void setHint(CharSequence hint) {
		mHint = hint;
		setHintVisible(StringUtils.isBlank(getText()) && getChipCount() == 0);
	}

	/**
	 * Only listens to input text changes, does not invoke when chips are added or removed.
	 * @param watcher
	 * @see #addChipEditTextWatcher(ChipEditTextWatcher)
	 */
	public void addTextWatcher(TextWatcher watcher) {
		mEditText.addTextChangedListener(watcher);
	}
	
	/**
	 * Set a validator to check text input before creating a new chip.
	 * @param validator
	 */
	public void setValidator(StringValidator validator) {
		mValidator = validator;
	}
	
	/**
	 * Set a listener for chip change events.
	 * @param listener
	 */
	public void setOnChipsChangedListener(ChipInputCommitListener listener) {
		mOnChipsChangedListener = listener;
	}
	
	/**
	 * Registers a listener for when chips or the input text are changed.
	 * @param listener
	 */
	public void addChipEditTextWatcher(ChipEditTextWatcher listener) {
		mChipEditTextWatchers.add(listener);
	}
	
	/**
	 * Removes a listener added via {@link #addChipEditTextWatcher(ChipEditTextWatcher)}.
	 * @param listener
	 */
	public void removeChipEditTextWatcher(ChipEditTextWatcher listener) {
		mChipEditTextWatchers.remove(listener);
	}
	
	/**
	 * Convert any inputed text into a chip. Requires commit chars to be set via xml.
	 * @return
	 */
	public boolean commitPending() {
		return mEditText.commitPending();
	}
	
	/**
	 * Removes the pending input. Does not remove any chips.
	 * @see #clear()
	 */
	public void clearText() {
		mEditText.setText("");
	}
	
	/**
	 * Removes all chips and all text.
	 * @see #clearText()
	 */
	public void clear() {
		mEditText.setText(null);
		removeAllChips();
	}
	
	/**
	 * Focus the field, open the keyboard and scroll to the bottom/right. Also unselects any selected chips.
	 */
	public void refocus() {
		unselectChips();
		focusEditText();
		scrollParentToBottom();
	}
	
	public boolean isFieldFocused() {
		return mEditText.isFocused();
	}
	
	/**
	 * Move the cursor to the end of the current input and scroll to the bottom/right.
	 */
	public void moveCursorToEnd() {
		mEditText.setSelection(mEditText.getText().length());
		getHandler().post(() -> mEditText.setSelection(mEditText.getText().length()));
		scrollParentToBottom();
	}
	
	@Override
	public void setAdapter(ChipViewCreator adapter) {
		super.setAdapter(adapter);
		if (mEditTextMimicChipAdapterStyleEnabled) {
			mimicAdapterStyle();
		}
	}
	
	/**
	 * If true, the input area will make its padding and height the same as the views returned from the {@link ChipViewCreator} set in {@link #setAdapter(ChipViewCreator)}. 
	 * @param enabled
	 */
	public void setMimicChipAdapterStyleEnabled(boolean enabled) {
		mEditTextMimicChipAdapterStyleEnabled = enabled;
		if (enabled) {
			mimicAdapterStyle();
		}
	}
	
	private boolean mimicAdapterStyle() {
		if (getAdapter() == null) {
			return false;
		}
		
		// Extract the chip's sizing to match the internal edit text
		View fake = getAdapter().getView("", this);
		if (fake == null) {
			return false;
		}
		
		mEditText.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, fake.getLayoutParams().height));
		mEditText.setPadding(fake.getPaddingLeft(), fake.getPaddingTop(), fake.getPaddingRight(), fake.getPaddingBottom());
		return true;
	}
	
	/**
	 * @see ChipEditTextInternalInput#setOnInputDoneListener(OnInputDoneListener)
	 */
	public void setOnInputDoneListener(OnInputDoneListener listener) {
		mEditText.setOnInputDoneListener(listener);
	}
	
	public void setOnInputFocusChangedListener(OnInputFocusChangeListener listener) {
		mOnInputFocusChangeListener = listener;
	}

	public void setFilters(InputFilter[] filters) {
		mEditText.setFilters(filters);
	}

	public void showKeyboard() {
		ViewUtil.forceFocus(true, mEditText);
	}
	
	private void setHintVisible(boolean visible) {
		if (visible) {
			mEditText.setHint(mHint);
		} else {
			mEditText.setHint(null);
		}
	}
	
	@Override
	public void onItemClick(ChipLayout parent, View view, int position) {
		// Clicks on chips, select them.
		unselectChips();
		if (view != mEditText) {
			mSelected = view;
			mEditText.setSelection(0);
			view.setSelected(true);
			focusEditText();
		}
	}
	
	@Override
	public int getChipCount() {
		return getChildCount() - 1; // Account for the edit text
	}
	
	/**
	 * If a chip is selected, unselect it.
	 */
	private void unselectChips() {
		if (mSelected != null) {
			mSelected.setSelected(false);
			mSelected = null;
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		ViewUtil.moveTouchEventIntoBounds(event, mEditText);
		if (event.getAction() == MotionEvent.ACTION_UP) {
			refocus();
		}
		return true;
	}
	
	/**
	 * Focus the field, open the keyboard
	 */
	private void focusEditText() {
		ViewUtil.forceFocus(true, mEditText);
		ViewUtil.forceSoftKeyboard(true, mEditText);
	}
	
	/**
	 * If the parent of this view is a ScrollView, scroll to the bottom/right of it.
	 */
	private void scrollParentToBottom() {
		// Find a parent ScrollView if there is one. 
		final View parent = findScrollParent();
		if (parent != null) {
			final Handler handler = parent.getHandler();

			parent.scrollTo(getWidth(), getHeight());
			
			// To scroll properly to the bottom, a post() is often needed.
			if (handler != null) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						parent.scrollTo(getWidth(), getHeight());
					}
				});
			}
		}		
	}
	
	/**
	 * Returns a scroll view parent or null. {@link ChipEditText} in {@link LineMode#SINGLE} will return its internal {@link HorizontalScrollView}.
	 * Otherwise, if the {@link ChipEditText} is wrapped by a {@link ScrollView} it will return that.
	 * @return
	 */
	private View findScrollParent() {
		if (getParent() instanceof HorizontalScrollView) {
			// Parent is in single line mode
			return (View) getParent();
			
		} else if (ViewUtil.getParent(this, 2) instanceof ScrollView) {
			// Parent is in multi line mode and is wrapped by a vertical scroll view
			return ViewUtil.getParent(this, 2);
			
		} else {
			return null;
		}
	}
	
	/**
	 * Invalidate and recheck the drawable state
	 */
	private void forceStateUpdate() {
		invalidate();
		refreshDrawableState();
		mParent.invalidate();
		mParent.refreshDrawableState();
	}
	
	private void invokeWatchers() {
		int chipCount = getChipCount();
		CharSequence inputText = getText();
		for (ChipEditTextWatcher watcher : mChipEditTextWatchers) {
			watcher.onChipEditTextChanged(chipCount, inputText);
		}
	}
	
	/**
	 * <b>Not supported, will throw exception.</b> Tapping a chip selects it.
	 */
	@Override
	public void setOnClickListener(OnClickListener l) {
		throw new UnsupportedOperationException("setOnClickListener() is not allowed with this class.");
	}

	/**
	 * <b>Not supported, will throw exception.</b> Tapping a chip selects it.
	 */
	@Override
	public void setOnItemClickListener(OnItemClickListener listener) {
		throw new UnsupportedOperationException("setOnItemClickListener() is not allowed with this class.");
	}
	
	private void checkInputValidity(String text) {
		
		if (mValidator!=null){
			if (mValidator.validate(text)==null) {
				// Text is valid
				mParent.setIsInputValid(true);
			} else {
				// Text is invalid
				mParent.setIsInputValid(false);
			}
			forceStateUpdate();
		}
	}
	
}
