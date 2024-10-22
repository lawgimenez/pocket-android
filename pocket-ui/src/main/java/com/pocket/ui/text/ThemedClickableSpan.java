package com.pocket.ui.text;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.TextPaint;
import android.text.style.ClickableSpan;

import androidx.annotation.NonNull;

public abstract class ThemedClickableSpan extends ClickableSpan {

    public interface StateSource {
        int[] getDrawableState();
    }

    private ColorStateList colorStateList;
    private StateSource stateSource;

    public ThemedClickableSpan(@NonNull ColorStateList colors, @NonNull StateSource source) {
        colorStateList = colors;
        stateSource = source;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        ds.setUnderlineText(false);
        ds.setColor(colorStateList.getColorForState(stateSource.getDrawableState(), Color.TRANSPARENT));
    }
}
