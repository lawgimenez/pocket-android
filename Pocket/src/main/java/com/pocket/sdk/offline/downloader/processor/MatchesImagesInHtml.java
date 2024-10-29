package com.pocket.sdk.offline.downloader.processor;

import com.pocket.sdk.offline.cache.Asset;

import androidx.annotation.NonNull;

/**
 * Finds images in html tags
 * The original regex for this was defined as:
 * <pre>
 * {@code
 * Example Case: <img src="something.jpg" alt="awesome" />
 * Regex: (<\s?(?:img|input|table|td)[\s\n]*(?:[^>]*)?(?:src|background)=["'])([^"']*)(["'])
 * Capture 1:  <img src="
 * Capture 2:  something.jpg
 * Capture 3:  "
 * }
 * </pre>
 */
class MatchesImagesInHtml implements StreamingMarkupProcessor.Matcher {
	
	@Override
	public int type() {
		return Asset.IMAGE;
	}
	
	enum Step {
		OPEN,
		TAG,
		TAG_I,
		TAG_IMG,
		TAG_INPUT,
		TAG_T,
		TAG_TABLE,
		SKIPPING,
		ATTR_SRC,
		ATTR_BACKGROUND,
		EQUAL,
		QUOTE,
		ABOUT_TO_CAPTURE,
		CAPTURE,
		POST_CAPTURE
	}
	
	Step step = Step.OPEN;
	int index = 0;
	
	StreamingMarkupProcessor.Mode advance(Step step) {
		this.step = step;
		index = 0;
		return mode(step);
	}
	
	private StreamingMarkupProcessor.Mode mode(Step step) {
		return step == Step.CAPTURE ? StreamingMarkupProcessor.Mode.CAPTURING : StreamingMarkupProcessor.Mode.MATCHING;
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
				if (codepoint == (int)'<') return advance(Step.TAG);
			case TAG:
				if (Character.isWhitespace(codepoint)) return continuing();
				if (codepoint == (int)'i' || codepoint == (int)'I') return advance(Step.TAG_I);
				if (codepoint == (int)'t' || codepoint == (int)'T') return advance(Step.TAG_T);
				return nope();
			case TAG_I:
				if (codepoint == (int)'m' || codepoint == (int)'M') return advance(Step.TAG_IMG);
				if (codepoint == (int)'n' || codepoint == (int)'N') return advance(Step.TAG_INPUT);
				return nope();
			case TAG_IMG:
				if (codepoint == (int)'g' || codepoint == (int)'G') return advance(Step.SKIPPING);
				return nope();
			case TAG_INPUT:
				if (index == 0) return (codepoint == (int)'p' || codepoint == (int)'P') ? advanceIndex() : nope();
				if (index == 1) return (codepoint == (int)'u' || codepoint == (int)'U') ? advanceIndex() : nope();
				if (index == 2) return (codepoint == (int)'t' || codepoint == (int)'T') ? advance(Step.SKIPPING) : nope();
				return nope();
			case TAG_T:
				if (codepoint == (int)'a' || codepoint == (int)'A') return advance(Step.TAG_TABLE);
				if (codepoint == (int)'d' || codepoint == (int)'D') return advance(Step.SKIPPING);
				return nope();
			case TAG_TABLE:
				if (index == 0) return (codepoint == (int)'b' || codepoint == (int)'B') ? advanceIndex() : nope();
				if (index == 1) return (codepoint == (int)'l' || codepoint == (int)'L') ? advanceIndex() : nope();
				if (index == 2) return (codepoint == (int)'e' || codepoint == (int)'E') ? advance(Step.SKIPPING) : nope();
				return nope();
			case SKIPPING:
				return skipping(codepoint);
			case ATTR_SRC:
				if (index == 0) return (codepoint == (int)'r' || codepoint == (int)'R') ? advanceIndex() : skipping(codepoint);
				if (index == 1) return (codepoint == (int)'c' || codepoint == (int)'C') ? advance(Step.EQUAL) : skipping(codepoint);
				return skipping(codepoint);
			case ATTR_BACKGROUND:
				if (index == 0) return (codepoint == (int)'a' || codepoint == (int)'A') ? advanceIndex() : skipping(codepoint);
				if (index == 1) return (codepoint == (int)'c' || codepoint == (int)'C') ? advanceIndex() : skipping(codepoint);
				if (index == 2) return (codepoint == (int)'k' || codepoint == (int)'K') ? advanceIndex() : skipping(codepoint);
				if (index == 3) return (codepoint == (int)'g' || codepoint == (int)'G') ? advanceIndex() : skipping(codepoint);
				if (index == 4) return (codepoint == (int)'r' || codepoint == (int)'R') ? advanceIndex() : skipping(codepoint);
				if (index == 5) return (codepoint == (int)'o' || codepoint == (int)'O') ? advanceIndex() : skipping(codepoint);
				if (index == 6) return (codepoint == (int)'u' || codepoint == (int)'U') ? advanceIndex() : skipping(codepoint);
				if (index == 7) return (codepoint == (int)'n' || codepoint == (int)'H') ? advanceIndex() : skipping(codepoint);
				if (index == 8) return (codepoint == (int)'d' || codepoint == (int)'D') ? advance(Step.EQUAL) : skipping(codepoint);
				return skipping(codepoint);
			case EQUAL:
				if (codepoint == (int)'=') return advance(Step.QUOTE);
				return skipping(codepoint);
			case QUOTE:
				if (codepoint == (int)'\'' || codepoint == (int)'"') return advance(Step.ABOUT_TO_CAPTURE);
				return skipping(codepoint);
			case ABOUT_TO_CAPTURE:
				if (codepoint == (int)'\'' || codepoint == (int)'"') return nope(); // Empty path
				return advance(Step.CAPTURE);
			case CAPTURE:
				if (codepoint == (int)'\'' || codepoint == (int)'"') return completed();
				return continuing();
		}
		throw new RuntimeException("unexpected state " + step);
	}
	
	private StreamingMarkupProcessor.Mode skipping(int codepoint) {
		if (codepoint == (int)'s' || codepoint == (int)'S') return advance(Step.ATTR_SRC);
		if (codepoint == (int)'b' || codepoint == (int)'B') return advance(Step.ATTR_BACKGROUND);
		return codepoint == (int)'>' ? nope() : advance(Step.SKIPPING);
	}
	
	@NonNull
	@Override
	public String toString() {
		// Only intended for help debugging
		return getClass().getSimpleName() + " " + step + " " + index;
	}
}
