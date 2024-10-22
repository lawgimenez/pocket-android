package com.pocket.ui.view.edittext;

import android.content.Context;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.pocket.ui.R;
import com.pocket.ui.view.themed.ThemedLinearLayout;

public class CharCountEditText extends ThemedLinearLayout {

    private final Binder binder = new Binder();

    private EditText editText;
    private CharCounter charCount;

    public CharCountEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CharCountEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public CharCountEditText(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.view_char_count_edittext, this);
        setOrientation(VERTICAL);
        editText = findViewById(R.id.edit_text);
        charCount = findViewById(R.id.char_count);
    }

    public void showKeyboard() {
        requestFocus();
        ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    public void hideKeyboard() {
        ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    public EditText getEditText() {
        return editText;
    }

    public Binder bind() {
        return binder;
    }

    public class Binder {

        private TextWatcher watcher;

        public Binder clear() {
            editText.setText(null);
            charCount.bind().clear();
            textChanged(null);
            return this;
        }

        public Binder maxLength(int length) {
            charCount.bind().watchText(editText, length);
            return this;
        }

        public Binder textChanged(TextWatcher w) {
            editText.removeTextChangedListener(watcher);
            watcher = w;
            editText.addTextChangedListener(watcher);
            return this;
        }
    }
}
