package com.pocket.sdk.tts;

import android.speech.tts.TextToSpeech;

import java.util.HashMap;

/**
 * TODO Documentation
 */
public class Utterance {
	
	/**
	 * The text to speak.
	 */
	public final String text;
	/**
	 * A {@link NodeSelector} to use to target this text within the article. May be null in some rare cases.
	 */
	public final NodeSelector jQuerySelector;
	/**
	 * The node index of this text in the Article View or 0 if none.
	 */
	public final int nodeIndex;
	/**
	 * Whether or not this text is a title/header in the article.
	 */
	public final boolean isHeader;
	/**
	 * The position in the article (represented by character count) at the end of this utterance.
	 */
	public final int endPosition;
	/**
	 * If this text is part of a larger node that had to be broken up because of {@link ArticleUtteranceParser#MAX_UTTERANCE_CHARACTER_LENGTH}, this will be the index
	 * within the parent that this segment is. -1 if it is a whole segment that wasn't broken up.
	 */
	public final int segmentIndex;
	/**
	 * The position of this utterance within {@link ArticleUtteranceParser.ArticleTTSUtterances#utterances}.
	 * This order is not expected to change so it is ok to cache this value.
	 */
	public final int position;
	
	protected final HashMap<String, String> params = new HashMap<String, String>();
	
	Utterance(String text, NodeSelector jQuerySelector, int nodeIndex, boolean isHeader, int endLength, int segmentIndex, int position) {
		this.text = text;
		this.jQuerySelector = jQuerySelector;
		this.nodeIndex = nodeIndex;
		this.isHeader = isHeader;
		this.endPosition = endLength;
		this.segmentIndex = segmentIndex;
		this.position = position;
		params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, String.valueOf(position));
	}
	
	/**
	 * If this text is part of a larger node that had to be broken up because of {@link ArticleUtteranceParser#MAX_UTTERANCE_CHARACTER_LENGTH}, this will be the index
	 * within the parent that this segment is.
	 *
	 * @return
	 */
	public boolean isSegment() {
		return segmentIndex >= 0;
	}
	
	@Override
	public String toString() {
		return "Utterance [text=" + text + ", jQuerySelector="
				+ jQuerySelector + ", nodeIndex=" + nodeIndex
				+ ", isHeader=" + isHeader + ", endPosition=" + endPosition
				+ ", segmentIndex=" + segmentIndex + ", position="
				+ position + "]";
	}
	
	
}
