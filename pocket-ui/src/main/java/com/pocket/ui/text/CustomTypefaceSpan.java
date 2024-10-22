package com.pocket.ui.text;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;

public class CustomTypefaceSpan extends TypefaceSpan {

	private final Typeface mNewType;
	private final boolean mFakeEffectsEnabled;

	/**
	 * {@link #CustomTypefaceSpan(String, Typeface, boolean)} with "" and true for other params.
	 */
	public CustomTypefaceSpan(Typeface type) {
		this("", type);
	}
	
	/**
	 * {@link #CustomTypefaceSpan(String, Typeface, boolean)} with "" for family
	 */
	public CustomTypefaceSpan(Typeface type, boolean allowFakeEffects) {
		this("", type, allowFakeEffects);
	}
	
	/**
	 * {@link #CustomTypefaceSpan(String, Typeface, boolean)} true for allowFakeEffects
	 */
	public CustomTypefaceSpan(String family, Typeface type) {
		this(family, type, true);
	}
	
	/**
	 * @param family @see {@link TypefaceSpan}
	 * @param type The typeface to apply to this span
	 * @param allowFakeEffects true if fake bold and italics are allowed. If you are supplying a variant typeface like an italic or bold font you likely want this set to false.
	 */
	public CustomTypefaceSpan(String family, Typeface type, boolean allowFakeEffects) {
		super(family);
		mNewType = type;
		mFakeEffectsEnabled = allowFakeEffects;
	}

	@Override
	public void updateDrawState(TextPaint ds) {
		applyCustomTypeFace(ds, mNewType, mFakeEffectsEnabled);
	}

	@Override
	public void updateMeasureState(TextPaint paint) {
		applyCustomTypeFace(paint, mNewType, mFakeEffectsEnabled);
	}
	
	private static void applyCustomTypeFace(Paint paint, Typeface tf, boolean fakeEffectsEnabled) {
		if (fakeEffectsEnabled) {
			int oldStyle;
			Typeface old = paint.getTypeface();
			if (old == null) {
				oldStyle = 0;
			} else {
				oldStyle = old.getStyle();
			}
	
			int fake = oldStyle & ~tf.getStyle();
			if ((fake & Typeface.BOLD) != 0) {
				paint.setFakeBoldText(true);
			}
	
			if ((fake & Typeface.ITALIC) != 0) {
				paint.setTextSkewX(-0.25f);
			}
		}

		paint.setTypeface(tf);
	}
}
