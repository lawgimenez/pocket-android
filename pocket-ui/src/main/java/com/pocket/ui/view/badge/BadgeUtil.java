package com.pocket.ui.view.badge;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;

import com.pocket.ui.R;

import androidx.core.content.ContextCompat;

/**
 * TODO Documentation
 */
public class BadgeUtil {
	
	public static int getBadgeSize(Context context) {
		Resources res = context.getResources();
		return Math.max(res.getDimensionPixelSize(R.dimen.pkt_badge_height_min), res.getDimensionPixelSize(R.dimen.pkt_badge_height));
	}
	
	/**
	 * Only use for locally declared fallbacks, these should be provided by the server
	 * in {@link com.pocket.sdk.api.generated.thing.Group#badge_text_color}.
	 */
	public static ColorStateList getGroupTextColor(Context context, int id) {
		switch (id) {
			case 1: return ContextCompat.getColorStateList(context, R.color.pkt_badge_best_of_text);
			case 2:
			default: // in case we don't get the server side implementation in time, have this provide some safety for future versions, just use this coloring for unknowns
				return ContextCompat.getColorStateList(context, R.color.pkt_badge_trending_text);
		}
	}
	
	/**
	 * Only use for locally declared fallbacks, these should be provided by the server
	 * in {@link com.pocket.sdk.api.generated.thing.Group#badge_color}.
	 */
	public static ColorStateList getGroupBadgeColor(Context context, int id) {
		switch (id) {
			case 1: return ContextCompat.getColorStateList(context, R.color.pkt_badge_best_of);
			case 2:
			default: // in case we don't get the server side implementation in time, have this provide some safety for future versions, just use this coloring for unknowns
				return ContextCompat.getColorStateList(context, R.color.pkt_badge_trending);
		}
	}
	
	
}
