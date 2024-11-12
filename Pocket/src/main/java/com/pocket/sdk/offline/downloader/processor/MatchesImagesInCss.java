package com.pocket.sdk.offline.downloader.processor;

import com.pocket.sdk.offline.cache.Asset;

import androidx.annotation.NonNull;

/**
 * Finds images in styles.
 * The original regex for this was defined as:
 * <pre>
 * {@code
 * Example Case: background-image: url("/awesome/image.jpg");
 * Regex: (background(?:-image)?:[^;}\(]*url\()(['"]?([^'"\(\)]*)['"]?)(\))
 * Capture 1:  background-image: url(
 * Capture 2:  "/awesome/image.jpg"
 * Capture 3:  /awesome/image.jpg
 * Capture 4:  )
 * }
 * </pre>
 */
class MatchesImagesInCss implements StreamingMarkupProcessor.Matcher {
	
	@Override
	public int type() {
		return Asset.IMAGE;
	}
	
	enum Step {
		OPEN,
		URL,
		PRE_CAPTURE,
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
	
	StreamingMarkupProcessor.Mode mode(Step step) {
		if (step == Step.CAPTURE) return StreamingMarkupProcessor.Mode.CAPTURING;
		if (step == Step.POST_CAPTURE) return StreamingMarkupProcessor.Mode.POST_CAPTURE_MATCHING;
		return StreamingMarkupProcessor.Mode.MATCHING;
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
				if (index == 0) return (codepoint == (int)'b' || codepoint == (int)'B') ? advanceIndex() : nope();
				if (index == 1) return (codepoint == (int)'a' || codepoint == (int)'A') ? advanceIndex() : nope();
				if (index == 2) return (codepoint == (int)'c' || codepoint == (int)'C') ? advanceIndex() : nope();
				if (index == 3) return (codepoint == (int)'k' || codepoint == (int)'K') ? advanceIndex() : nope();
				if (index == 4) return (codepoint == (int)'g' || codepoint == (int)'G') ? advanceIndex() : nope();
				if (index == 5) return (codepoint == (int)'r' || codepoint == (int)'R') ? advanceIndex() : nope();
				if (index == 6) return (codepoint == (int)'o' || codepoint == (int)'O') ? advanceIndex() : nope();
				if (index == 7) return (codepoint == (int)'u' || codepoint == (int)'U') ? advanceIndex() : nope();
				if (index == 8) return (codepoint == (int)'n' || codepoint == (int)'N') ? advanceIndex() : nope();
				if (index == 9) return (codepoint == (int)'d' || codepoint == (int)'D') ? advanceIndex() : nope();
				if (index == 10) {
					if (codepoint == (int)':') return advance(Step.URL);
					if (codepoint == (int)'-') return advanceIndex();
					return nope();
				}
				if (index == 11) return (codepoint == (int)'i' || codepoint == (int)'I') ? advanceIndex() : nope();
				if (index == 12) return (codepoint == (int)'m' || codepoint == (int)'M') ? advanceIndex() : nope();
				if (index == 13) return (codepoint == (int)'a' || codepoint == (int)'A') ? advanceIndex() : nope();
				if (index == 14) return (codepoint == (int)'g' || codepoint == (int)'G') ? advanceIndex() : nope();
				if (index == 15) return (codepoint == (int)'e' || codepoint == (int)'E') ? advanceIndex() : nope();
				if (index == 16 && codepoint == (int)':') return advance(Step.URL);
				return nope();
			case URL:
				if (codepoint == (int)';' || codepoint == (int)'}' || (index < 3 && codepoint == (int)'(')) return nope();
				if (index == 0) return (codepoint == (int)'u' || codepoint == (int)'U') ? advanceIndex() : advance(Step.URL);
				if (index == 1) return (codepoint == (int)'r' || codepoint == (int)'R') ? advanceIndex() : advance(Step.URL);
				if (index == 2) return (codepoint == (int)'l' || codepoint == (int)'L') ? advanceIndex() : advance(Step.URL);
				if (index == 3) return (codepoint == (int)'(') ? advance(Step.PRE_CAPTURE) : advance(Step.URL);
				return continuing();
			case PRE_CAPTURE:
				if (codepoint == (int)')') return nope();
				if (index == 0 && (codepoint == (int)'\'' || codepoint == (int)'"')) return advanceIndex();
				if (index == 1 && (codepoint == (int)'\'' || codepoint == (int)'"')) return nope(); // Empty
				return advance(Step.CAPTURE);
			case CAPTURE:
				if (codepoint == (int)')') return completed();
				if (codepoint == (int)'\'' || codepoint == (int)'"') return advance(Step.POST_CAPTURE);
				return continuing();
			case POST_CAPTURE:
				return codepoint == (int)')' ? completed() : nope();
		}
		throw new RuntimeException("unexpected state " + step + " " + index);
	}
	
	@NonNull
	@Override
	public String toString() {
		// Only intended for help debugging
		return getClass().getSimpleName() + " " + step + " " + index;
	}
}
