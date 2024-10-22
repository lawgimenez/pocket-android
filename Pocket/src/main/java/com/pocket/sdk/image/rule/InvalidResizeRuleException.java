package com.pocket.sdk.image.rule;

/**
 * The image could not be resized because the resize rules were impossible or invalid.
 * 
 * @author max
 *
 */
public class InvalidResizeRuleException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5266990067819157289L;

	public InvalidResizeRuleException(IllegalArgumentException e) {
		super(e);
	}	
}
