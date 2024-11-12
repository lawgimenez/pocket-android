package com.pocket.sdk.tts;

import com.pocket.app.App;
import com.pocket.util.java.StringBuilders;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

public class NodeSelector {
	
	/**
	 * Get a {@link NodeSelector} that will target this exact {@link TextNode} within the document.
	 * 
	 * @param node
	 * @return The selector or null if none could be determined.
	 */
	protected static NodeSelector getNew(TextNode node) {
		if (node.parent() instanceof Element) {
			Element parent = (Element) node.parent();
			String selector = getJQuerySelector(parent);
			return new NodeSelector(selector, indexInParent(node));
			
		} else {
			// I don't believe any of our text supported nodes (Element or TextNode) would ever not have a parent like this. (Without having an unique id)
			// So I think we can ignore it.
			return null;
		}
	}
	
	/**
	 * Get a {@link NodeSelector} that will target this exact {@link Element} within the document.
	 * 
	 * @param node
	 * @return The selector or null if none could be determined.
	 */
	protected static NodeSelector getNew(Element element) {
		String selector = getJQuerySelector(element);
		if (selector != null) {
			return new NodeSelector(selector);
		} else {
			return null;
		}
	}
	
	
	
	
	
	/**
	 * A jQuery selector for the element. If {@link #mIndex} is not -1, then this is actually the parent of the TextNode. 
	 */
	public final String mElement;
	/**
	 * If the node was a TextNode, this will be the index within {@link #mElement}. where the TextNode is located.
	 */
	public final int mIndex;
	
	private NodeSelector(String element, int node){
		mElement = element;
		mIndex = node;
	}
	
	private NodeSelector(String element) {
		this(element, -1);
	}
	
	@Override
	public String toString() {
		return "NodeSelector [mElement=" + mElement + ", mIndex=" + mIndex
				+ "]";
	}

	/**
	 * Find a css/jquery selector that will target this exact {@link Element} within the document.
	 * 
	 * @param node
	 * @return The selector or null if none could be determined.
	 */
	private static String getJQuerySelector(Element element) {
		String uniqueId = getUniqueSelector(element);
		if (uniqueId != null) {
			return uniqueId;
		}
		
		final Elements parents = element.parent().parents();
		final StringBuilder parentsSelectorBuilder = StringBuilders.get();
		final int childIndex = indexInParent(element);
		
		if (childIndex >= 0) {
			// :eq(childIndex) selects the the nth-child of the matching element.
			parentsSelectorBuilder.append(":eq(")
				.append(childIndex)
				.append(")");
		}
		
		for (Element parent : parents) {
			String uniqueSelector = getUniqueSelector(parent);
			if (uniqueSelector != null) {
				parentsSelectorBuilder.insert(0, uniqueSelector);
				String selector = parentsSelectorBuilder.toString();
				StringBuilders.recycle(parentsSelectorBuilder);
				return selector;
				
			} else {
				// add each tag to the selector so it builds a chain like "p span a" to describe where this child is located
				// build a :eq(childIndex) backwards at the front of the string
				parentsSelectorBuilder.insert(0, ")")
					.insert(0, indexInParent(parent))
					.insert(0, ":eq(");
				
				// Should continue to loop
			}
		}
		
		// I don't think it should ever get here? It should always find a unique selector at some point
		if (App.getApp().mode().isForInternalCompanyOnly()) {
			throw new RuntimeException("unexpected state");
		}
		return null;
	}
	
	private static int indexInParent(Element child) {
		Element parent = child.parent();
		if (parent == null) {
			return -1;
		}
		return parent.children().indexOf(child);
	}
	
	private static int indexInParent(TextNode child) {
		Node parent = child.parent();
		if (parent == null) {
			return -1;
		}
		if (parent instanceof Element) {
			return ((Element) parent).childNodes().indexOf(child);
		} else {
			return -1; // Not supported
		}
	}
	
	/**
	 * Tries to find an document unique jQuery selector to reach this element. Does not traverse parents
	 * to try to create one if it doesn't exist within the element itself.
	 * 
	 * Looks for an id, a node index or if it is a unique tag like body or html.
	 * 
	 * @param element
	 * @return A unique jQuery selector to reach this element or null if none available.
	 * 
	 * @see #getJQuerySelector(Node, int)
	 */
	private static String getUniqueSelector(Element element) {
		int nodeIndex = ArticleUtteranceParser.getNodeIndexOf(element);
		if (nodeIndex != 0) {
			return "[" + ArticleUtteranceParser.NODE_INDEX + "=" + nodeIndex + "]";
		} else if (!StringUtils.isBlank(element.id())) {
			return "#" + element.id();
		} else {
			String tag = element.tagName();
			if (StringUtils.equalsIgnoreCase(tag, "body") || StringUtils.equalsIgnoreCase(tag, "html")) {
				return tag;
			}
		}
		
		return null;
	}
	
}
