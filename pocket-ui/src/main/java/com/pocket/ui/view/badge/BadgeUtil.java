package com.pocket.ui.view.badge;

import android.content.Context;
import android.content.res.Resources;

import com.pocket.ui.R;

public class BadgeUtil {
	
	public static int getBadgeSize(Context context) {
		Resources res = context.getResources();
		return Math.max(res.getDimensionPixelSize(R.dimen.pkt_badge_height_min), res.getDimensionPixelSize(R.dimen.pkt_badge_height));
	}
}
