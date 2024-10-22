package com.pocket.sdk.api;

import com.pocket.sdk.api.generated.enums.ItemStatus;
import com.pocket.sdk.api.generated.enums.PositionType;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.generated.thing.Position;
import com.pocket.sdk.api.generated.thing.Tag;
import com.pocket.sdk.api.spec.PocketSpec;
import com.pocket.sdk.api.thing.ItemUtil;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.api.value.UrlString;
import com.pocket.sync.space.Holder;
import com.pocket.sync.space.Space;
import com.pocket.sync.space.mutable.MutableSpace;

import junit.framework.Assert;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TODO Documentation
 */
public class ItemActionsSpecTest {
	
	// TODO check time updated and other time related fields
	
	protected Space space() {
		return new MutableSpace().setSpec(spec());
	}
	
	protected PocketSpec spec() {
		return new PocketSpec();
	}
	
	@Test
	public void addByUrl() throws Exception {
		Space space = space();
		PocketSpec spec = spec();
		Holder holder = Holder.persistent("holder");
		
		Item id = ItemUtil.create("http://getpocket.com", spec);
		space.remember(holder, id);
		
		spec.apply(spec.actions().add()
				.title("title")
				.tags(Arrays.asList("a", "b", "c"))
				.url(new UrlString("http://getpocket.com"))
				.ref_id("1234567890")
				.time(Timestamp.now())
				// TODO extended attribution and posts
				.build(), space);
		
		Item item = space.get(id);
		Assert.assertEquals("status is correct", ItemStatus.UNREAD, item.status);
		Assert.assertEquals("title is correct", "title", item.given_title);
		Assert.assertTrue("contains tag", item.tags.contains(new Tag.Builder().tag("a").build()));
		Assert.assertTrue("contains tag", item.tags.contains(new Tag.Builder().tag("b").build()));
		Assert.assertTrue("contains tag", item.tags.contains(new Tag.Builder().tag("c").build()));
		Assert.assertEquals("tweet id is correct", "1234567890", item.meta._1);
	}
	
	@Test
	public void addByItem() throws Exception {
		Space space = space();
		PocketSpec spec = spec();
		Holder holder = Holder.persistent("holder");
		
		Item item = ItemUtil.build("http://getpocket.com", spec)
				.given_title("title")
				.favorite(true)
				.build();
		space.remember(holder, item.identity());
		
		spec.apply(spec.actions().add()
				.item(item)
				.time(Timestamp.now())
				.build(), space);
		
		item = space.get(item.identity());
		Assert.assertEquals("status is correct", ItemStatus.UNREAD, item.status);
		Assert.assertEquals("title is correct", "title", item.given_title);
		Assert.assertTrue("favorite is correct", item.favorite);
	}
	
	@Test
	public void readd() throws Exception {
		Space space = space();
		PocketSpec spec = spec();
		Holder holder = Holder.persistent("holder");
		
		Item item = item(spec);
		
		space.remember(holder, item.identity());
		
		spec.apply(spec.actions().add()
				.item(item)
				.time(Timestamp.now())
				.build(), space);
		
		Assert.assertEquals("status is correct", ItemStatus.UNREAD, space.get(item).status);
		
		spec.apply(spec.actions().archive()
				.url(item.id_url)
				.time(Timestamp.now())
				.build(), space);
		
		Assert.assertEquals("status is correct", ItemStatus.ARCHIVED, space.get(item).status);
		
		spec.apply(spec.actions().readd()
				.item(item)
				.time(Timestamp.now())
				.build(), space);
		
		Assert.assertEquals("status is correct", ItemStatus.UNREAD, space.get(item).status);
	}
	
	@Test
	public void archive() throws Exception {
		Space space = space();
		PocketSpec spec = spec();
		Holder holder = Holder.persistent("holder");
		
		Item item = item(spec);
		
		space.remember(holder, item.identity());
		
		spec.apply(spec.actions().add()
				.item(item)
				.time(Timestamp.now())
				.build(), space);
		
		Assert.assertEquals("status is correct", ItemStatus.UNREAD, space.get(item).status);
		
		spec.apply(spec.actions().archive()
				.url(item.id_url)
				.time(Timestamp.now())
				.build(), space);
		
		Assert.assertEquals("status is correct", ItemStatus.ARCHIVED, space.get(item).status);
		
		spec.apply(spec.actions().undo_archive()
				.url(item.id_url)
				.time(Timestamp.now())
				.build(), space);
		
		Assert.assertEquals("status is correct", ItemStatus.UNREAD, space.get(item).status);
	}
	
	@Test
	public void delete() throws Exception {
		Space space = space();
		PocketSpec spec = spec();
		Holder holder = Holder.persistent("holder");
		
		Item item = item(spec);
		
		space.remember(holder, item.identity());
		
		spec.apply(spec.actions().add()
				.item(item)
				.time(Timestamp.now())
				.build(), space);
		
		Assert.assertEquals("status is correct", ItemStatus.UNREAD, space.get(item).status);
		
		spec.apply(spec.actions().delete()
				.url(item.id_url)
				.time(Timestamp.now())
				.build(), space);
		
		Assert.assertEquals("status is correct", ItemStatus.DELETED, space.get(item).status);
		
		spec.apply(spec.actions().undo_delete()
				.old_status(ItemStatus.UNREAD)
				.url(item.id_url)
				.time(Timestamp.now())
				.build(), space);
		
		Assert.assertEquals("status is correct", ItemStatus.UNREAD, space.get(item).status);
	}
	
	@Test
	public void scrolled() throws Exception {
		Space space = space();
		PocketSpec spec = spec();
		Holder holder = Holder.persistent("holder");
		
		Item item = item(spec);
		
		space.remember(holder, item.identity());
		space.imprint(item);
		
		spec.apply(spec.actions().scrolled()
				.view(PositionType.ARTICLE)
				.node_index(1)
				.page(2)
				.percent(3)
				.time_spent(4)
				.time_updated(Timestamp.now())
				.url(item.id_url)
				.time(Timestamp.now())
				.build(), space);
		
		item = space.get(item.identity());
		Position position = item.positions.get(PositionType.ARTICLE.toString());
		Assert.assertEquals(PositionType.ARTICLE, position.view);
		Assert.assertEquals(1, position.node_index.intValue());
		Assert.assertEquals(2, position.page.intValue());
		Assert.assertEquals(3, position.percent.intValue());
		Assert.assertEquals(4, position.time_spent.intValue());
	}
	
	@Test
	public void favoriting() throws Exception {
		Space space = space();
		PocketSpec spec = spec();
		Holder holder = Holder.persistent("holder");
		
		Item item = item(spec);
		
		space.remember(holder, item);
		space.imprint(item);
		
		spec.apply(spec.actions().favorite()
				.url(item.id_url)
				.time(Timestamp.now())
				.build(), space);
		
		Assert.assertTrue("favorited", space.get(item.identity()).favorite);
		
		spec.apply(spec.actions().unfavorite()
				.url(item.id_url)
				.time(Timestamp.now())
				.build(), space);
		
		Assert.assertFalse("unfavorited", space.get(item.identity()).favorite);
		
		
		spec.apply(spec.actions().favorite()
				.url(item.id_url)
				.time(Timestamp.now())
				.build(), space);
		
		Assert.assertTrue("favorited again", space.get(item.identity()).favorite);
	}
	
	@Test
	public void tagsAdd() throws Exception {
		Space space = space();
		PocketSpec spec = spec();
		Holder holder = Holder.persistent("holder");
		
		Item item = itemWithTags(spec);
		
		space.remember(holder, item);
		space.imprint(item);
		
		spec.apply(spec.actions().tags_add()
				.tags(Arrays.asList("a", "b", "c"))
				.url(item.id_url)
				.time(Timestamp.now())
				.build(), space);
		
		List<Tag> tags = space.get(item.identity()).tags;
		Assert.assertTrue("contains tag", tags.contains(new Tag.Builder().tag("a").build()));
		Assert.assertTrue("contains tag", tags.contains(new Tag.Builder().tag("b").build()));
		Assert.assertTrue("contains tag", tags.contains(new Tag.Builder().tag("c").build()));
		
		spec.apply(spec.actions().tags_add()
				.tags(Arrays.asList("a", "B", "g")) // B should not be duplicated since it is compared case-insensitively.
				.url(item.id_url)
				.time(Timestamp.now())
				.build(), space);
		
		tags = space.get(item.identity()).tags;
		Assert.assertTrue("contains tag", tags.contains(new Tag.Builder().tag("a").build()));
		Assert.assertTrue("contains tag", tags.contains(new Tag.Builder().tag("b").build()));
		Assert.assertTrue("contains tag", tags.contains(new Tag.Builder().tag("c").build()));
		Assert.assertTrue("contains tag", tags.contains(new Tag.Builder().tag("g").build()));
		Assert.assertEquals("size should indicate no duplicates", 4, tags.size());
	}
	
	@Test
	public void tagsClear() throws Exception {
		Space space = space();
		PocketSpec spec = spec();
		Holder holder = Holder.persistent("holder");
		
		Item item = itemWithTags(spec, "a", "b", "c");
		
		space.remember(holder, item);
		space.imprint(item);
		
		Assert.assertFalse("verify it has the starting tags", space.get(item.identity()).tags.isEmpty());
		
		spec.apply(spec.actions().tags_clear()
				.url(item.id_url)
				.time(Timestamp.now())
				.build(), space);
		
		Assert.assertTrue("contains tag", space.get(item.identity()).tags.isEmpty());
	}
	
	@Test
	public void tagsReplace() throws Exception {
		Space space = space();
		PocketSpec spec = spec();
		Holder holder = Holder.persistent("holder");
		
		Item item = itemWithTags(spec, "a", "b", "c");
		
		space.remember(holder, item);
		space.imprint(item);
		
		Assert.assertFalse("verify it has the starting tags", space.get(item.identity()).tags.isEmpty());
		
		spec.apply(spec.actions().tags_replace()
				.tags(Arrays.asList("c", "d", "e"))
				.url(item.id_url)
				.time(Timestamp.now())
				.build(), space);
		
		List<Tag> tags = space.get(item.identity()).tags;
		Assert.assertTrue("contains tag", tags.contains(new Tag.Builder().tag("c").build()));
		Assert.assertTrue("contains tag", tags.contains(new Tag.Builder().tag("d").build()));
		Assert.assertTrue("contains tag", tags.contains(new Tag.Builder().tag("e").build()));
		Assert.assertEquals("size should indicate no others", 3, tags.size());
	}
	
	@Test
	public void tagsRemove() throws Exception {
		Space space = space();
		PocketSpec spec = spec();
		Holder holder = Holder.persistent("holder");
		
		Item item = itemWithTags(spec, "a", "b", "c");
		
		space.remember(holder, item);
		space.imprint(item);
		
		Assert.assertFalse("verify it has the starting tags", space.get(item.identity()).tags.isEmpty());
		
		spec.apply(spec.actions().tags_remove()
				.tags(Arrays.asList("C", "b", "g")) // C and b tests case insensitive, g tests ignoring something not existing already
				.url(item.id_url)
				.time(Timestamp.now())
				.build(), space);
		
		List<Tag> tags = space.get(item.identity()).tags;
		Assert.assertTrue("contains tag", tags.contains(new Tag.Builder().tag("a").build()));
		Assert.assertEquals("size should indicate no others", 1, tags.size());
	}
	
	@Test
	public void tagRename() throws Exception {
		Space space = space();
		PocketSpec spec = spec();
		Holder holder = Holder.persistent("holder");
		
		Item item = itemWithTags(spec, "a", "b", "c");
		
		space.remember(holder, item);
		space.imprint(item);
		
		Assert.assertFalse("verify it has the starting tags", space.get(item.identity()).tags.isEmpty());
		
		spec.apply(spec.actions().tag_rename()
				.old_tag("b")
				.new_tag("bb")
				.time(Timestamp.now())
				.build(), space);
		
		List<Tag> tags = space.get(item.identity()).tags;
		Assert.assertTrue("contains tag", tags.contains(new Tag.Builder().tag("a").build()));
		Assert.assertTrue("contains tag", tags.contains(new Tag.Builder().tag("bb").build()));
		Assert.assertTrue("contains tag", tags.contains(new Tag.Builder().tag("c").build()));
		Assert.assertEquals("size should indicate no others", 3, tags.size());
	}
	
	@Test
	public void tagDelete() throws Exception {
		Space space = space();
		PocketSpec spec = spec();
		Holder holder = Holder.persistent("holder");
		
		Item item = itemWithTags(spec, "a", "b", "c");
		
		space.remember(holder, item);
		space.imprint(item);
		
		Assert.assertFalse("verify it has the starting tags", space.get(item.identity()).tags.isEmpty());
		
		spec.apply(spec.actions().tag_delete()
				.tag("b")
				.time(Timestamp.now())
				.build(), space);
		
		List<Tag> tags = space.get(item.identity()).tags;
		Assert.assertTrue("contains tag", tags.contains(new Tag.Builder().tag("a").build()));
		Assert.assertTrue("contains tag", tags.contains(new Tag.Builder().tag("c").build()));
		Assert.assertEquals("size should indicate no others", 2, tags.size());
	}
	
	private Item item(PocketSpec spec) {
		return ItemUtil.build("http://getpocket.com/item" + RandomUtils.nextInt(0, 5000000), spec)
				.status(ItemStatus.UNREAD)
				.build();
	}
	
	private Item itemWithTags(PocketSpec spec, String... tags) {
		List<Tag> list = null;
		if (tags.length > 0) {
			list = new ArrayList<>();
			for (String t : tags) {
				list.add(new Tag.Builder().tag(t).build());
			}
		}
		return new Item.Builder(item(spec))
				.tags(list)
				.build();
	}
	
}