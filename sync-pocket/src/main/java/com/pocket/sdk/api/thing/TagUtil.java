package com.pocket.sdk.api.thing;

import com.pocket.sdk.api.generated.enums.ReservedTag;
import com.pocket.sdk.api.generated.thing.Tag;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Tools for working with {@link Tag}s.
 */
public class TagUtil {
	
	public static Tag clean(String tag) {
		if (tag == null) return null;
		return clean(new Tag.Builder().tag(tag).build());
	}
	
	/**
	 * @return null if the tag is null, blank or a {@link com.pocket.sdk.api.generated.enums.ReservedTag}.
	 * 			Otherwise returns a tag with whitespace trimmed and truncated if too large.
	 */
	public static Tag clean(Tag tag) {
		String value = tag != null ? StringUtils.trimToNull(tag.tag) : null;
		if (value == null) return null;
		if (value.length() > 25) value = StringUtils.substring(value, 0, 25);
		for (ReservedTag r : ReservedTag.values()) {
			if (r.value.equals(value)) return null;
		}
		return tag.builder().tag(value).build();
	}
	
	/**
	 * @return a non-null copy of the list of tags, deduplicated and where each tag has passed through {@link #clean(Tag)}.
	 * Any that clean returned null for are removed from the list.
	 */
	public static List<Tag> cleanTagsStrs(List<String> tags) {
		if (tags == null) return Collections.emptyList();

		Collection<Tag> cleaned = new HashSet<>();
		for (String tag : tags) {
			Tag clean = clean(tag);
			if (clean != null) {
				cleaned.add(clean);
			}
		}
		return new ArrayList<>(cleaned);
	}
	
	/** Compares equality using the special tag rules (case insensitive) */
	public static boolean equals(Tag a, Tag b) {
		return equals(a != null ? a.tag : null, b != null ? b.tag : null);
	}
	
	/** Compares equality using the special tag rules (case insensitive) */
	public static boolean equals(String tag1, String tag2) {
		return StringUtils.equalsIgnoreCase(tag1, tag2);
	}
	
	/** Finds the index of the tag in the list, using the special tag comparing rules (case insensitive) or -1 if not found */
	public static int indexOfTag(List<Tag> tags, Tag tag) {
		return indexOfTag(tags, tag.tag);
	}
	
	/** Finds the index of the tag in the list, using the special tag comparing rules (case insensitive) or -1 if not found */
	public static int indexOfTag(List<Tag> tags, String tag) {
		if (tags == null || tags.isEmpty() || tag == null) return -1;
		for (int i = 0; i < tags.size(); i++) {
			if (equals(tags.get(i).tag, tag)) return i;
		}
		return -1;
	}
	
	/** A contains implementation using the special tag comparing rules (case insensitive) */
	public static boolean contains(Collection<String> tags, String tag) {
		if (tags == null || tags.isEmpty() || tag == null || tag.length() == 0) return false;
		for (String t : tags) {
			if (equals(t, tag)) return true;
		}
		return false;
	}
	
	/** Helper for doing an addAll() between Tag and String types. It also will only add it if not already present in the collection. */
	public static boolean addAll(Collection<Tag> from, Collection<String> into) {
		if (from == null || from.isEmpty() || into == null) return false;
		boolean modified = false;
		for (Tag tag : from) {
			if (contains(into, tag.tag)) continue;
			into.add(tag.tag);
			modified = true;
		}
		return modified;
	}
	
}
