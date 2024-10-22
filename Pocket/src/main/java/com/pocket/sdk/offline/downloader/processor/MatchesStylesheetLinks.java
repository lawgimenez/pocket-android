package com.pocket.sdk.offline.downloader.processor;

import com.pocket.sdk.offline.cache.Asset;

import androidx.annotation.NonNull;

/**
 * Finds stylesheets referenced from html
 * The original regex for this was defined as:
 * <pre>
 * {@code
 * Example Case: <link type="text/css" rel="stylesheet" href="yay.css">
 * Regex: (<\s?link[\s\n][^>]*?href=["'])([^"']*)(["'][^>]*?>)
 * Capture 1:  <link type="text/css" href="
 * Capture 2:  yay.css
 * Capture 3:  "
 * }
 * </pre>
 * Also requires that there is a "stylesheet" word somewhere within the tag as well.
 */
class MatchesStylesheetLinks implements StreamingMarkupProcessor.Matcher {
	
	@Override
	public int type() {
		return Asset.STYLESHEET;
	}
	
	enum Step {
		OPEN,
		TAG,
		HREF,
		QUOTE,
		ABOUT_TO_CAPTURE,
		CAPTURE,
		POST_CAPTURE
	}
	
	Step step = Step.OPEN;
	int index = 0;
	int stylesheetIndex = 0;
	boolean foundStyleSheet = false;
	
	StreamingMarkupProcessor.Mode advance(Step step) {
		this.step = step;
		index = 0;
		return mode(step);
	}
	
	StreamingMarkupProcessor.Mode mode(Step step) {
		 if (step == Step.CAPTURE) return StreamingMarkupProcessor.Mode.CAPTURING;
		 if (step == Step.POST_CAPTURE) return StreamingMarkupProcessor.Mode.POST_CAPTURE_MATCHING;
		 return StreamingMarkupProcessor.Mode.MATCHING;
	}
	
	StreamingMarkupProcessor.Mode advanceIndex() {
		index++;
		stylesheetIndex = 0;
		return continuing();
	}
	
	boolean advanceStylesheetIndex() {
		index = 0;
		stylesheetIndex++;
		return true;
	}
	
	StreamingMarkupProcessor.Mode completed() {
		reset();
		return StreamingMarkupProcessor.Mode.MATCHED;
	}
	
	StreamingMarkupProcessor.Mode nope() {
		reset();
		return StreamingMarkupProcessor.Mode.NO_MATCH;
	}
	
	StreamingMarkupProcessor.Mode continuing() {
		return mode(step);
	}
	
	@Override
	public void reset() {
		step = Step.OPEN;
		index = 0;
		foundStyleSheet = false;
		stylesheetIndex = 0;
	}
	
	@Override
	public StreamingMarkupProcessor.Mode read(int codepoint) {
		switch (step) {
			case OPEN:
				if (codepoint == (int)'<') return advance(Step.TAG);
				return nope();
			case TAG:
				if (index == 0) {
					if (Character.isWhitespace(codepoint)) return continuing();
					return (codepoint == (int)'l' || codepoint == (int)'L') ? advanceIndex() : nope();
				}
				if (index == 1) return (codepoint == (int)'i' || codepoint == (int)'I') ? advanceIndex() : nope();
				if (index == 2) return (codepoint == (int)'n' || codepoint == (int)'N') ? advanceIndex() : nope();
				if (index == 3) return (codepoint == (int)'k' || codepoint == (int)'K') ? advance(Step.HREF) : nope();
				return nope();
			case HREF:
				if (codepoint == (int)'>') return nope();
				if (Character.isWhitespace(codepoint)) return continuing();
				if (lookForStylesheetWord(codepoint)) return continuing();
				if (index == 0) return (codepoint == (int)'h' || codepoint == (int)'H') ? advanceIndex() : continuing();
				if (index == 1) return (codepoint == (int)'r' || codepoint == (int)'R') ? advanceIndex() : advance(Step.HREF);
				if (index == 2) return (codepoint == (int)'e' || codepoint == (int)'E') ? advanceIndex() : advance(Step.HREF);
				if (index == 3) return (codepoint == (int)'f' || codepoint == (int)'F') ? advanceIndex() : advance(Step.HREF);
				if (index == 4) return (codepoint == (int)'=') ? advance(Step.QUOTE) : advance(Step.HREF);
				return continuing();
			case QUOTE:
				if (codepoint == (int)'\'' || codepoint == (int)'"') return advance(Step.ABOUT_TO_CAPTURE);
				return nope();
			case ABOUT_TO_CAPTURE:
				if (codepoint == (int)'\'' || codepoint == (int)'"') return nope(); // Empty path
				return advance(Step.CAPTURE);
			case CAPTURE:
				if (codepoint == (int)'\'' || codepoint == (int)'"') return foundStyleSheet ? completed() : advance(Step.POST_CAPTURE);
				return continuing();
			case POST_CAPTURE:
				if (codepoint == (int)'>') return foundStyleSheet ? completed() : nope();
				lookForStylesheetWord(codepoint);
				return continuing();
		}
		throw new RuntimeException("unexpected state " + step);
	}
	
	private boolean lookForStylesheetWord(int codepoint) {
		if (foundStyleSheet) return false;
		if (stylesheetIndex == 0) if (codepoint == (int)'s' || codepoint == (int)'S') return advanceStylesheetIndex();
		if (stylesheetIndex == 1) if (codepoint == (int)'t' || codepoint == (int)'T') return advanceStylesheetIndex();
		if (stylesheetIndex == 2) if (codepoint == (int)'y' || codepoint == (int)'Y') return advanceStylesheetIndex();
		if (stylesheetIndex == 3) if (codepoint == (int)'l' || codepoint == (int)'L') return advanceStylesheetIndex();
		if (stylesheetIndex == 4) if (codepoint == (int)'e' || codepoint == (int)'E') return advanceStylesheetIndex();
		if (stylesheetIndex == 5) if (codepoint == (int)'s' || codepoint == (int)'S') return advanceStylesheetIndex();
		if (stylesheetIndex == 6) if (codepoint == (int)'h' || codepoint == (int)'H') return advanceStylesheetIndex();
		if (stylesheetIndex == 7) if (codepoint == (int)'e' || codepoint == (int)'E') return advanceStylesheetIndex();
		if (stylesheetIndex == 8) if (codepoint == (int)'e' || codepoint == (int)'E') return advanceStylesheetIndex();
		if (stylesheetIndex == 9) if (codepoint == (int)'t' || codepoint == (int)'T') {
			foundStyleSheet = true;
			return true;
		}
		return false;
	}
	
	@NonNull
	@Override
	public String toString() {
		// Only intended for help debugging
		return getClass().getSimpleName() + " " + step + " " + index + " " + stylesheetIndex + " " + foundStyleSheet;
	}
}
