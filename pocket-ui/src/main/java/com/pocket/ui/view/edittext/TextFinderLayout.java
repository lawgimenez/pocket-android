package com.pocket.ui.view.edittext;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * An interface used to denote the essential components of a text finder or "find text in page" type view,
 * common on WebView / browser implementations.
 */
public interface TextFinderLayout {

    View root();
    View cancel();
    EditText input();
    TextView count();
    View back();
    View forward();

}
