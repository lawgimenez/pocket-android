package com.pocket.sdk.image.rule;

/**
 * The decoded image returned <= 0 for width and/or height.
 * 
 * @author max
 *
 */
public class InvalidImageException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8606985735537007976L;

	public InvalidImageException(String detailMessage) {
		super(detailMessage);
	}

	public InvalidImageException(int outWidth, int outHeight) {
		this("Width: " + outWidth + " Height: " + outHeight);
	}
	
}
