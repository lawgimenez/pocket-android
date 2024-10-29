package com.pocket.util.android;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * A no op implementation so you can override only the methods you will use. No need to call super methods as they do nothing (unless your class is a sub-sub-class).
 */
public abstract class SimpleTextWatcher implements TextWatcher {

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}

	@Override
	public void afterTextChanged(Editable s) {}
	
}
