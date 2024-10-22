package com.pocket.sdk.api.spec;

import com.pocket.sdk.api.generated.thing.Get;
import com.pocket.sync.space.Space;
import com.pocket.sync.thing.Thing;
import com.pocket.util.java.Safe;

import java.util.List;

/**
 * Helper methods for working with {@link Thing}s that have count and offset support.
 */
public class SublistUtil {
	/** Return a sublist by applying offset and count to the list. */
	public static <T> List<T> applyOffsetCount(List<T> list, Integer offset, Integer count) {
		if (list.isEmpty()) return list;
		
		if (offset != null && offset > 0) {
			if (offset >= list.size()) {
				list.clear();
			} else {
				list = list.subList(offset, list.size());
			}
		}
		
		if (count != null) {
			if (count == 0) {
				list.clear();
			} else {
				list = list.subList(0, Math.min(list.size(), count));
			}
		}
		return list;
	}
	
	/**
	 * Checks if all of the items that will be needed to derive this list are available locally.
	 * It does this by checking the offset of the provided thing and making sure if it is > 0,
	 * that there are things that cover the range from 0 to this thing's offset.
	 * <p>
	 * For starters, this is just going to simply assume that this object is paged by the same count,
	 * and look for exact offsets and counts going backwards, but in theory this could be more flexible
	 * to look for overlapping or different sizes that cover the same area.
	 *
	 * @param t The get instance to check, (could generalize this to other count/offset things as well if needed)
	 * @return true if all of the previous pages exist in this space, false if not
	 */
	public static boolean hasPreviousPages(Get t, Space.Selector space) {
		int count = Safe.value(t.count);
		int offset = Safe.value(t.offset);
		if (offset == 0) return true;
		if (offset < 0 || count <= 0) return false; // In our simple first implementation this case isn't expected or supported
		if (offset < count || offset % count != 0) return false; // Also only expecting consistent paging sizes where offset increments by a the page size count
		
		while ((offset -= count) >= 0) {
			if (!space.contains(t.builder().count(count).offset(offset).build())[0]) {
				return false;
			}
		}
		return true;
	}
}
