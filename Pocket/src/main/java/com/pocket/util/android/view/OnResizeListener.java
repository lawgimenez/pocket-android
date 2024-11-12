package com.pocket.util.android.view;

import android.view.View;

public interface OnResizeListener {
	public void onViewSizeChanged(View v, int newWidth, int newHeight, int oldWidth, int oldHeight);
}
