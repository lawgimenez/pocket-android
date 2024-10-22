package com.pocket.sdk.offline.downloader.processor;

import com.pocket.sdk.offline.cache.Asset;

import androidx.annotation.NonNull;

/**
 * Used to find any absolute links that weren't otherwise matched and clean them up.
 * The original regex for this was defined as:
 * <pre>
 * {@code
 * Regex: ([\s\"\'](?:background|src)=[\"\'])(https?:[^\"\']*)([\"\'])
 * Example Case:  src="https://getpocket.com/image.png"
 * </pre>
 */
class MatchesCleanup implements StreamingMarkupProcessor.Matcher {
	
	@Override
	public int type() {
		return Asset.STYLESHEET;
	}
	
	enum Step {
		OPEN,
		BACKGROUND,
		SRC,
		EQUAL,
		QUOTE,
		PENDING_HTTP,
		HTTP,
		URL
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
		return step == Step.HTTP || step == Step.URL ? StreamingMarkupProcessor.Mode.CAPTURING : StreamingMarkupProcessor.Mode.MATCHING;
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
				if (index == 0) return (Character.isWhitespace(codepoint)) ? advanceIndex() : nope();
				if (index == 1) {
					if (codepoint == (int)'b' || codepoint == (int)'B') return advance(Step.BACKGROUND);
					if (codepoint == (int)'s' || codepoint == (int)'S') return advance(Step.SRC);
				}
				return nope();
			case SRC:
				if (index == 0) return (codepoint == (int)'r' || codepoint == (int)'R') ? advanceIndex(): nope();
				if (index == 1) return (codepoint == (int)'c' || codepoint == (int)'C') ? advance(Step.EQUAL): nope();
				return nope();
			case BACKGROUND:
				if (index == 0) return (codepoint == (int)'a' || codepoint == (int)'A') ? advanceIndex(): nope();
				if (index == 1) return (codepoint == (int)'c' || codepoint == (int)'C') ? advanceIndex(): nope();
				if (index == 2) return (codepoint == (int)'k' || codepoint == (int)'K') ? advanceIndex(): nope();
				if (index == 3) return (codepoint == (int)'g' || codepoint == (int)'G') ? advanceIndex(): nope();
				if (index == 4) return (codepoint == (int)'r' || codepoint == (int)'R') ? advanceIndex(): nope();
				if (index == 5) return (codepoint == (int)'o' || codepoint == (int)'O') ? advanceIndex(): nope();
				if (index == 6) return (codepoint == (int)'u' || codepoint == (int)'U') ? advanceIndex(): nope();
				if (index == 7) return (codepoint == (int)'n' || codepoint == (int)'N') ? advanceIndex(): nope();
				if (index == 8) return (codepoint == (int)'d' || codepoint == (int)'D') ? advance(Step.EQUAL): nope();
				return nope();
			case EQUAL:
				return (codepoint == (int)'=') ? advance(Step.QUOTE) : nope();
			case QUOTE:
				return (codepoint == (int)'\'' || codepoint == (int)'"') ? advance(Step.PENDING_HTTP) : nope();
			case PENDING_HTTP:
				return (codepoint == (int)'h' || codepoint == (int)'H') ? advance(Step.HTTP) : nope();
			case HTTP:
				if (index <= 1) return (codepoint == (int)'t' || codepoint == (int)'T') ? advanceIndex(): nope();
				if (index == 2) return (codepoint == (int)'p' || codepoint == (int)'P') ? advanceIndex(): nope();
				if (index == 3 && (codepoint == (int)'s' || codepoint == (int)'S')) return advanceIndex();
				return codepoint == (int)':' ? advance(Step.URL) : nope();
			case URL:
				if (codepoint == (int)'\'' || codepoint == (int)'"') return completed();
				return continuing();
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
