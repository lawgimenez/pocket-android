package com.pocket.ui.view.edittext;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.TextView;

import com.pocket.ui.R;
import com.pocket.ui.util.NestedColorStateList;
import com.pocket.ui.view.themed.ThemedTextView;

public class CharCounter extends ThemedTextView {

    private final Binder binder = new Binder();

    private int maxLength;
    private TextView watchedText;

    private TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            //
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //
        }

        @Override
        public void afterTextChanged(Editable s) {
            final int length = watchedText == null ? 0 : watchedText.getText().length();
            setText(getResources().getString(R.string.quantity_count, length, maxLength));
            setTextColor(NestedColorStateList.get(getContext(), length == maxLength ? R.color.pkt_themed_apricot_1 : R.color.pkt_themed_grey_3));
        }
    };

    public CharCounter(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public CharCounter(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CharCounter(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        setTextAppearance(context, R.style.Pkt_Text_Teeny_Tiny_Light);
    }

    public Binder bind() {
        return binder;
    }

    public class Binder {

        public Binder clear() {
            setText(null);
            watchText(null, 0);
            return this;
        }

        public Binder watchText(TextView tv, int max) {
            // unbind the last view
            if (watchedText != null) {
                watchedText.removeTextChangedListener(watcher);
                watchedText = null;
            }
            // bind this one
            if (tv != null) {
                watchedText = tv;
                watchedText.addTextChangedListener(watcher);
                tv.setFilters(new InputFilter[]{new InputFilter.LengthFilter(max)});
            }
            maxLength = max;
            watcher.afterTextChanged(null);
            return this;
        }
    }
}
