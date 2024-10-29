
package com.pocket.util.android.view.chip;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.core.view.ViewKt;

import com.ideashower.readitlater.R;
import com.pocket.ui.view.themed.ThemedEditText;
import com.pocket.util.android.FormFactor;
import com.pocket.util.android.SimpleTextWatcher;
import com.pocket.util.android.view.chip.ChipEditText.OnInputDoneListener;
import com.pocket.util.java.StringBuilders;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

/**
 * The internal {@link EditText} used by {@link ChipEditTextInternal}. Not intended for other use.
 * <p>
 * This class handles all of the text watching logic needed to auto-commit text into chips.
 */
public class ChipEditTextInternalInput extends ThemedEditText implements OnEditorActionListener {

    /**
     * This inflation method allows us to set the theme for the view so we can set the Material Theme's accent and activated colors to use.
     * @param context
     * @param commitChars
     * @param styleId
     * @return
     */
    public static ChipEditTextInternalInput inflate(Context context, String commitChars, int styleId) {
        ChipEditTextInternalInput view = new ChipEditTextInternalInput(context);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (commitChars != null) {
            view.setCommitChars(commitChars.toCharArray());
        } else {
            view.setCommitChars(new char[0]);
        }

        if (styleId != 0) {
            view.setTextAppearance(context, styleId);
        }
        return view;
    }
	
	private char[] mCommitCharsArray;
	private String mCommitChars;
	private OnChipCommitListener mOnChipCommitListener;
	private OnInputDoneListener mOnInputDoneListener;
	private boolean mIsChangingText;

	/**
	 * See {@link InputConnectionHelper}
	 */
	private KeyEvent mLastDelete;

    public ChipEditTextInternalInput(Context context) {
        super(context);
        init();
    }

    private void init() {
		setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		setMinWidth(FormFactor.dpToPx(40));
		ViewKt.setPadding(this, 0);
		setBackgroundDrawable(null);
		setOnEditorActionListener(this);
		setSingleLine(true);
		setMaxLines(1);
		setInputType(getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		
		addTextChangedListener(new SimpleTextWatcher() {
			
			@Override
			public void afterTextChanged(Editable s) {
				if (!mIsChangingText) {
					checkForCommits(false);
				} else {
					mIsChangingText = false;
				}
			}
			
		});

		setOnKeyListener((v, keyCode, event) -> checkForDeleteEvent(event));
	}

	private boolean checkForDeleteEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN
				&& event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
			if (mLastDelete != event) {
				onBackspaceKey();
				mLastDelete = event;
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Commit any text still pending.
	 * @return true if ok, false if there was an error needing user attention in order to commit.
	 */
	public boolean commitPending() {
		return checkForCommits(true);
	}
	
	/**
	 * Set a char that when typed, it will attempt commit the input before it into a chip.
	 * @param value Not null
	 */
	protected void setCommitChar(char value) {
		setCommitChars(new char[]{value});
	}
	
	/**
	 * A set of chars that when one is typed, it will attempt to commit the input before it into a chip.
	 * @param chars not null, can be empty
	 */
	protected void setCommitChars(char[] chars) {
		mCommitCharsArray = chars;
		mCommitChars = String.valueOf(chars);
	}
	
	/**
	 * @see OnInputDoneListener#onInputDone()
	 * @param listener
	 */
	protected void setOnInputDoneListener(OnInputDoneListener listener) {
		mOnInputDoneListener = listener;
	}
	
	/**
	 * Set a listener for managing the auto-committing of chips.
	 * @param listener
	 */
	protected void setOnChipCommitListener(OnChipCommitListener listener) {
		mOnChipCommitListener = listener;
	}
	
	@Override
    public boolean onEditorAction(TextView view, int action, KeyEvent keyEvent) {
        if (action == EditorInfo.IME_ACTION_DONE) {
        	checkForCommits(true);
        	invokeDoneListener();
        	return true;
        }
        return false;
    }
	
	/**
     * Monitor key presses in this view to see if the user types
     * any commit keys, which consist of ENTER, TAB, or DPAD_CENTER.
     * If the user has entered text that has contact matches and types
     * a commit key, create a chip from the topmost matching contact.
     * If the user has entered text that has no contact matches and types
     * a commit key, then create a chip from the text they have entered.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_ENTER:
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_TAB:
            if (event.hasNoModifiers()) {
                checkForCommits(true);
                if (keyCode != KeyEvent.KEYCODE_TAB) {
                	invokeDoneListener();
                }
                return true;
            }
            break;
        }
        return super.onKeyUp(keyCode, event);
    }
	
	@Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection connection = new InputConnectionHelper(super.onCreateInputConnection(outAttrs), true);
		//InputConnection connection = super.onCreateInputConnection(outAttrs);
        
        // Change to a done/return button that keeps focus in the field.
        int imeActions = outAttrs.imeOptions&EditorInfo.IME_MASK_ACTION;
        if ((imeActions&EditorInfo.IME_ACTION_DONE) != 0) {
            // clear the existing action
            outAttrs.imeOptions ^= imeActions;
            // set the DONE action
            outAttrs.imeOptions |= EditorInfo.IME_ACTION_DONE;
        }
        if ((outAttrs.imeOptions&EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
            outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        }
        outAttrs.actionLabel = getContext().getString(R.string.done);
        return connection;
    }

	/** 
	 * Reliable delete key detection on hard and soft keyboards.
	 *
	 * Update 8/28/19
	 * This is an attempt to resolve even more delete detection strangeness, related to this bug ticket: https://getpocket.atlassian.net/browse/BUG-3094,
	 * which is a bug wherein hardware delete keys do not delete tags in this EditText.
	 *
	 * {@link InputConnection}, used here, is specifically related to the software keyboard, and *should* in theory have no detection of hardware
	 * keyboard presses.  In testing, this does appear to be the case.  No issue there.
	 *
	 * This means that in order to detect a delete key on a hardware keyboard, we need to add an {@link OnKeyListener}, which has been added to this View's init() method.
	 * OnKeyListener *should* only detect hardware key presses.  In testing this appears to be the case for every hardware key *except*, hilariously, the backspace key, which is
	 * the only key picked up by OnKeyListener.  That same event is then also passed here, to InputConnection, causing a double delete.
	 *
	 * To prevent this, we keep a reference to the last delete event, and if it's the same (meaning it already passed through OnKeyListener), then we don't call onBackspaceKey()
	 * again.
	 */
    private class InputConnectionHelper extends InputConnectionWrapper {

        public InputConnectionHelper(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            checkForDeleteEvent(event);
            return super.sendKeyEvent(event);
        }
        
        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
			/*
			 * Ok, so Swype stopped letting users delete tags with the backspace key. DROID-360
			 * For some reason in their keyboard, this triggers a beforeLength=0 instead of 1 like
			 * the Google Keyboard and others.
			 *
			 * Not sure why the if (beforeLength > 0 && afterLength == 0) {
			 * statement was there, but removing it appears to work still on
			 * all keyboards.... perhaps there will now be a different regression?
			 *
			 * Leaving this in case you come wondering back here, this is where things are.
			 * Good luck...
			 */

			onBackspaceKey();

//			The code was this before:
// 			if (beforeLength > 0 && afterLength == 0) {
//        		onBackspaceKey();
//        	}

        	return super.deleteSurroundingText(beforeLength, afterLength);
        }
        
        @Override
        public boolean performEditorAction(int editorAction) {
        	if (editorAction == EditorInfo.IME_ACTION_DONE) {
        		checkForCommits(true);
        		invokeDoneListener();
        	}
        	return super.performEditorAction(editorAction);
        }
        
    }
    
    private void onBackspaceKey() {
		if (getSelectionStart() == 0 && getSelectionEnd() == 0) {
			mOnChipCommitListener.deleteSelectedChip();
		}
	}
	
    /**
     * Search the current input for tags to commit.
     * @param forceCommit 
     * @return true if valid input, false if there were errors
     */
	private boolean checkForCommits(boolean forceCommit) {
		if (mCommitCharsArray == null || mCommitCharsArray.length == 0 || mOnChipCommitListener == null) {
			return true; // If no commit chars, then auto committing is not enabled.
		}
		
		Editable s = getText();
		
		int len = s.length();
		if (len == 0) {
			return true; 
		}
		
		// Note: If a user pasted input in, it is possible there are multiple commits to handle.
		
		String input = s.toString();
		String[] tags = StringUtils.splitPreserveAllTokens(input, mCommitChars);
		
		if (tags.length == 0) {
			return true;
		} else if (tags.length == 1 && !forceCommit) {
			return true;
		}
		
		ArrayList<CharSequence> toCommit = new ArrayList<CharSequence>(tags.length);
		ArrayList<String> toKeep = new ArrayList<String>(tags.length);
		ArrayList<String> errors = new ArrayList<String>();
		int i = 0;
		for (String tag : tags) {
			String trimmed = StringUtils.trimToEmpty(tag);
			if (trimmed.length() == 0) {
				continue;
			}
			
			if (i == tags.length-1 && !forceCommit) {
				toKeep.add(tag);
				continue;
			}
			
			String error = mOnChipCommitListener.validate(tag);
			if (error != null) {
				errors.add(error);
				toKeep.add(tag);
				continue;
			}
			
			toCommit.add(trimmed);
			i++;
		}
		
		if (toCommit.size() > 1) {
			mOnChipCommitListener.onChipsCommitted(toCommit);
		} else if (!toCommit.isEmpty()) {
			mOnChipCommitListener.onChipCommitted(toCommit.get(0));
		}
		
		if (toKeep.isEmpty()) {
			setTextWithoutWatcher(s, "");
		} else {
			StringBuilder newText = StringBuilders.get();
			for (String keep : toKeep) {
				if (newText.length() > 0) {
					newText.append(mCommitCharsArray[0]);
				}
				newText.append(keep);
			}
			setTextWithoutWatcher(s, newText.toString());
			setSelection(newText.length());
			StringBuilders.recycle(newText);
			
			if (!errors.isEmpty()) {
				mOnChipCommitListener.onInvalidInput(errors);
			}
		}
		
		return errors.isEmpty();
	}
	
	private void invokeDoneListener() {
		if (mOnInputDoneListener != null) {
			mOnInputDoneListener.onInputDone();
		}
	}
	
	/**
	 * Change the text without invoking our own text watcher.
	 * @param s
	 * @param text
	 */
	private void setTextWithoutWatcher(Editable s, CharSequence text) {
		mIsChangingText = true;
		s.clear();
		s.append(text);
		mIsChangingText = false;
	}
	
	/**
	 * An interface for managing auto-commiting of chips.
	 */
	public interface OnChipCommitListener {
		/** Validate the input and return if it can become a chip.
		 * <p>
		 * If valid, return null, if invalid, return error message to show to user.
		 */
		String validate(String tag);
		
		/** One chip was committed */
		void onChipCommitted(CharSequence text);
		
		/** Multiple chips were committed at once */
		void onChipsCommitted(ArrayList<CharSequence> text);
		
		/** It attempted to commit chips but some or all were invalid. */
		void onInvalidInput(ArrayList<String> errors);
		
		/** The user has pressed the delete key. If there is a selected chip, it should be removed */
		void deleteSelectedChip();
	}
	
}