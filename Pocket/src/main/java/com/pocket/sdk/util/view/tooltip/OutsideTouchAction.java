package com.pocket.sdk.util.view.tooltip;

/**
 * Configuration for {@link Tooltip}'s handling of user touching outside the tooltip.
 */
public class OutsideTouchAction {
	public enum Block {
		EVERYWHERE, IF_NOT_ON_ANCHOR, NOWHERE
	}
	
	public final Block block;
	public final boolean dismiss;
	
	/**
	 * @param block Should the tooltip block the touch from propagating to the views below? You can either completely 
	 * block touches until tooltip is dismissed, block everywhere except on the anchor or block nowhere (pass through 
	 * everywhere)
	 * @param dismiss Whether touching outside automatically dismiss the tooltip or not
	 */
	public OutsideTouchAction(Block block, boolean dismiss) {
		this.block = block;
		this.dismiss = dismiss;
	}
}
