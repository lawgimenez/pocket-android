package com.pocket.sdk.offline.downloader.processor;

import com.pocket.sdk.offline.cache.Asset;

import androidx.annotation.NonNull;

/**
 * Finds stylesheets imported in other stylesheets.
 * The original regex for this was defined as:
 * <pre>
 * {@code
 * Regex: (@import\s*(?:url\()?)(['"]?([^'"\(\)]*)['"]?)(\))?
 * Example Case: @import url('/something/')
 * Example Case: @import '/something/'
 * Example Case: @import /something/
 * Capture 1:  @import url(
 * Capture 2:  '/something/'
 * Capture 3:  /something/
 * Capture 4:  )
 * }
 * </pre>
 */
class MatchesCssImportStatements implements StreamingMarkupProcessor.Matcher {
	
	@Override
	public int type() {
		return Asset.STYLESHEET;
	}
	
	enum Step {
		OPEN,
		/** In the tricky phase it could be whitespace, the actual path, a opening quote or an opening url( (which could also have its own optional opening quote) */
		CAPTURE_MAYBE,
		/** The next point should be the quote, or the start of the capture. */
		QUOTE_MAYBE,
		/** The next point should be the start of the capture. */
		CAPTURE_READY,
		CAPTURE_RESTART,
		/** We are in a capture */
		CAPTURE
	}
	
	Step step = Step.OPEN;
	int index = 0;
	
	StreamingMarkupProcessor.Mode advance(Step step) {
		this.step = step;
		index = 0;
		return mode(step);
	}
	
	StreamingMarkupProcessor.Mode advanceIndex() {
		index++;
		return continuing();
	}
	
	StreamingMarkupProcessor.Mode completed() {
		reset();
		return StreamingMarkupProcessor.Mode.MATCHED;
	}
	
	StreamingMarkupProcessor.Mode nope() {
		reset();
		return StreamingMarkupProcessor.Mode.NO_MATCH;
	}
	
	StreamingMarkupProcessor.Mode mode(Step step) {
		if (step == Step.CAPTURE) return StreamingMarkupProcessor.Mode.CAPTURING;
		if (step == Step.CAPTURE_MAYBE) return StreamingMarkupProcessor.Mode.CAPTURING;
		if (step == Step.CAPTURE_READY) return StreamingMarkupProcessor.Mode.NO_MATCH;
		if (step == Step.QUOTE_MAYBE) return StreamingMarkupProcessor.Mode.NO_MATCH;
		return StreamingMarkupProcessor.Mode.MATCHING;
	}
	
	StreamingMarkupProcessor.Mode continuing() {
		return mode(step);
	}
	
	@Override
	public void reset() {
		step = Step.OPEN;
		index = 0;
	}
	
	@Override
	public StreamingMarkupProcessor.Mode read(int codepoint) {
		switch (step) {
			case OPEN:
				if (index == 0) return (codepoint == (int)'@') ? advanceIndex() : nope();
				if (index == 1) return (codepoint == (int)'i' || codepoint == (int)'I') ? advanceIndex() : nope();
				if (index == 2) return (codepoint == (int)'m' || codepoint == (int)'M') ? advanceIndex() : nope();
				if (index == 3) return (codepoint == (int)'p' || codepoint == (int)'P') ? advanceIndex() : nope();
				if (index == 4) return (codepoint == (int)'o' || codepoint == (int)'O') ? advanceIndex() : nope();
				if (index == 5) return (codepoint == (int)'r' || codepoint == (int)'R') ? advanceIndex() : nope();
				if (index == 6) return (codepoint == (int)'t' || codepoint == (int)'T') ? advance(Step.CAPTURE_MAYBE) : nope();
				return nope();
			case CAPTURE_MAYBE:
				if (index == 0) {
					if (Character.isWhitespace(codepoint)) return continuing();
					if (codepoint == (int)'u' || codepoint == (int)'U') return advanceIndex();
					if (codepoint == (int)'\'' || codepoint == (int)'"') return advance(Step.CAPTURE_READY);
					return isValidCaptureChar(codepoint) ? advance(Step.CAPTURE) : nope();
				}
				if (Character.isWhitespace(codepoint) || codepoint == (int)';') return completed();
				if (index == 1 && codepoint == (int)'r' || codepoint == (int)'R') return advanceIndex();
				if (index == 2 && codepoint == (int)'l' || codepoint == (int)'L') return advanceIndex();
				if (index == 3 && codepoint == (int)'(') return advance(Step.QUOTE_MAYBE);
				return isValidCaptureChar(codepoint) ? advance(Step.CAPTURE) : nope();
			case QUOTE_MAYBE:
				if (codepoint == (int)'\'' || codepoint == (int)'"') return advance(Step.CAPTURE_READY);
				return isValidCaptureChar(codepoint) ? advance(Step.CAPTURE) : nope();
			case CAPTURE_READY:
				return isValidCaptureChar(codepoint) ? advance(Step.CAPTURE) : nope();
			case CAPTURE:
				if (codepoint == (int)'\'' || codepoint == (int)'"' || codepoint == (int)';') return completed();
				return isValidCaptureChar(codepoint) ? continuing() : nope();
		}
		throw new RuntimeException("unexpected state " + step + " " + index);
	}
	
	private boolean isValidCaptureChar(int codepoint) {
		return codepoint != (int)')' && codepoint != (int)'('; /// but really... most symbols?
	}
	
	@NonNull
	@Override
	public String toString() {
		// Only intended for help debugging
		return getClass().getSimpleName() + " " + step + " " + index;
	}
	
}
