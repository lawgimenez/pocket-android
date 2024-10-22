package com.pocket.sdk.api.source;

import com.pocket.sdk.api.generated.thing.Guid;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.generated.thing.PocketShare;
import com.pocket.sync.space.Space;
import com.pocket.sync.spec.Resolver;
import com.pocket.sync.thing.Thing;

/**
 * Pocket's {@link Resolver} implementation.
 */
class PocketResolver implements Resolver {
	
	@Override
	public <T extends Thing> T resolve(T t, Space space) {
		if (t instanceof Item && ((Item)t).id_url == null) {
			// Try to find by item_id.
			Item item = (Item) t;
			Item found = (Item) space.where(Item.THING_TYPE, "item_id", item.item_id);
			if (found != null) {
				Item.Builder fix = item.builder();
				if (found.given_url != null) {
					return (T) fix.given_url(found.given_url).build();
				} else if (found.resolved_url != null) {
					return (T) fix.resolved_url(found.resolved_url).build();
				}
			}
		}
		return t;
	}
	
	@Override
	public <T extends Thing> T reduce(T t) {
		if (t instanceof Item) {
			Item item = (Item) t;
			final Item.Builder builder = item.identity().builder();
			
			if (item.declared.item_id) builder.item_id(item.item_id);
			if (item.declared.resolved_id) builder.resolved_id(item.resolved_id);
			if (item.declared.resolved_url) builder.resolved_url(item.resolved_url);
			if (item.declared.resolved_url_normalized) builder.resolved_url_normalized(item.resolved_url_normalized);
			if (item.declared.amp_url) builder.amp_url(item.amp_url);
			if (item.declared.title) builder.title(item.title);
			if (item.declared.excerpt) builder.excerpt(item.excerpt);
			if (item.declared.is_article) builder.is_article(item.is_article);
			if (item.declared.is_index) builder.is_index(item.is_index);
			if (item.declared.word_count) builder.word_count(item.word_count);
			if (item.declared.listen_duration_estimate) builder.listen_duration_estimate(item.listen_duration_estimate);
			if (item.declared.has_image) builder.has_image(item.has_image);
			if (item.declared.has_video) builder.has_video(item.has_video);
			if (item.declared.domain_metadata) builder.domain_metadata(item.domain_metadata);
			if (item.declared.top_image_url) builder.top_image_url(item.top_image_url);
			
			return (T) builder.build();
		} else if (t instanceof Guid) {
			return t; // Allow entire object to be returned.
		} else if (t instanceof PocketShare) {
			// Allow entire object to be imprinted (updated).
			return t;
		}
		return (T) t.identity();
	}
}
