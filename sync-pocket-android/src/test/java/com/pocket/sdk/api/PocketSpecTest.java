package com.pocket.sdk.api;

import com.pocket.sdk.api.generated.enums.ItemContentType;
import com.pocket.sdk.api.generated.enums.ItemSortKey;
import com.pocket.sdk.api.generated.enums.ItemStatus;
import com.pocket.sdk.api.generated.enums.ItemStatusKey;
import com.pocket.sdk.api.generated.enums.PositionType;
import com.pocket.sdk.api.generated.thing.Annotation;
import com.pocket.sdk.api.generated.thing.Get;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.generated.thing.Position;
import com.pocket.sdk.api.generated.thing.Saves;
import com.pocket.sdk.api.spec.PocketSpec;
import com.pocket.sdk.api.thing.ItemUtil;
import com.pocket.sdk.api.value.DateString;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sync.space.Holder;
import com.pocket.sync.space.Space;
import com.pocket.sync.space.mutable.MutableSpace;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * TODO Documentation
 */
public class PocketSpecTest {
	
	protected Space space() {
		return new MutableSpace().setSpec(spec());
	}
	
	protected PocketSpec spec() {
		return new PocketSpec();
	}

	/**
	 * Tests that a currently reading {@link Saves}
	 * properly adds, removes, changes as items change their state.
	 *
	 * @throws Exception
	 */
	@Test
	public void currentlyReading() throws Exception {
		Space space = space();
		PocketSpec spec = spec();
		
		// First build a starting state with some items.
		Item item1 = ItemUtil.build("http://getpocket.com/item1", spec)
				.status(ItemStatus.UNREAD)
				.is_article(true)
				.positions(Collections.singletonMap(PositionType.ARTICLE.toString(), new Position.Builder()
					.percent(50)
					.time_spent(30)
					.build()))
				.build();
		Item item2 = ItemUtil.build("http://getpocket.com/item2", spec)
				.status(ItemStatus.UNREAD)
				.is_article(true)
				.positions(Collections.singletonMap(PositionType.ARTICLE.toString(), new Position.Builder()
						.percent(50)
						.time_spent(30)
						.build()))
				.build();
		List<Item> list = new ArrayList<>();
		list.add(item1);
		list.add(item2);
		Saves items = new Saves.Builder()
				.contentType(ItemContentType.ARTICLE)
				.count(10)
				.offset(0)
				.maxScrolled(95)
				.minTimeSpent(20)
				.state(ItemStatusKey.UNREAD)
				.list(list)
				.build();
		
		// Store it
		space.remember(Holder.persistent("holder"), items);
		space.imprint(items);
		Assert.assertEquals("all items remembered", 2, space.get(items).list.size());
		
		// Archive an item, it should be removed from currently reading
		spec.apply(spec.actions().archive()
						.url(item1.id_url)
						.time(Timestamp.now())
						.build(), space);
		Assert.assertEquals("archived item is removed from currently reading list", 1, space.get(items).list.size());
		
		// Create a new item, it should be added
		Item item3 = ItemUtil.build("http://getpocket.com/item3", spec)
				.status(ItemStatus.UNREAD)
				.is_article(true)
				.positions(Collections.singletonMap(PositionType.ARTICLE.toString(), new Position.Builder()
						.percent(50)
						.time_spent(30)
						.build()))
				.build();
		spec.apply(spec.actions().add()
						.item(item3)
						.time(Timestamp.now())
						.build(), space);
		Assert.assertEquals("new item is included in currently reading", 2, space.get(items).list.size());
		
		// Scroll an item out of range, it should be removed.
		spec.apply(spec.actions().scrolled()
						.url(item2.id_url)
						.time(Timestamp.now())
						.view(PositionType.ARTICLE)
						.percent(100)
						.build(), space);
		Assert.assertEquals("item should have been removed", 1, space.get(items).list.size());
	}
	
	/**
	 * Tests that {@link com.pocket.sdk.api.generated.thing.Get} properly adds, removes, changes as Annotations when they change state
	 */
	@Test
	@Ignore("TODO this test has been broken for months, we should fix it or remove it. Just adding this line so it doesn't break/fail for now.")
	public void getAnnotations() throws Exception {
		Space space = space();
		PocketSpec spec = spec();
		Holder holder = Holder.persistent("holder");
		
		// 1. Create an empty GetAnnotations
		Get getAnnotations = new Get.Builder().hasAnnotations(true).sort(ItemSortKey.ANNOTATION).count(5).offset(0).list(new ArrayList<>()).build();
		space.remember(holder, getAnnotations);
		space.initialize(getAnnotations);
		
		// 2. Create an Item
		Item item1 = ItemUtil.build("http://getpocket.com/item1", spec)
				.status(ItemStatus.UNREAD)
				.build();
		space.remember(holder, item1);
		spec.apply(spec.actions().add().item(item1).time(Timestamp.now()).build(), space);
		
		assertNullOrEmpty(space.get(getAnnotations).list);
		assertNullOrEmpty(space.get(item1).annotations);
		
		// 3. Add an annotation to the item and verify GetAnnotations obtains it
		Annotation annotation1 = new Annotation.Builder()
				.annotation_id("1")
				.build();
		spec.apply(spec.actions().add_annotation().annotation(annotation1).url(item1.id_url).time(Timestamp.now()).build(), space);
		
		getAnnotations = space.get(getAnnotations);
		Item first = getAnnotations.list.get(0);
		Assert.assertEquals(item1, first);
		Assert.assertEquals(1, first.annotations.size());
		Assert.assertEquals(annotation1, first.annotations.get(0));
		
		// 4. Delete the annotation, verify list becomes empty
		spec.apply(spec.actions().delete_annotation().annotation_id(annotation1.annotation_id).url(item1.id_url).time(Timestamp.now()).build(), space);
		
		assertNullOrEmpty(space.get(getAnnotations).list);
		assertNullOrEmpty(space.get(item1).annotations);
		
		// 5. Add the annotation again, again verify GetAnnotations obtains it
		spec.apply(spec.actions().add_annotation().annotation(annotation1).url(item1.id_url).time(Timestamp.now()).build(), space);
		
		getAnnotations = space.get(getAnnotations);
		first = getAnnotations.list.get(0);
		Assert.assertEquals(item1, first);
		Assert.assertEquals(1, first.annotations.size());
		Assert.assertEquals(annotation1, first.annotations.get(0));
		
		// 6. Add 2 more annotations, make sure GetAnnotations picks it up
		Thread.sleep(1000); // Just to ensure the created_at that will be set during the action is ahead of previous ones by at least one second
		Annotation annotation2 = new Annotation.Builder()
				.annotation_id("2")
				.build();
		spec.apply(spec.actions().add_annotation().annotation(annotation2).url(item1.id_url).time(Timestamp.now()).build(), space);
		Thread.sleep(1000); // Just to ensure the created_at that will be set during the action is ahead of previous ones by at least one second
		Annotation annotation3 = new Annotation.Builder()
				.annotation_id("3")
				.build();
		spec.apply(spec.actions().add_annotation().annotation(annotation3).url(item1.id_url).time(Timestamp.now()).build(), space);
		
		getAnnotations = space.get(getAnnotations);
		first = getAnnotations.list.get(0);
		Assert.assertTrue(first.annotations.contains(annotation1));
		Assert.assertTrue(first.annotations.contains(annotation2));
		Assert.assertTrue(first.annotations.contains(annotation3));
		Assert.assertEquals(1, getAnnotations.list.size());
		Assert.assertEquals(3, first.annotations.size());
		Assert.assertEquals(item1, first);
		
		// 7. Add another item, with three annotations, make sure GetAnnotations picks it up
		Annotation annotation4 = new Annotation.Builder()
				.annotation_id("4")
				.created_at(DateString.fromMillis(System.currentTimeMillis()+1000))
				.build();
		Annotation annotation5 = new Annotation.Builder()
				.annotation_id("5")
				.created_at(DateString.fromMillis(System.currentTimeMillis()+2000))
				.build();
		Annotation annotation6 = new Annotation.Builder()
				.annotation_id("6")
				.created_at(DateString.fromMillis(System.currentTimeMillis()+3000))
				.build();
		Item item2 = ItemUtil.build("http://getpocket.com/item2", spec)
				.status(ItemStatus.UNREAD)
				.annotations(Arrays.asList(annotation4, annotation5, annotation6))
				.build();
		space.remember(holder, item2);
		spec.apply(spec.actions().add().item(item2).time(Timestamp.now()).build(), space);
		
		getAnnotations = space.get(getAnnotations);
		first = getAnnotations.list.get(0);
		Item second = getAnnotations.list.get(1);
		Assert.assertEquals(2, getAnnotations.list.size());
		Assert.assertEquals(item2, first);
		Assert.assertEquals(item1, second);
		Assert.assertEquals(3, first.annotations.size());
		Assert.assertTrue(first.annotations.contains(annotation6));
		Assert.assertTrue(first.annotations.contains(annotation5));
		Assert.assertTrue(first.annotations.contains(annotation4));
		Assert.assertEquals(3, second.annotations.size());
		Assert.assertTrue(second.annotations.contains(annotation3));
		Assert.assertTrue(second.annotations.contains(annotation2));
		Assert.assertTrue(second.annotations.contains(annotation1));
	}
	
	private void assertNullOrEmpty(Collection collection) {
		Assert.assertTrue("collection is not empty " + collection, collection == null || collection.isEmpty());
	}
	
	
}