package com.pocket.sdk.api.thing;

import com.pocket.sdk.api.generated.enums.Imageness;
import com.pocket.sdk.api.generated.enums.ItemContentType;
import com.pocket.sdk.api.generated.enums.ItemSortKey;
import com.pocket.sdk.api.generated.enums.ItemStatus;
import com.pocket.sdk.api.generated.enums.ItemStatusKey;
import com.pocket.sdk.api.generated.enums.OfflinePreference;
import com.pocket.sdk.api.generated.enums.OfflineStatus;
import com.pocket.sdk.api.generated.enums.PositionType;
import com.pocket.sdk.api.generated.enums.ReservedTag;
import com.pocket.sdk.api.generated.enums.SharedItemStatus;
import com.pocket.sdk.api.generated.enums.Videoness;
import com.pocket.sdk.api.generated.thing.Annotation;
import com.pocket.sdk.api.generated.thing.Author;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.generated.thing.ListenSettings;
import com.pocket.sdk.api.generated.thing.Position;
import com.pocket.sdk.api.generated.thing.SharedItem;
import com.pocket.sdk.api.generated.thing.Tag;
import com.pocket.sdk.api.spec.PocketSpec;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.api.value.UrlString;
import com.pocket.util.java.DomainUtils;
import com.pocket.util.java.Safe;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.threeten.bp.Duration;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static kotlin.collections.CollectionsKt.sortWith;
import static kotlin.comparisons.ComparisonsKt.compareBy;
import static kotlin.comparisons.ComparisonsKt.reversed;

/**
 * Tools for working with {@link Item}
 */

public class ItemUtil {
	
	public static final int MAX_TAG_LENGTH = 25; // TODO where to put constants like this?

	// TODO other languages faster or slower?
	private static final int AVERAGE_WORDS_PER_MINUTE = 220;
	
	public static Position getPosition(Item item, PositionType type) {
		if (item.positions != null) {
			return item.positions.get(type.toString());
		} else {
			return null;
		}
	}
	
	public static Item create(String url, PocketSpec spec) {
		return build(url, spec).build();
	}
	
	public static Item.Builder build(String url, PocketSpec spec) {
		return build(new UrlString(url), spec);
	}
	
	public static Item.Builder build(UrlString url, PocketSpec spec) {
		return spec.things().item().given_url(url);
	}
	
	/**
	 * This is used in a few places in the app.
	 * REVIEW is this still needed? if so, can we figure it out enough to document its reason?
	 */
	public static String unhashBang(String url) {
		return url.replace("#!", "?_escaped_fragment_=");
	}
	
	/**
	 * @return true if the status indicates it has not yet been downloaded for offline viewing and hasn't been marked as invalid.
	 */
	public static boolean isDownloadable(OfflineStatus status, boolean allowRetries) {
		if (status == null || status == OfflineStatus.NOT_OFFLINE) return true;
		if (allowRetries) return status == OfflineStatus.FAILED || status == OfflineStatus.PARTIAL;
		return false;
	}
	
	/**
	 * @return A list of views that still need to be downloaded to match the user's preferences
	 */
	public static Set<PositionType> downloadables(Item item, OfflinePreference pref, boolean allowRetries) {
		boolean article;
		boolean web;
		if (pref == OfflinePreference.BOTH) {
			article = ItemUtil.isDownloadable(item.offline_text, allowRetries);
			web = ItemUtil.isDownloadable(item.offline_web, allowRetries);
			
		} else if (pref == OfflinePreference.ARTICLE_ONLY) {
			article = (item.is_article == null || item.is_article) && ItemUtil.isDownloadable(item.offline_text, allowRetries);
			web = false;
			
		} else if (pref == OfflinePreference.WEB_ONLY) {
			article = false;
			web = ItemUtil.isDownloadable(item.offline_web, allowRetries);
			
		} else if (pref == OfflinePreference.AUTO) {
			if (item.is_article == null || item.is_article) {
				article = ItemUtil.isDownloadable(item.offline_text, allowRetries);
				web = false;
			} else {
				article = false;
				web = ItemUtil.isDownloadable(item.offline_web, allowRetries);
			}
		} else {
			return Collections.emptySet();
		}
		Set<PositionType> views = new HashSet<>();
		if (article) views.add(PositionType.ARTICLE);
		if (web) views.add(PositionType.WEB);
		return views;
	}
	
	public static boolean isViewableOffline(OfflineStatus status) {
		return status == OfflineStatus.OFFLINE || status == OfflineStatus.OFFLINE_AS_ASSET || status == OfflineStatus.PARTIAL;
	}
	
	/**
	 * @return The {@link Position#percent} of the {@link Item#positions} with the most recent {@link Position#time_updated}, or 0 if no item or position data is found.
	 */
	public static int getPercent(Item data) {
		Position p = mostRecentPosition(data);
		return p != null ? Safe.value(p.percent) : 0;
	}
	
	public static Position mostRecentPosition(Item data) {
		if (data == null || data.positions == null) return null;
		Position recent = null;
		for (Position p : data.positions.values()) {
			if (recent == null || Timestamp.get(recent.time_updated) < Timestamp.get(p.time_updated)) {
				recent = p;
			}
		}
		return recent;
	}
	
	public static long positionUpdated(Item item) {
		Position p = mostRecentPosition(item);
		return p != null ? Timestamp.get(p.time_updated) : 0;
	}
	
	/** @return The {@link Annotation} that has the most recent {@link Annotation#created_at} or null if no annotations or the item is null. */
	public static Annotation mostRecentAnnotation(Item data) {
		if (data == null || data.annotations == null || data.annotations.isEmpty()) return null;
		Annotation recent = null;
		for (int i = 0, len = data.annotations.size(); i < len; i++) {
			Annotation a = data.annotations.get(i);
			if (recent == null || a.created_at.compareTo(recent.created_at) < 0) {
				recent = a;
			}
		}
		return recent;
	}
	
	/**
	 * @return true if this item matches this tag. false if the item is null, the tag is null or blank or the item doesn't match the tag.
	 * 			If the tag is {@link com.pocket.sdk.api.generated.enums.ReservedTag#_UNTAGGED_} this will match if the item doesn't have any tags.
	 * 			Otherwise it matches if the item contains this tag using the rules described in {@link TagUtil#indexOfTag(List, Tag)}.
	 */
	public static boolean matchesTag(Item item, String tag) {
		if (item == null || StringUtils.isBlank(tag)) return false;
		boolean hasTags = item.tags != null && !item.tags.isEmpty();
		if (ReservedTag._UNTAGGED_.value.equals(tag)) return !hasTags;
		return hasTags && TagUtil.indexOfTag(item.tags, tag) >= 0;
	}
	
	/** @return true if this item matches the {@link ItemStatusKey} requirements. false if the item is null or doesn't match. */
	public static boolean matchesStatusKey(Item item, @Nullable ItemStatusKey key) {
		if (item == null) return false;
		key = key != null ? key : ItemStatusKey.ALL; // Defaults to ALL when not specified
		if (key == ItemStatusKey.ALL) {
			return item.status == ItemStatus.UNREAD || item.status == ItemStatus.ARCHIVED;
		} else if (key == ItemStatusKey.UNREAD || key == ItemStatusKey.QUEUE) {
			return item.status == ItemStatus.UNREAD;
		} else if (key == ItemStatusKey.ARCHIVE) {
			return item.status == ItemStatus.ARCHIVED;
		} else if (key == ItemStatusKey.ANYACTIVE) {
			return item.status == ItemStatus.UNREAD || item.status == ItemStatus.ARCHIVED || hasShareOf(item, SharedItemStatus.UNAPPROVED);
		} else if (key == ItemStatusKey.UNREAD_AND_ARCHIVED) {
			return item.status == ItemStatus.UNREAD || item.status == ItemStatus.ARCHIVED;
		}
		
		return false;
	}
	
	/**
	 * @param item The item to compare
	 * @param type The content type to match
	 * @param listenSettings If using {@link ItemContentType#LISTENABLE}, it will need some settings to check. If this is null, it will use the default settings.
	 * @return true if this item matches the {@link ItemContentType} requirements. false if the item or type is null or doesn't match.
	 */
	public static boolean matchesContentType(Item item, ItemContentType type, ListenSettings listenSettings) {
		if (item == null || type == null) return false;
		if (type == ItemContentType.ARTICLE) {
			return Safe.value(item.is_article);
			
		} else if (type == ItemContentType.IMAGE) {
			return item.has_image == Imageness.IS_IMAGE;
			
		} else if (type == ItemContentType.VIDEO) {
			return item.has_video == Videoness.IS_VIDEO || item.has_video == Videoness.HAS_VIDEOS;
			
		} else if (type == ItemContentType.LISTENABLE) {
			if (!Safe.value(item.is_article)) return false;
			int word_count = Safe.value(item.word_count);
			int item_min_word_count = listenSettings != null && listenSettings.item_min_word_count != null ? listenSettings.item_min_word_count : 0;
			int item_max_word_count = listenSettings != null && listenSettings.item_max_word_count != null ? listenSettings.item_max_word_count : 24000; // This default comes from the description on item_max_word_count. TODO find a way to document constants like this in figment?
			return word_count >= item_min_word_count && word_count <= item_max_word_count;
		}
		return false;
	}
	
	/** @return true if this item contains at least one {@link SharedItem} with this status. false if the item is null or doesn't contain any. */
	public static boolean hasShareOf(Item item, SharedItemStatus status) {
		if (item == null || item.shares == null) return false;
		for (SharedItem si : item.shares) {
			if (si.status == status) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Sorts the list by the provided key.
	 * @return true if sorted, false if the sort is a type that isn't supported.
	 */
	public static boolean sort(List<Item> list, ItemSortKey sort) {
		if (list == null || list.isEmpty()) return true;
		if (sort == null || sort == ItemSortKey.NEWEST) {
			sortItems(list, (o1, o2) -> ObjectUtils.compare(o2.time_added, o1.time_added));
		} else if (sort == ItemSortKey.OLDEST) {
			Collections.sort(list, (o1, o2) -> ObjectUtils.compare(o1.time_added, o2.time_added));
		} else if (sort == ItemSortKey.POSITION_UPDATED) {
			Collections.sort(list, (o1, o2) -> ObjectUtils.compare(ItemUtil.positionUpdated(o2), ItemUtil.positionUpdated(o1)));
		} else if (sort == ItemSortKey.UPDATED) {
			Collections.sort(list, (o1, o2) -> ObjectUtils.compare(o2.time_updated, o1.time_updated));
		} else if (sort == ItemSortKey.ANNOTATION) {
			Collections.sort(list, (o1, o2) -> {
				Annotation a1 = mostRecentAnnotation(o1);
				Annotation a2 = mostRecentAnnotation(o2);
				return ObjectUtils.compare(a2.created_at, a1.created_at);
			});
		} else if (sort == ItemSortKey.LONGEST) {
			Comparator<Item> comparator = compareBy(
					ItemUtil::viewingTime,
					item -> item.time_added,
					item -> item.id_url.url
			);
			// By default compareValuesBy puts nulls first, so when we reverse it works out.
			sortWith(list, reversed(comparator));
		} else if (sort == ItemSortKey.SHORTEST) {
			// If we want to have nulls last, but non-nulls in ascending order,
			// we have write it out a bit more manually. 
			Comparator<Item> comparator = compareBy(
					item -> viewingTime(item) == null ? 1 : 0, // nulls last,
					item -> viewingTime(item),
					item -> item.time_added,
					item -> item.id_url.url
			);
			sortWith(list,comparator);
		} else {
			return false;
		}
		return true;
	}
	
	/**
	 * Sorts items with a primary comparator and if they match, (like when comparing timestamps they are equal),
	 * has a fallback comparator that will provide a consistent order.
	 * This helps random shifting issues where if otherwise left, the ordering of similar elements could be random
	 * @param list The collection to sort
	 * @param primary The primary sort comparison, the fallback will be used when this returns 0.
	 */
	private static void sortItems(List<Item> list, Comparator<Item> primary) {
		Comparator<Item> fallback = (o1, o2) -> o1.id_url.url.compareTo(o2.id_url.url);
		Collections.sort(list, (o1, o2) -> {
			int c = primary.compare(o1, o2);
			return c != 0 ? c : fallback.compare(o1, o2);
		});
	}

	/**
	 * Returns time needed to view or consume the item. For an article it's reading time. For a 
	 * video it's its length.
	 */
	public static Duration viewingTime(Item item) {
		Long seconds = viewingSeconds(item);
		return seconds == null ? null : Duration.ofSeconds(seconds);
	}

	public static Long viewingSeconds(Item item) {
		if (Safe.value(item.is_article) && item.word_count != null) {
			if (item.word_count < minWordCountForViewingTime()) {
				return null;
			} else {
				return (long) (item.word_count * 60 / AVERAGE_WORDS_PER_MINUTE);
			}

		} else if (item.has_video == Videoness.IS_VIDEO) {
			return Safe.get(() -> ((long) item.videos.get(0).length));

		} else {
			return null;
		}
	}

	/**
	 * If it's small enough we'd have to round it to a single minute,
	 * then don't trust it's accurate. Quoting Kait:
	 * These are rarely items that are truly under 2 min to read.
	 * Oftentimes it's things we can't parse or that didn't parse correctly,
	 * and they're always a disappointment
	 */
	public static int minWordCountForViewingTime() {
		return AVERAGE_WORDS_PER_MINUTE * 3 / 2;
	}
	
	public static int secondsToWordCount(long seconds) {
		return (int) (seconds * AVERAGE_WORDS_PER_MINUTE / 60);
	}
}
