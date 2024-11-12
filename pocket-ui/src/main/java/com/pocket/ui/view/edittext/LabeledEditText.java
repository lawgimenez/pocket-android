package com.pocket.ui.view.edittext;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.pocket.ui.R;
import com.pocket.ui.text.Fonts;
import com.pocket.ui.text.TextViewUtil;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.util.NestedColorStateList;
import com.pocket.ui.view.themed.ThemedEditText;
import com.pocket.ui.view.themed.ThemedTextInputLayout;
import com.pocket.ui.view.visualmargin.VisualMargin;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.view.ViewCompat;

public class LabeledEditText extends ThemedTextInputLayout implements VisualMargin {

    private final Binder binder = new Binder();

    private View line;
    private EditText editText;

    public LabeledEditText(Context context) {
        super(context);
        init(context, null);
    }

    public LabeledEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public LabeledEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        setHintTextAppearance(R.style.Pkt_TextFloatingLabelAppearance);

        editText = new ThemedEditText(new ContextThemeWrapper(context, R.style.Pkt_EditTextAppearance), attrs);

        // The editText inherits the id of its parent LabeledEditText via passing attrs in its creation, which can cause duplicate id crashes.
        // Here we set it back to its default id-less state.
        editText.setId(View.NO_ID);

        addView(editText);

        // set the typeface of the floating label
        setTypeface(Fonts.get(context, Fonts.Font.GRAPHIK_LCG_MEDIUM));

        // add our own horizontal line
        setOrientation(VERTICAL);
        line = new View(context);
        line.setBackgroundColor(getResources().getColor(R.color.pkt_themed_grey_5));
        ViewCompat.setBackgroundTintList(line, NestedColorStateList.get(context, R.color.pkt_edittext_underline));
        line.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DimenUtil.dpToPxInt(context, 1)));
        addView(line);

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.LabeledEditText);

            setHintLabel(ta.getText(R.styleable.LabeledEditText_android_hint));

            int inputType = ta.getInt(R.styleable.LabeledEditText_android_inputType, EditorInfo.TYPE_TEXT_VARIATION_NORMAL);
            editText.setInputType(inputType);
            // fix inputType:password changing font to monospace
            if (inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                editText.setTypeface(Typeface.DEFAULT);
            }
            binder.underline(ta.getBoolean(R.styleable.LabeledEditText_underLine, true));

            editText.setCompoundDrawablesWithIntrinsicBounds(null, null, ta.getDrawable(R.styleable.LabeledEditText_android_drawableRight), null);
            ta.recycle();
        }
    }

    private void setHintLabel(CharSequence hint) {
        setHint(hint);
        editText.setHint(null); // remove the hint that gets propagated to the EditText
    }

    private void setErrorColors(boolean error) {
        line.setActivated(error);
        editText.setActivated(error);
        invalidate();
    }
    
    public boolean isErrorState() {
        return editText.isActivated();
    }
    
    @Override
    public void setOnFocusChangeListener(OnFocusChangeListener l) {
        // Respond to the focus change of the internal edit text, and when returning the view, return this view not, the internal edit text.
        editText.setOnFocusChangeListener(l != null ? (OnFocusChangeListener) (v, hasFocus) -> l.onFocusChange(LabeledEditText.this, hasFocus) : null);
    }
    
    public Binder bind() {
        return binder;
    }

    public class Binder {

        public Binder clear() {
            label(null);
            text(null);
            underline(true);
            setErrorColors(false);
            return this;
        }

        public Binder label(CharSequence label) {
            setHintLabel(label);
            return this;
        }

        public Binder text(CharSequence text) {
            editText.setText(text);
            return this;
        }

        public Binder underline(boolean show) {
            line.setVisibility(show ? View.VISIBLE : View.GONE);
            return this;
        }

        public Binder errorState(boolean error) {
            setErrorColors(error);
            return this;
        }

    }
    
    @Override
    public int visualAscent() {
        return (int) Math.ceil(TextViewUtil.ascent(editText));
    }
    
    @Override
    public int visualDescent() {
        return 0;
    }
    
    @Override
    public boolean prepareVisualAscent() {
        return false;
    }
    
    @Override
    public boolean prepareVisualDescent() {
        return false;
    }

}
