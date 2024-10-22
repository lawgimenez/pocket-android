package com.pocket.ui.view.highlight;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineBackgroundSpan;
import android.text.style.UpdateAppearance;
import android.widget.TextView;

import com.pocket.ui.util.DimenUtil;

/**
 * Provides custom sizing of the highlighted or selection background color on text.
 * <p>
 * A typical {@link android.text.style.BackgroundColorSpan} only allows you to change the color,
 * not the size or positioning of the background rectangle. This gives finer control.
 * <p>
 * To use this class, you must apply this span to the entire length of the text, then
 * add {@link HighlightedRegion} spans to the areas to highlight.
 * You can use {@link #attach(Spannable, int, int)} as a convenience method.
 */
public class HighlightSpan extends LeadingMarginSpan.Standard implements LineBackgroundSpan {
	
	private final int sidePadding;
	private final float ascent;
	private final float descent;
	private final ColorStateList highlightColor;
	private final StateSource stateSource;
	private final MetricsSource metrics;
	private final RectF rect = new RectF();
	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	
	/**
	 * A span with default sizing and states for typical for Pocket view, but provided colors.
	 */
	public HighlightSpan(TextView view, ColorStateList color) {
		this(0, DimenUtil.dpToPx(view.getContext(), 1), DimenUtil.dpToPx(view.getContext(), 2), color, view::getDrawableState, view.getPaint()::getFontMetrics);
	}
	
	/**
	 * @param sidePadding padding of highlight to left and right
	 * @param ascent padding of highlight above the font's top
	 * @param descent padding of highlight below the font's baseline
	 * @param highlightColor The color
	 * @param stateSource Getter of the current state (used to determine the right color)
	 * @param metrics Getter of the current FontMetrics
	 */
	public HighlightSpan(int sidePadding,
			float ascent,
			float descent,
			ColorStateList highlightColor,
			StateSource stateSource,
			MetricsSource metrics) {
		super(sidePadding);
		this.ascent = ascent;
		this.sidePadding = sidePadding;
		this.descent = descent;
		this.highlightColor = highlightColor;
		this.stateSource = stateSource;
		this.metrics = metrics;
		
		paint.setStyle(Paint.Style.FILL);
	}
	
	/**
	 * Add this span to the spannable. It will add this span covering the entire
	 * spannable, and create a {@link HighlightedRegion} span over the region to highlight.
	 *
	 * @param spannable Where to add it.
	 * @param startInclusive The first character of the region to highlight.
	 * @param endExclusive 1 past the last character to highlight.
	 */
	public void attach(Spannable spannable, int startInclusive, int endExclusive)  {
		spannable.setSpan(new HighlightedRegion(), startInclusive, endExclusive, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		spannable.setSpan(this, 0, spannable.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
	}
	
	@Override
	public void drawBackground(Canvas c,
			Paint p,
			int left,
			int right,
			int top,
			int baseline,
			int bottom,
			CharSequence text,
			int start,
			int end,
			int lnum) {
		Spanned spanned = text instanceof Spanned ? (Spanned) text : null;
		if (spanned == null) {
			// Not expected, but if the framework changes let's not blow up the app
			return;
		}
		
		HighlightedRegion[] highlights = spanned.getSpans(start, end, HighlightedRegion.class);
		for (HighlightedRegion highlight : highlights) {
			// Determine the left and right edge of the highlight to draw, it may extend outside of this line
			int spanStart = spanned.getSpanStart(highlight);
			int spanEnd = spanned.getSpanEnd(highlight);
			if (spanStart > end || spanEnd < start)  {
				// Not in this line
				continue;
			} else if (spanStart <= start) {
				// Can draw to edge
				rect.left = left;
			} else {
				// Need to figure out how much to inset
				rect.left = left + p.measureText(text, start, spanStart);
			}
			rect.left -= sidePadding;
			
			rect.right = rect.left
					+ p.measureText(text,
						Math.max(spanStart, start), // Either the beginning of the line or the span
						Math.min(spanEnd, end))  // Either the end of the line or the span
					+  sidePadding*2;
			
			Paint.FontMetrics fm = metrics.metrics();
			rect.top = baseline + fm.ascent - ascent;
			rect.bottom = baseline + descent;
			
			// TODO could consider expanding the rect each iteration and then draw once at the end
			paint.setColor(highlightColor.getColorForState(stateSource.getDrawableState(), Color.TRANSPARENT));
			c.drawRect(rect, paint);
		}
	}
	
	/**
	 * Remove all {@link HighlightSpan} and {@link HighlightedRegion} spans from this text.
	 * @param text
	 */
	public static void removeAll(Spannable text) {
		if (TextUtils.isEmpty(text)) {
			return;
		}
		for (Object o : text.getSpans(0, text.length(), HighlightSpan.class)) {
			text.removeSpan(o);
		}
		for (Object o : text.getSpans(0, text.length(), HighlightedRegion.class)) {
			text.removeSpan(o);
		}
	}
	
	public interface StateSource {
		int[] getDrawableState();
	}
	
	public interface MetricsSource {
		Paint.FontMetrics metrics();
	}
	
	public static class HighlightedRegion implements UpdateAppearance {}
	
}
