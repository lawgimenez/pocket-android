package com.pocket.sdk.api.thing;

import com.pocket.sdk.api.generated.thing.Get;
import com.pocket.sdk.api.generated.thing.SearchItem;
import com.pocket.sdk.api.generated.thing.SearchMatch;
import com.pocket.util.java.Safe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tools for working with {@link com.pocket.sdk.api.generated.thing.Get}
 */
public class GetUtil {
	
	/**
	 * Set all of the standard item field request flags.
	 * @param builder The builder to set the flags on.
	 * @return the same provided builder for chaining
	 */
	public static Get.Builder setAllItemFlags(Get.Builder builder) {
		return builder
				.authors(1)
				.images(1)
				.videos(1)
				.include_item_tags(1)
				.shares(1)
				.rediscovery(1)
				.posts(1)
				.annotations(1)
				.meta(1);
	}
	
	/**
	 * Converts a {@link Get#list} to a list of {@link SearchItem} which has all of the remapped values like sort_id and search_matches included.
	 * Also the returned list is sorted by sort_id.
	 * @param t Where to extract the list and meta data from.
	 * @return A new list instance
	 */
	public static List<SearchItem> list(Get t) {
		if (t == null || t.list == null) return new ArrayList<>(0);
		List<SearchItem> list = new ArrayList<>(t.list.size());
		int offset = Safe.value(t.offset);
		for (int i = 0; i < t.list.size(); i++) {
			int sort_id = offset + (t.sort_ids != null && t.sort_ids.size() > i ? t.sort_ids.get(i) : 0);
			SearchMatch matches = t.search_matches != null && t.search_matches.size() > i ? t.search_matches.get(i) : null;
			list.add(new SearchItem.Builder()
					.item(t.list.get(i))
					.sort_id(sort_id)
					.matches(matches)
					.build());
		}
		Collections.sort(list, (o1, o2) -> Integer.compare(o1.sort_id, o2.sort_id));
		return list;
	}
	
	
}
