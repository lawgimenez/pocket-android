package com.pocket.ui.view;

import android.view.View;

public interface OnResizeListener {
    void onViewSizeChanged(View v, int newWidth, int newHeight, int oldWidth, int oldHeight);
}
