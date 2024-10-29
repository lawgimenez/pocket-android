package com.pocket.ui.text;

import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.widget.TextView;

public class TextViewUtil {

	/**
	 * We've seen a number of text duplication bugs from third party keyboards, including Galaxy, Fleksy, and Kika from
	 * our original implementation of a lower casing {@link android.text.InputFilter}.
	 *
	 * This version is an adaptation of {@link android.text.InputFilter.AllCaps}, modified to do the opposite, which is
	 * to lower case the text, while preserving text spans.
	 */
	public static class AllLowerCase implements InputFilter {
		@Override
		public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
			final CharSequence wrapper = new CharSequenceWrapper(source, start, end);
			boolean upperOrTitleFound = false;
			final int length = end - start;
			for (int i = 0, cp; i < length; i += Character.charCount(cp)) {
				// We access 'wrapper' instead of 'source' to make sure no code unit beyond 'end' is
				// ever accessed.
				cp = Character.codePointAt(wrapper, i);
				if (Character.isUpperCase(cp) || Character.isTitleCase(cp)) {
					upperOrTitleFound = true;
					break;
				}
			}

			if (!upperOrTitleFound) {
				return null; // keep original
			}

			// lower case the text
			final CharSequence lower = wrapper.toString().toLowerCase();

			if (lower.toString().equals(wrapper.toString())) {
				return null; // Nothing was changed in the lowercase operation, keep original
			} else {
				if (source instanceof Spanned) {
					// copy spans
					SpannableString spannable = new SpannableString(lower);
					TextUtils.copySpansFrom((Spanned) source, start, end, null, spannable, 0);
					return spannable;
				} else {
					return lower;
				}
			}
		}
	}

	/**
	 * Copy / pasted from {@link android.text.InputFilter.AllCaps}, for use in {@link AllLowerCase}
	 */
	private static class CharSequenceWrapper implements CharSequence, Spanned {
		private final CharSequence mSource;
		private final int mStart, mEnd;
		private final int mLength;

		CharSequenceWrapper(CharSequence source, int start, int end) {
			mSource = source;
			mStart = start;
			mEnd = end;
			mLength = end - start;
		}

		public int length() {
			return mLength;
		}

		public char charAt(int index) {
			if (index < 0 || index >= mLength) {
				throw new IndexOutOfBoundsException();
			}
			return mSource.charAt(mStart + index);
		}

		public CharSequence subSequence(int start, int end) {
			if (start < 0 || end < 0 || end > mLength || start > end) {
				throw new IndexOutOfBoundsException();
			}
			return new CharSequenceWrapper(mSource, mStart + start, mStart + end);
		}

		public String toString() {
			return mSource.subSequence(mStart, mEnd).toString();
		}

		public <T> T[] getSpans(int start, int end, Class<T> type) {
			return ((Spanned) mSource).getSpans(mStart + start, mStart + end, type);
		}

		public int getSpanStart(Object tag) {
			return ((Spanned) mSource).getSpanStart(tag) - mStart;
		}

		public int getSpanEnd(Object tag) {
			return ((Spanned) mSource).getSpanEnd(tag) - mStart;
		}

		public int getSpanFlags(Object tag) {
			return ((Spanned) mSource).getSpanFlags(tag);
		}

		public int nextSpanTransition(int start, int limit, Class type) {
			return ((Spanned) mSource).nextSpanTransition(mStart + start, mStart + limit, type)
					- mStart;
		}
	}

	/**
	 * Set padding so that is visually spaced from edges of the text.
	 * TODO test how this works with accented languages
	 *
	 * @param textView
	 * @param horizontalPaddingResId
	 * @param verticalPaddingResId
	 */
	public static void setVisualTextPadding(TextView textView, int verticalPaddingResId, int horizontalPaddingResId) {
		// Tweak internal padding to visually look like Pocket's common space dimensions
		Resources res = textView.getResources();
		Paint.FontMetrics fm = textView.getPaint().getFontMetrics();
		int vert = res.getDimensionPixelSize(verticalPaddingResId);
		int hori = res.getDimensionPixelSize(horizontalPaddingResId);
		textView.setPadding(hori, (int) (vert + fm.top - fm.ascent),
				hori, (int) (vert - fm.descent));
	}
	
	/**
	 * Set padding so that is visually spaced from edges of the text.
	 */
	public static void setVisualTextPadding(TextView textView, int left, int top, int right, int bottom) {
		// Tweak internal padding to visually look like Pocket's common space dimensions
		Resources res = textView.getResources();
		Paint.FontMetrics fm = textView.getPaint().getFontMetrics();
		textView.setPadding(
				left,
				(int) Math.max(0, top + fm.top - fm.ascent),
				right,
				(int) Math.max(0, (bottom - fm.descent)));
	}
	
	/**
	 * Modify the padding of this text view so the ascent and descent are visually equal.
	 * This changes top and bottom padding. Left and right padding are not changed.
	 */
	public static void verticallyCenterPadding(TextView textView) {
		int ascent = (int) Math.ceil(ascent(textView));
		int descent = (int) Math.ceil(descent(textView));
		int top;
		int bottom;
		if (ascent > descent) {
			top = 0;
			bottom = ascent - descent;
		} else {
			top = descent - ascent;
			bottom = 0;
		}
		textView.setPadding(
				textView.getPaddingLeft(),
				top,
				textView.getPaddingRight(),
				bottom);
	}

	public static float descent(TextView view) {
		return view.getPaint().getFontMetrics().descent;
	}
	
	public static float bottom(TextView view) {
		return view.getPaint().getFontMetrics().bottom;
	}
	
	public static float ascent(TextView view) {
		Paint.FontMetrics fm = view.getPaint().getFontMetrics();
		return Math.abs(fm.ascent - fm.top);
	}

	/**
	 * Gets the total height a TextView will have on screen.
	 *
	 * @param paint a {@link TextPaint} object, which should include the TextView's {@link Typeface} and text size.
	 * @param text The text that will be placed in the TextView.
	 * @param alignment The text alignment.
	 * @param viewWidthPx The width of the TextView on the screen.
	 * @param lineHeightPx The line height of the TextView.
	 * @return the total height in pixels a TextView with the given parameters will cover.
	 */
	public static float getExpectedTextViewHeight(TextPaint paint, CharSequence text, Layout.Alignment alignment, int viewWidthPx, float lineHeightPx) {
		Paint.FontMetrics metrics = paint.getFontMetrics();
		float fontHeightPx = metrics.descent - metrics.ascent;
		return new StaticLayout(text, paint, viewWidthPx, alignment, 1, lineHeightPx - fontHeightPx, true).getHeight();
	}
}
