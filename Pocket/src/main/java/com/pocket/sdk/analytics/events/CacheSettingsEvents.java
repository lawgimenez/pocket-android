package com.pocket.sdk.analytics.events;

import com.pocket.sdk.api.generated.enums.CxtEvent;
import com.pocket.sdk.api.generated.enums.CxtPage;
import com.pocket.sdk.api.generated.enums.CxtSection;
import com.pocket.sdk.offline.cache.Assets;
import com.pocket.sdk.offline.cache.Assets.CachePriority;
import com.pocket.sdk.preferences.AppPrefs;
import com.pocket.util.java.BytesUtil;

public class CacheSettingsEvents {
	
	private static PvWtEvent create(CxtEvent action, int typeId) {
		return new PvWtEvent(typeId, CxtSection.SETTINGS, CxtPage.CACHE_SETTINGS, action);
	}

	// Actions
	public static final PvWtEvent VIEW = create(CxtEvent.VIEW_PAGE, 1);
	private static final PvWtEvent CHANGE_LIMIT = create(CxtEvent.CHANGE_LIMIT, 3);
	private static final PvWtEvent CHANGE_PRIORITY = create(CxtEvent.CHANGE_PRIORITY, 3);
	
	/**
	 * @param priorityKey The new {@link AppPrefs#CACHE_SORT} key. One of {@link CachePriority}.
	 */
	public static void sendPriorityChange(int priorityKey) {
		String value;
		if (priorityKey == Assets.CachePriority.OLDEST_FIRST) {
			value = "oldest";
		} else {
			value = "newest";
		}
		CacheSettingsEvents.CHANGE_PRIORITY.send(value);
	}
	
	/**
	 * @param limit The new limit {@link AppPrefs#CACHE_SIZE_USER_LIMIT} in bytes.
	 */
	public static void sendLimitChange(long bytes) {
		String mb;
		if (bytes <= 0) {
			mb = "0"; // Unlimited
		} else {
			mb = String.valueOf((int) BytesUtil.bytesToMb(bytes));
		}
		CacheSettingsEvents.CHANGE_LIMIT.send(mb);
	}
	
}
