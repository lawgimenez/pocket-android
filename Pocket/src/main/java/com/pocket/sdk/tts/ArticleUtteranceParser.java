package com.pocket.sdk.tts;

import android.util.SparseArray;

import com.pocket.app.App;
import com.pocket.sdk.api.generated.enums.OfflineStatus;
import com.pocket.sdk.api.generated.enums.PositionType;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.thing.ItemUtil;
import com.pocket.util.java.StringBuilders;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a java implementation of the TTS parsing methods in assets-extra/j/articleview-mobile.js.
 * 
 * Loads an html file and parses out the speakable pieces of text.
 * 
 * @author max
 *
 */
public class ArticleUtteranceParser {
	
	/**
	 * This is taken from TextToSpeechService. 
	 * 4000 is the value set in ICS's TextToSpeechService, but let's go smaller than that.
	 */
	public static final int MAX_UTTERANCE_CHARACTER_LENGTH = 3000;
	
	protected static final String NODE_INDEX = "nodeIndex";
	private static final String NODE_INDEX_SELECTOR = "[" + NODE_INDEX + "]";
	private static final Pattern SINGLE_QUOTE_BEFORE_LETTER = Pattern.compile(" '[a-zA-Z]");
	private static final Pattern SINGLE_QUOTE_AFTER_LETTER = Pattern.compile("[a-zA-Z]' ");
	
	private final ArrayList<Utterance> mSpeech = new ArrayList<Utterance>();
	/**
	 * Maps nodeIndex's to a listenIndex
	 */
	private final SparseArray<Utterance> mUtterancesByNodeIndex = new SparseArray<Utterance>();
	private final App mApp;
	
	private String mTitle;
	private int mTextLength;
	private final String mUrl;
	private String mAuthor;

	private final OnParsedListener mListener;
	
	private boolean mStarted;
	/**
	 * Node indexes that are waiting to be indexed to whatever the next utterance is.
	 */
	private final ArrayList<Integer> mPendingNodeIndexes = new ArrayList<Integer>();
	
	
	/**
	 * 
	 * @param url The url of the article to parse.
	 * @param listener To receive the results of the parsing.
	 */
	public ArticleUtteranceParser(String url, OnParsedListener listener) {
		mUrl = url;
		mListener = listener;
		mApp = App.getApp();
	}
	
	/**
	 * Start the parsing. This is asynchronous. The {@link OnParsedListener} provided in the constructor will be invoked on success or failure.
	 * 
	 * Calling this twice will throw a {@link RuntimeException}.
	 */
	public void parse() {
		if (mStarted) {
			throw new RuntimeException("already parsing / parsed");
		}
		
		mStarted = true;
		
		Item item = ItemUtil.create(mUrl, mApp.pocket().spec());
		mApp.offline().download(item, PositionType.ARTICLE, false, (i, view, status) -> mApp.threads().runOrPostOnUiThread(() -> {
			if (status == OfflineStatus.OFFLINE) {
				// Begin the parsing
				mApp.threads().asyncThen(() -> parse(App.getApp().offline().articleViewLocation(item), mUrl),
					(success, e) -> {
						if (!success || mSpeech.isEmpty()) {
							mListener.onArticleUtterancesParserError(mUrl, ListenError.ARTICLE_PARSING_FAILED);
						} else {
							mListener.onArticleUtterancesParsed(mUrl, new ArticleTTSUtterances(mSpeech, mUtterancesByNodeIndex, mTitle, mUrl, mAuthor, mTextLength));
						}
					});
				
			} else {
				mListener.onArticleUtterancesParserError(mUrl, ListenError.ARTICLE_NOT_DOWNLOADED);
			}
		}));
	}

	private void parse(File file, String baseUri) throws IOException {
		Document doc = Jsoup.parse(file, "UTF-8", baseUri);
		
		// Add header elements
		Element title = doc.select("#RIL_header h1").first();
		addUtterance(title);
		mTitle = title.text();
		
		Element author = doc.getElementsByClass("RIL_author").first();
		if (author != null) {
			addUtterance(author, " by ");
			mAuthor = author.text();
		}
		
		addUtterance(doc.getElementsByClass("RIL_date").first()); // Date
		
		// Add body text
		parseNode(doc.getElementById("RIL_body"));
		flushPendingNodeIndexes();
	}
	
	/**
	 * Recursively search the provided element and its children for speakable text. Any text found will be added to {@link #mSpeech} and {@link #mNonSpeech} as needed. 
	 * {@link #mListenIndex} is also incremented.
	 * 
	 * @param element
	 */
	private void parseNode(Element element) {
		List<Node> children = element.childNodes();
		for (Node childAsNode : children) {
			if (childAsNode instanceof TextNode) {
				// Ready to add as-is
				addUtterance((TextNode) childAsNode);
				
			} else if (childAsNode instanceof Element) {
				Element childAsElement = (Element) childAsNode;
				String lcTag = childAsElement.tagName().toLowerCase();
				
				// Need to check if we should add it.
				if (lcTag.equals("script") || lcTag.equals("style")) {
					// Skip this element and all of its children completely.
				} else if (childAsElement.hasAttr("pktnolisten")) {
					// Skip, Pocket's parser asked us to skip this element completely.
					
				} else {
					// Going to add it, but determine in what way.
					
					if (selectOnlyChildren(childAsElement, NODE_INDEX_SELECTOR).isEmpty()) {
						// Has no children that are nodeIndex's, add this node as a single speech block
						addUtterance(childAsElement);
						
					} else {
						// Has children with nodeIndexes
						int nodeIndex = getNodeIndexOf(childAsElement);
						if (nodeIndex != 0 && isOneLogicalSpeechBlock(childAsElement)) {
							// We will include this node as one speech block and not break up its children.
			            	addUtterance(childAsElement);
			            	
						} else {
							// This element itself will not be a speech node, we will break it up into different speech nodes.
			            	if (nodeIndex != 0) {
								mPendingNodeIndexes.add(nodeIndex);
							}
							parseNode(childAsElement); // Continue recursively
						}
					}
				}
				
			} else {
				// Ignore
			}
		}
	}
	
	private void addUtterance(TextNode node) {
		String text = node.text();
		if (StringUtils.isBlank(text)) {
			return; // Not speakable, ignore
		}
		
		addUtterance(node.text(), NodeSelector.getNew(node), 0, false);
	}
	
	private void addUtterance(Element node) {
		addUtterance(node, null);
	}
	
	private void addUtterance(Element node, String prefix) {
		if (node == null) {
			return; // Ignore
		}
		
		// Make sure there are no hidden nodes
		node.select("[style*=display:none]").remove();
		node.select("[type=hidden]").remove();
		node.select("[width=0]").select("[height=0]").remove();
		
		int nodeIndex = getNodeIndexOf(node);
		String text = node.text();
		if (StringUtils.isBlank(text)) {
			// Ignore it, but record the nodeIndex
			if (nodeIndex != 0) {
				mPendingNodeIndexes.add(nodeIndex);
			}
			
		} else {
			if (!StringUtils.isEmpty(prefix)) {
				text = (prefix + text).trim();
			}
			
			addUtterance(text, NodeSelector.getNew(node), nodeIndex, isHeader(node));
		}
	}
	
	private void addUtterance(String text, NodeSelector jQuerySelector, int nodeIndex, boolean isHeader) {
		addUtterance(text, jQuerySelector, nodeIndex, isHeader, -1);
	}
	
	/**
	 * Do not call directly. For recursive use. Please use {@link #addUtterance(String, String, int, boolean)}.
	 * 
	 * @param isSegment true if this is a child/segment of some text that was broken up due to {@link #MAX_UTTERANCE_CHARACTER_LENGTH}. false if this is a whole block of text.
	 */
	private void addUtterance(String text, NodeSelector jQuerySelector, int nodeIndex, boolean isHeader, int segmentIndex) {
		if (StringUtils.isBlank(text)) {
			return; // Ignore
		}
		
		text = cleanText(text);
		
		// Check if the utterance is within TTS limits or needs to be broken up 
		if (text.length() < MAX_UTTERANCE_CHARACTER_LENGTH) {
			// Ready to add
			mTextLength += text.length();
			Utterance utterance = new Utterance(text, jQuerySelector, nodeIndex, isHeader, mTextLength, segmentIndex, mSpeech.size());
			mSpeech.add(utterance);
			
			flushPendingNodeIndexes();
			if (nodeIndex != 0) {
				mUtterancesByNodeIndex.put(nodeIndex, utterance);
			}
			
		} else {
			// Text is too long to speak with TTS. Need to break it up into separate speech nodes.
			StringTokenizer tokenizer = new StringTokenizer(text, " ");
			StringBuilder builder = StringBuilders.get();
			
			int len = 0;
			int i = 0;
			while (tokenizer.hasMoreTokens()) {
		        String word = tokenizer.nextToken();
		        
		        int wordLen = word.length();
		        if (len + wordLen < MAX_UTTERANCE_CHARACTER_LENGTH) {
		        	// Fits
		        	builder.append(word);
		        	len += wordLen;
		        	
		        } else {
		        	// The next word would go over the limit, add what we already have 
		        	addUtterance(builder.toString(), jQuerySelector, nodeIndex, false, i);
		        	i++;
		        	
		        	// Reset the builder
		        	builder.setLength(0);
		        	len = 0;
		        }
		    }
			
			if (builder.length() > 0) {
				// Reminder
				addUtterance(builder.toString(), jQuerySelector, nodeIndex, false, i);
			}
			
			StringBuilders.recycle(builder);
		}
	}
	
	/**
	 * Index any pending node indexes to whatever the last added {@link Utterance} was.
	 */
	private void flushPendingNodeIndexes() {
		Utterance lastUtterance = mSpeech.get(mSpeech.size()-1);
		for (Integer ni : mPendingNodeIndexes) {
			mUtterancesByNodeIndex.put(ni.intValue(), lastUtterance);
		}
		mPendingNodeIndexes.clear();
	}

	/**
	 * Is the element a header tag like h1, h2, h3, h4, h5, h6.
	 * 
	 * @param element
	 * @return
	 */
	private static boolean isHeader(Element element) {
		String tag = element.tagName();
		if (tag.length() != 2) {
			return false;
		} else if (tag.charAt(0) != 'h' && tag.charAt(0) != 'H') {
			return false;
		} else {
			return Character.isDigit(tag.charAt(1));
		}
	}
	
	/**
	 * If the only nodeIndex child are A's with less than 30 characters each, or are just empty nodes, then we should just
	 * include the parent as one speechNode and not break it up.
	 * 
	 * @param parent
	 * @return
	 */
	private static boolean isOneLogicalSpeechBlock(Element parent) {
		Elements nodeIndexChildren = selectOnlyChildren(parent, NODE_INDEX_SELECTOR);
		for (Element child : nodeIndexChildren) {
			int len = child.text().length();
			if (len == 0) {
				continue; // Will be ignored anyways, so no need to break it up.
			}
			
			if (StringUtils.equalsIgnoreCase(child.tagName(), "a") && len < 30) {
				continue; // It is a link that is short, likely not needing to be broken up.
			}
			
			// This node should probably be separated.	
			return false;
		}
		return true;
	}
	
	/**
	 * Returns the nodeIndex attribute as an int. If there is none set, it returns 0.
	 * @param node
	 * @return
	 */
	protected static int getNodeIndexOf(Element node) {
		String nodeIndexString = node.attr(NODE_INDEX);
		return !StringUtils.isEmpty(nodeIndexString) ? Integer.valueOf(nodeIndexString) : 0;
	}
	
	private static Elements selectOnlyChildren(Element element, String selector) {
		Elements elements = element.select(selector);
		elements.remove(element); // Remove self since select() can include itself.
		return elements;
	}
	
	/**
	 * Cleans up text to avoid bugs or weird speech patterns with TTS.
	 * 
	 * Details:
	 * There is a native tts crash that occurs when speaking "a 'share" or similar phrases. It
	 * seems that it requires a word and a space before, and then a single quote and then a word at least 5 characters long.
	 * 
	 * Also single quotes sometimes read as "single quote" outloud by tts. So let's just replace them all with double quotes.
	 * 
	 * These patterns have spaces before and after the single quote to avoid replacing apostrophes like in "it's".
	 * 
	 * @param text
	 * @return
	 */
	public static String cleanText(String text) {
		Matcher matcher = SINGLE_QUOTE_BEFORE_LETTER.matcher(text);
		text = matcher.replaceAll(" \"");
		matcher = SINGLE_QUOTE_AFTER_LETTER.matcher(text);
		text = matcher.replaceAll("\" ");
		
		return text;
	}
	
	public static class NonSpeech {
		
		public final int nodeIndex;
		public final int listenIndex;
		
		private NonSpeech(int nodeIndex, int listenIndex) {
			this.nodeIndex = nodeIndex;
			this.listenIndex = listenIndex;
		}
		
	}
	
	/**
	 * The result of an Article being parsed into utterances for TTS.
	 * 
	 * @see ArticleUtteranceParser
	 * @author max
	 *
	 */
	public static class ArticleTTSUtterances {
		
		private final ArrayList<Utterance> utterances;
		private final SparseArray<Utterance> utterancesByNodeIndex;
		private final String title;
		private final String url;
		private final String author;
		private final int length;

		private ArticleTTSUtterances(ArrayList<Utterance> utterances,
				SparseArray<Utterance> utterancesByNodeIndex,
				String title,
				String url,
				String author,
				int length) {
			
			this.utterances = utterances;
			this.utterancesByNodeIndex = utterancesByNodeIndex;
			this.title = title;
			this.url = url;
			this.author = author;
			this.length = length;
		}

		public ArrayList<Utterance> getUtterances() {
			return utterances;
		}
		
		public Utterance get(int index) {
			return utterances.get(index);
		}

		public Utterance getByNodeIndex(int nodeIndex) {
			return utterancesByNodeIndex.get(nodeIndex);
		}
		
		public String getTitle() {
			return title;
		}

		public String getUrl() {
			return url;
		}
		
		public String getAuthor() {
			return author;
		}

		/**
		 * @return The length() of all text combined.
		 */
		public int getLength() {
			return length;
		}
		
	}

	public interface OnParsedListener {
		/**
		 * Invoked when the parser has completed successfully.
		 * @param url The url of the article that was parsed
		 * @param result The parse result
		 */
		public void onArticleUtterancesParsed(String url, ArticleTTSUtterances result);
		
		/**
		 * An error reason such as {@link ListenError#ARTICLE_PARSING_FAILED}
		 * @param url The url of the article that was parsed
		 */
		void onArticleUtterancesParserError(String url, ListenError error);
	}
	
}
