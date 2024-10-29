package com.pocket.sync.space;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocket.sdk.api.generated.enums.ItemContentType;
import com.pocket.sdk.api.generated.enums.ItemStatus;
import com.pocket.sdk.api.generated.thing.Guid;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.generated.thing.Post;
import com.pocket.sdk.api.generated.thing.Profile;
import com.pocket.sdk.api.generated.thing.Saves;
import com.pocket.sdk.api.spec.PocketSpec;
import com.pocket.sdk.api.thing.ItemUtil;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sync.SyncAsserts;
import com.pocket.sync.SyncTestsSpec;
import com.pocket.sync.ThingMock;
import com.pocket.sync.source.JsonConfig;
import com.pocket.sync.source.subscribe.Changes;
import com.pocket.sync.spec.Spec;
import com.pocket.sync.spec.Syncable;
import com.pocket.sync.test.generated.thing.AnotherReactiveImplementation;
import com.pocket.sync.test.generated.thing.DeepCollectionsTest;
import com.pocket.sync.test.generated.thing.InterfaceAllIdentifiableImpl1;
import com.pocket.sync.test.generated.thing.InterfaceMixIdentifiable;
import com.pocket.sync.test.generated.thing.InterfaceMixIdentifiableDeep;
import com.pocket.sync.test.generated.thing.InterfaceMixIdentifiableId;
import com.pocket.sync.test.generated.thing.InterfaceMixIdentifiableNon;
import com.pocket.sync.test.generated.thing.NestedIdentity;
import com.pocket.sync.test.generated.thing.OpenUsages;
import com.pocket.sync.test.generated.thing.ReactionWatchedAlsoAsConcreteImplementation;
import com.pocket.sync.test.generated.thing.ReactionWatchedOnlyAsInterface;
import com.pocket.sync.test.generated.thing.ReactiveImplementation;
import com.pocket.sync.test.generated.thing.ReactiveInterface;
import com.pocket.sync.test.generated.thing.ReactiveTarget;
import com.pocket.sync.test.generated.thing.ReactiveThing;
import com.pocket.sync.test.generated.thing.SomethingWithIdentity;
import com.pocket.sync.test.generated.thing.UnknownReaction;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.thing.ThingUtil;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * TODO Documentation
 */
public abstract class SpaceTest {

	/** For testing purposes, no special Json parsing configuration rules are used here */
	private static final JsonConfig JSON_CONFIG = Syncable.NO_ALIASES;

	/**
	 * @return A new instance of space with the provided database name.
	 * 			Multiple calls with the same name should return new instances pointing to the same database.
	 */
	protected abstract Space instance(String name, Spec spec);
	
	/**
	 * @return A new space instance, pointing to the same database each time.
	 */
	protected Space instance() {
		return instance("test", spec());
	}
	
	protected PocketSpec spec() {
		return new PocketSpec();
	}
	
	@Test
	public void getDoesNotEffectState() throws Exception {
		Space space = instance();
		Assert.assertNull(space.get(new Guid.Builder().guid(null).build()));
		Assert.assertNull(space.get(new Guid.Builder().guid("value").build()));
	}
	
	@Test
	public void rememberDoesNotEffectState() throws Exception {
		Space space = instance();
		
		space.remember(Holder.persistent("holder"), new Guid.Builder().guid("state").build());
		Assert.assertNull(space.get(new Guid.Builder().guid(null).build()));
		Assert.assertNull(space.get(new Guid.Builder().guid("value").build()));
		
		space.imprint(new Guid.Builder().guid("actual").build());
		Assert.assertEquals(space.get(new Guid.Builder().guid(null).build()).guid, "actual");
		
		space.remember(Holder.persistent("holder2"), new Guid.Builder().guid("state2").build());
		Assert.assertEquals(space.get(new Guid.Builder().guid(null).build()).guid, "actual");
	}
	
	@Test
	public void rememberWithMultiples() throws Exception {
		Space space = instance();
		PocketSpec spec = spec();
		
		Guid id1 = new Guid.Builder().guid(null).build();
		Item id2 = ItemUtil.create("http://getpocket.com", spec);
		
		space.remember(Holder.persistent("holder"), id1, id2);
		
		space.imprint(id1.builder().guid("value").build());
		space.imprint(id2.builder().title("value").build());
		
		Assert.assertEquals(space.get(id1).guid, "value");
		Assert.assertEquals(space.get(id2).title, "value");
	}
	
	@Test
	public void updateDoesNotEffectNonRemembered() throws Exception {
		Space space = instance();
		
		Assert.assertNull(space.get(new Guid.Builder().guid(null).build()));
		space.imprint(new Guid.Builder().guid("value").build());
		Assert.assertNull(space.get(new Guid.Builder().guid(null).build()));
	}
	
	@Test
	public void update() throws Exception {
		Space space = instance();
		
		Assert.assertNull("space should return null if not remembered", space.get(new Guid.Builder().guid(null).build()));
		
		space.remember(Holder.persistent("holder"), new Guid.Builder().guid(null).build());
		
		Diff diff;
		
		space.startDiff();
		space.imprint(new Guid.Builder().guid("value").build());
		diff = space.endDiff();
		Assert.assertFalse("diff should include changes", diff.isEmpty());
		
		Assert.assertEquals("guid should be set", space.get(new Guid.Builder().guid(null).build()).guid, "value");
		
		space.startDiff();
		space.imprint(new Guid.Builder().guid("value2").build());
		diff = space.endDiff();
		Assert.assertFalse("diff should include changes", diff.isEmpty());
		Assert.assertEquals("new value should have been set", space.get(new Guid.Builder().guid(null).build()).guid, "value2");
		
		space.startDiff();
		space.imprint(new Guid.Builder().guid("value2").build());
		diff = space.endDiff();
		Assert.assertTrue("diff should be empty, since nothing changed", diff.isEmpty());
	}
	
	@Test
	public void forgetHolder() throws Exception {
		Space space = instance();
		PocketSpec spec = spec();
		
		Holder holder = Holder.persistent("holder");
		Guid guid = new Guid.Builder().guid("value").build();
		Item item = ItemUtil.build("http://getpocket.com", spec).title("value")
				.build();
		
		space.remember(holder, guid, item);
		space.imprint(guid);
		space.imprint(item);
		
		Assert.assertEquals("guid should now be set", space.get(guid).guid, "value");
		Assert.assertEquals("item should now be set", space.get(item).title, "value");
		
		space.forget(holder);
		Assert.assertNull("guid should have been released", space.get(guid));
		Assert.assertNull("item should have been released", space.get(item));
	}
	
	@Test
	public void forgetOne() throws Exception {
		Space space = instance();
		PocketSpec spec = spec();
		
		Holder holder = Holder.persistent("holder");
		Guid guid = new Guid.Builder().guid("value").build();
		Item item = ItemUtil.build("http://getpocket.com", spec)
				.title("value")
				.build();
		
		space.remember(holder, guid, item);
		space.imprint(guid);
		space.imprint(item);
		
		Assert.assertEquals("guid should now be set", space.get(guid).guid, "value");
		Assert.assertEquals("item should now be set", space.get(item).title, "value");
		
		space.forget(holder, guid);
		Assert.assertNull("guid should have been released", space.get(guid));
		Assert.assertEquals("item should still be set", space.get(item).title, "value");
	}
	
	@Test
	public void forgetMultiple() throws Exception {
		Space space = instance();
		PocketSpec spec = spec();
		
		space.remember(Holder.persistent("holder"),
				new Guid.Builder().guid(null).build(),
				ItemUtil.create("http://getpocket.com", spec));
		
		space.imprint(new Guid.Builder().guid("value").build());
		space.imprint(ItemUtil.build("http://getpocket.com", spec)
				.title("value")
				.build());
		
		Assert.assertEquals(space.get(new Guid.Builder().guid(null).build()).guid, "value");
		
		space.forget(Holder.persistent("holder"),
				new Guid.Builder().guid(null).build(),
				ItemUtil.create("http://getpocket.com", spec));
		
		Assert.assertNull(space.get(new Guid.Builder().guid(null).build()));
		Assert.assertNull(space.get(ItemUtil.create("http://getpocket.com", spec)));
	}
	
	@Test
	public void remembering() throws Exception {
		// given
		Space space = instance();
		PocketSpec spec = spec();
		Holder holder = Holder.persistent("holder");
		
		// when
		space.remember(holder,
				new Guid.Builder().guid(null).build(),
				ItemUtil.create("http://getpocket.com", spec));
		
		// then
		// WIP update this test with a new result test
	}
	
	@Test
	public void saveComplexNestedWithReferences() throws Exception {
		Space space = instance();
		Saves items = newComplexThing();
		space.remember(Holder.persistent("holder"), items);
		space.imprint(items);
		Assert.assertEquals(items, space.get(items.identity()));
	}
	
	@Test
	public void canGetNestedThingFromSavingItsParent() throws Exception {
		// given
		Space space = instance();
		Saves items = newComplexThing();
		
		// when
		space.remember(Holder.persistent("holder"), items);
		space.imprint(items);
		items = space.get(items); // Get the latest values that might have been derived
		
		// then
		Item item = items.list.get(0);
		SyncAsserts.equalsState(item, space.get(item.identity()));
	}
	
	@Test
	public void diffIncludesEffectedReferences() throws Exception {
		Space space = instance();
		Saves items = newComplexThing();
		space.remember(Holder.persistent("holder"), items);
		space.imprint(items);
		Item item = items.list.get(0);
		Item modified = item.builder().given_title("a new given title").build();
		space.startDiff();
		space.imprint(modified);
		Diff diff = space.endDiff();
		if (!diff.find(Changes.of(Saves.class)).isEmpty()) {
			Assert.assertEquals("includes new value", "a new given title", space.get(items).list.get(0).given_title);
		} else {
			Assert.fail("reference was not included");
		}
	}
	
	/**
	 * Verifies a field that is reactive to `Type` reacts properly.
	 */
	@Test
	public void reactiveToType() throws Exception {
		// given
		Space space = instance("r", new SyncTestsSpec());
		Holder holder = Holder.persistent("holder");
		ReactiveThing reactive = new ReactiveThing.Builder().id("reactive1").build();
		ReactiveTarget target = new ReactiveTarget.Builder().id("target1").build();
		space.remember(holder, reactive, target);
		space.imprint(target);
		space.imprint(reactive);
		long start = System.nanoTime();
		
		// when
		space.imprint(target.builder().b("value").build());
		
		// then
		ReactiveThing result = space.get(reactive);
		assertReacted(true, result.reactive_type, start);
		assertReacted(false, result.reactive_type_field, start);
		assertReacted(false, result.reactive_field, start);
		assertReacted(false, result.reactive_collection_field, start);
		assertReacted(true, result.reactive_self, start);
	}
	
	/**
	 * Verifies a field that is reactive to `Type.field` reacts properly.
	 */
	@Test
	public void reactiveToTypeField() throws Exception {
		// given
		Space space = instance("r", new SyncTestsSpec());
		Holder holder = Holder.persistent("holder");
		ReactiveThing reactive = new ReactiveThing.Builder().id("reactive1").build();
		ReactiveTarget target = new ReactiveTarget.Builder().id("target1").build();
		space.remember(holder, reactive, target);
		space.imprint(target);
		space.imprint(reactive);
		long start = System.nanoTime();
		
		// when
		space.imprint(target.builder().a("value").build());
		
		// then
		ReactiveThing result = space.get(reactive);
		assertReacted(true, result.reactive_type, start);
		assertReacted(true, result.reactive_type_field, start);
		assertReacted(false, result.reactive_field, start);
		assertReacted(false, result.reactive_collection_field, start);
		assertReacted(true, result.reactive_self, start);
	}
	
	/**
	 * Verifies a field that is reactive to `.field` reacts properly.
	 */
	@Test
	public void reactiveToField() throws Exception {
		// given
		Space space = instance("r", new SyncTestsSpec());
		Holder holder = Holder.persistent("holder");
		ReactiveThing reactive = new ReactiveThing.Builder().id("reactive1").build();
		space.remember(holder, reactive);
		space.imprint(reactive);
		long start = System.nanoTime();
		
		// when
		space.imprint(reactive.builder().field("value").build());
		
		// then
		ReactiveThing result = space.get(reactive);
		assertReacted(false, result.reactive_type, start);
		assertReacted(false, result.reactive_type_field, start);
		assertReacted(true, result.reactive_field, start);
		assertReacted(false, result.reactive_collection_field, start);
		assertReacted(true, result.reactive_self, start);
	}
	
	/**
	 * Verifies a field that is reactive to `.` reacts properly.
	 */
	@Test
	public void reactiveToSiblings() throws Exception {
		// given
		Space space = instance("r", new SyncTestsSpec());
		Holder holder = Holder.persistent("holder");
		ReactiveThing reactive = new ReactiveThing.Builder().id("reactive1").build();
		space.remember(holder, reactive);
		space.imprint(reactive);
		long start = System.nanoTime();
		
		// when
		space.imprint(reactive.builder().field2("value").build());
		
		// then
		ReactiveThing result = space.get(reactive);
		assertReacted(false, result.reactive_type, start);
		assertReacted(false, result.reactive_type_field, start);
		assertReacted(false, result.reactive_field, start);
		assertReacted(false, result.reactive_collection_field, start);
		assertReacted(true, result.reactive_self, start);
		
		// when
		space.imprint(reactive.builder().reactive_self("value").build());
		
		// then
		result = space.get(reactive);
		assertReacted(false, result.reactive_type, start);
		assertReacted(false, result.reactive_type_field, start);
		assertReacted(false, result.reactive_field, start);
		assertReacted(false, result.reactive_collection_field, start);
		Assert.assertEquals("field should not have reacted", "value", result.reactive_self);
	}
	
	/**
	 * Verifies a field that is reactive to `.` reacts properly.
	 */
	@Test
	public void reactiveToCollectionField() throws Exception {
		// given
		Space space = instance("r", new SyncTestsSpec());
		Holder holder = Holder.persistent("holder");
		SomethingWithIdentity sub1 = new SomethingWithIdentity.Builder().id("1").state("value").build();
		SomethingWithIdentity sub2 = new SomethingWithIdentity.Builder().id("2").state("value").build();
		ReactiveThing reactive = new ReactiveThing.Builder()
				.id("reactive1")
				.collection(Arrays.asList(sub1, sub2))
				.build();
		space.remember(holder, reactive);
		space.imprint(reactive);
		long start = System.nanoTime();
		
		// when
		space.imprint(sub2.builder().state("updated_value").build());
		
		// then
		ReactiveThing result = space.get(reactive);
		assertReacted(false, result.reactive_type, start);
		assertReacted(false, result.reactive_type_field, start);
		assertReacted(false, result.reactive_field, start);
		assertReacted(true, result.reactive_collection_field, start);
		assertReacted(true, result.reactive_self, start);
	}

	@Test
	public void reactiveToInterface() throws Exception {
		// given
		Space space = instance("r", new SyncTestsSpec());
		Holder holder = Holder.persistent("holder");
		ReactiveInterface reactive = new ReactiveImplementation.Builder().id("reactive").build();
		ReactionWatchedOnlyAsInterface reaction = new ReactionWatchedOnlyAsInterface.Builder().id("1").build();
		space.remember(holder, reactive, reaction);
		space.imprint(reactive);
		space.imprint(reaction);
		long start = System.nanoTime();
		
		// when
		space.imprint(reaction.builder().not_watched("update").build());
		
		// then
		ReactiveInterface result = space.get(reactive);
		assertReacted(false, result._reactive_interface_field(), start);
		assertReacted(false, result._reactive_implementation_field(), start);
		assertReacted(true, result._reactive_interface(), start);
		assertReacted(false, result._reactive_implementation(), start);
		assertReacted(false, result._reactive_self_field(), start);
		assertReacted(true, result._reactive_self(), start);
	}

	@Test
	public void reactiveToUnknownInterface() throws Exception {
		// given
		Space space = instance("r", new SyncTestsSpec());
		Holder holder = Holder.persistent("holder");
		ReactiveInterface reactive = new ReactiveImplementation.Builder().id("reactive").build();
		UnknownReaction reaction = new UnknownReaction.Builder().id("1").build();
		space.remember(holder, reactive, reaction);
		space.imprint(reactive);
		space.imprint(reaction);
		long start = System.nanoTime();
		
		// when
		space.imprint(reaction.builder().not_watched("update").build());
		
		// then
		ReactiveInterface result = space.get(reactive);
		assertReacted(false, result._reactive_interface_field(), start);
		assertReacted(false, result._reactive_implementation_field(), start);
		assertReacted(true, result._reactive_interface(), start);
		assertReacted(false, result._reactive_implementation(), start);
		assertReacted(false, result._reactive_self_field(), start);
		assertReacted(true, result._reactive_self(), start);
	}

	@Test
	public void reactiveToImplementation() throws Exception {
		// given
		Space space = instance("r", new SyncTestsSpec());
		Holder holder = Holder.persistent("holder");
		ReactiveInterface reactive = new ReactiveImplementation.Builder().id("reactive").build();
		ReactionWatchedAlsoAsConcreteImplementation reaction = new ReactionWatchedAlsoAsConcreteImplementation.Builder().id("1").build();
		space.remember(holder, reactive, reaction);
		space.imprint(reactive);
		space.imprint(reaction);
		long start = System.nanoTime();
		
		// when
		space.imprint(reaction.builder().not_watched("update").build());
		
		// then
		ReactiveInterface result = space.get(reactive);
		assertReacted(false, result._reactive_interface_field(), start);
		assertReacted(false, result._reactive_implementation_field(), start);
		assertReacted(true, result._reactive_interface(), start);
		assertReacted(true, result._reactive_implementation(), start);
		assertReacted(false, result._reactive_self_field(), start);
		assertReacted(true, result._reactive_self(), start);
	}

	@Test
	public void reactiveToInterfaceField() throws Exception {
		// given
		Space space = instance("r", new SyncTestsSpec());
		Holder holder = Holder.persistent("holder");
		ReactiveInterface reactive = new ReactiveImplementation.Builder().id("reactive").build();
		ReactionWatchedOnlyAsInterface reaction = new ReactionWatchedOnlyAsInterface.Builder().id("1").build();
		space.remember(holder, reactive, reaction);
		space.imprint(reactive);
		space.imprint(reaction);
		long start = System.nanoTime();
		
		// when
		space.imprint(reaction.builder().watched("update").build());
		
		// then
		ReactiveInterface result = space.get(reactive);
		assertReacted(true, result._reactive_interface_field(), start);
		assertReacted(false, result._reactive_implementation_field(), start);
		assertReacted(true, result._reactive_interface(), start);
		assertReacted(false, result._reactive_implementation(), start);
		assertReacted(false, result._reactive_self_field(), start);
		assertReacted(true, result._reactive_self(), start);
	}

	@Test
	public void reactiveToUnknownInterfaceField() throws Exception {
		// given
		Space space = instance("r", new SyncTestsSpec());
		Holder holder = Holder.persistent("holder");
		ReactiveInterface reactive = new ReactiveImplementation.Builder().id("reactive").build();
		UnknownReaction reaction = new UnknownReaction.Builder().id("1").build();
		space.remember(holder, reactive, reaction);
		space.imprint(reactive);
		space.imprint(reaction);
		long start = System.nanoTime();
		
		// when
		space.imprint(reaction.builder().watched("update").build());
		
		// then
		ReactiveInterface result = space.get(reactive);
		assertReacted(true, result._reactive_interface_field(), start);
		assertReacted(false, result._reactive_implementation_field(), start);
		assertReacted(true, result._reactive_interface(), start);
		assertReacted(false, result._reactive_implementation(), start);
		assertReacted(false, result._reactive_self_field(), start);
		assertReacted(true, result._reactive_self(), start);
	}

	@Test
	public void reactiveToImplementationField() throws Exception {
		// given
		Space space = instance("r", new SyncTestsSpec());
		Holder holder = Holder.persistent("holder");
		ReactiveInterface reactive = new ReactiveImplementation.Builder().id("reactive").build();
		ReactionWatchedAlsoAsConcreteImplementation reaction = new ReactionWatchedAlsoAsConcreteImplementation.Builder().id("1").build();
		space.remember(holder, reactive, reaction);
		space.imprint(reactive);
		space.imprint(reaction);
		long start = System.nanoTime();
		
		// when
		space.imprint(reaction.builder().watched("update").build());
		
		// then
		ReactiveInterface result = space.get(reactive);
		assertReacted(true, result._reactive_interface_field(), start);
		assertReacted(true, result._reactive_implementation_field(), start);
		assertReacted(true, result._reactive_interface(), start);
		assertReacted(true, result._reactive_implementation(), start);
		assertReacted(false, result._reactive_self_field(), start);
		assertReacted(true, result._reactive_self(), start);
	}

	@Test
	public void reactiveToImplementationSelfField() throws Exception {
		// given
		Space space = instance("r", new SyncTestsSpec());
		Holder holder = Holder.persistent("holder");
		ReactiveImplementation reactive = new ReactiveImplementation.Builder().id("reactive").build();
		space.remember(holder, reactive);
		space.imprint(reactive);
		long start = System.nanoTime();
		
		// when
		space.imprint(reactive.builder().watched("update").build());
		
		// then
		ReactiveInterface result = space.get(reactive);
		assertReacted(false, result._reactive_interface_field(), start);
		assertReacted(false, result._reactive_implementation_field(), start);
		assertReacted(false, result._reactive_interface(), start);
		assertReacted(false, result._reactive_implementation(), start);
		assertReacted(true, result._reactive_self_field(), start);
		assertReacted(true, result._reactive_self(), start);
	}

	@Test
	public void reactiveToImplementationSelf() throws Exception {
		// given
		Space space = instance("r", new SyncTestsSpec());
		Holder holder = Holder.persistent("holder");
		ReactiveImplementation reactive = new ReactiveImplementation.Builder().id("reactive").build();
		space.remember(holder, reactive);
		space.imprint(reactive);
		long start = System.nanoTime();
		
		// when
		space.imprint(reactive.builder().not_watched("update").build());
		
		// then
		ReactiveInterface result = space.get(reactive);
		assertReacted(false, result._reactive_interface_field(), start);
		assertReacted(false, result._reactive_implementation_field(), start);
		assertReacted(false, result._reactive_interface(), start);
		assertReacted(false, result._reactive_implementation(), start);
		assertReacted(false, result._reactive_self_field(), start);
		assertReacted(true, result._reactive_self(), start);
	}

	@Test
	public void reactiveToAnotherImplementation() throws Exception {
		// given
		Space space = instance("r", new SyncTestsSpec());
		Holder holder = Holder.persistent("holder");
		ReactiveInterface reactive = new ReactiveImplementation.Builder().id("reactive").build();
		AnotherReactiveImplementation another = new AnotherReactiveImplementation.Builder().id("another").build();
		space.remember(holder, reactive, another);
		space.imprint(reactive);
		long start = System.nanoTime();
		
		// when
		space.imprint(another.builder().not_watched("update").build());
		space.imprint(another.builder().watched("update").build());
		
		// then
		ReactiveInterface result = space.get(reactive);
		assertReacted(false, result._reactive_interface_field(), start);
		assertReacted(false, result._reactive_implementation_field(), start);
		assertReacted(false, result._reactive_interface(), start);
		assertReacted(false, result._reactive_implementation(), start);
		assertReacted(false, result._reactive_self_field(), start);
		assertReacted(false, result._reactive_self(), start);
	}

	private void assertReacted(boolean reacted, String fieldValue, long preImprintTime) {
		Assert.assertEquals(reacted ? "field should have reacted" : "field should NOT have reacted", reacted, Long.parseLong(fieldValue) >= preImprintTime);
	}
	
	private Saves newComplexThing() {
		PocketSpec spec = spec();
		Profile profile = new Profile.Builder()
				.uid("12312")
				.username("max")
				.build();
		List<Item> list = new ArrayList<>();
		list.add(ItemUtil.build("http://getpocket.com/item1", spec)
				.status(ItemStatus.UNREAD)
				.is_article(true)
				.time_added(Timestamp.fromMillis(System.currentTimeMillis()+2000))
				.posts(Collections.singletonList(new Post.Builder()
						.post_id("post1")
						.profile(profile)
						.build()))
				.build());
		list.add(ItemUtil.build("http://getpocket.com/item2", spec)
				.status(ItemStatus.ARCHIVED)
				.is_article(true)
				.time_added(Timestamp.now())
				.posts(Collections.singletonList(new Post.Builder()
						.post_id("post2")
						.profile(profile)
						.build()))
				.build());
		return new Saves.Builder()
				.list(list)
				.contentType(ItemContentType.ARTICLE)
				.build();
	}
	
	@Test
	public void imprint() throws Exception {
		imprint(ThingMock.thing().getNotifications());
		imprint(ThingMock.thing().getProfileFeed());
	}
	
	private void imprint(Thing thing) {
		Space space = instance();
		space.clear();
		space.remember(Holder.persistent("test"), thing);
		
		// First imprint as new
		space.imprint(thing);
		Thing result1 = space.get(thing);
		SyncAsserts.equalsState(thing, result1, false);
		
		// Second imprint the same again
		space.imprint(thing);
		Thing result2 = space.get(thing);
		SyncAsserts.equalsState(thing, result2, false);
		
		if (space instanceof Space.Persisted) {
			// Close the database and reload from disk, and then get
			space.release();
			space = instance();
			Thing result3 = space.get(thing);
			SyncAsserts.equalsState(thing, result3, false);
		}
	}
	
	/**
	 * Run a stress test of all of the various cases of tables, making sure they are all handled.
	 *
	 * If you need to generate a test instance, see {@link DeepCollectionBuilder}, but there will already be a deepcollections.json
	 * ready for use.
	 */
	@Test
	public void collectionsNest() throws Exception {
		Space space = instance("g", new SyncTestsSpec());
		Holder holder = Holder.persistent("hold");

		DeepCollectionsTest deep = ThingMock.thing().deepCollections();
		space.remember(holder, deep);
		space.imprint(deep);

		SyncAsserts.equalsState(deep, space.get(deep));
	}
	
	/**
	 * Tests a variety of changes that can happen to nested collections.
	 */
	@Test
	public void collectionChanges() throws Exception {
		Space space = instance("g", new SyncTestsSpec());
		Holder holder = Holder.persistent("hold");

		DeepCollectionsTest deep = ThingMock.thing().deepCollections();
		ObjectMapper mapper = new ObjectMapper();

		// Nulling a root collection with many nested collections
		modifyImprintCompare(holder, deep, space, (thing, json) -> json.put("ref_list0", (ArrayNode) null));
		modifyImprintCompare(holder, deep, space, (thing, json) -> json.put("val_list0", (ArrayNode) null));
		modifyImprintCompare(holder, deep, space, (thing, json) -> json.put("obj_list0", (ArrayNode) null));
		modifyImprintCompare(holder, deep, space, (thing, json) -> json.put("ref_map0", (ObjectNode) null));
		modifyImprintCompare(holder, deep, space, (thing, json) -> json.put("val_map0", (ObjectNode) null));
		modifyImprintCompare(holder, deep, space, (thing, json) -> json.put("obj_map0", (ObjectNode) null));

		// Emptying a root collection with many nested collections
		modifyImprintCompare(holder, deep, space, (thing, json) -> json.put("ref_list0", mapper.createArrayNode()));
		modifyImprintCompare(holder, deep, space, (thing, json) -> json.put("val_list0", mapper.createArrayNode()));
		modifyImprintCompare(holder, deep, space, (thing, json) -> json.put("obj_list0", mapper.createArrayNode()));
		modifyImprintCompare(holder, deep, space, (thing, json) -> json.put("ref_map0", mapper.createObjectNode()));
		modifyImprintCompare(holder, deep, space, (thing, json) -> json.put("val_map0", mapper.createObjectNode()));
		modifyImprintCompare(holder, deep, space, (thing, json) -> json.put("obj_map0", mapper.createObjectNode()));

		// Removing first from a root list
		modifyImprintCompare(holder, deep, space, (thing, json) -> ((ArrayNode) json.get("ref_list0")).remove(0));
		modifyImprintCompare(holder, deep, space, (thing, json) -> ((ArrayNode) json.get("val_list0")).remove(0));
		modifyImprintCompare(holder, deep, space, (thing, json) -> ((ArrayNode) json.get("obj_list0")).remove(0));
		// Removing last from a root list
		modifyImprintCompare(holder, deep, space, (thing, json) -> {
			ArrayNode list = (ArrayNode) json.get("ref_list0");
			list.remove(list.size()-1);
		});
		modifyImprintCompare(holder, deep, space, (thing, json) -> {
			ArrayNode list = (ArrayNode) json.get("val_list0");
			list.remove(list.size()-1);
		});
		modifyImprintCompare(holder, deep, space, (thing, json) -> {
			ArrayNode list = (ArrayNode) json.get("obj_list0");
			list.remove(list.size()-1);
		});

		// Remove an element from a root map
		modifyImprintCompare(holder, deep, space, (thing, json) -> {
			Iterator<JsonNode> it = json.get("ref_map0").iterator();
			it.next();
			it.remove();
		});
		modifyImprintCompare(holder, deep, space, (thing, json) -> {
			Iterator<JsonNode> it = json.get("val_map0").iterator();
			it.next();
			it.remove();
		});
		modifyImprintCompare(holder, deep, space, (thing, json) -> {
			Iterator<JsonNode> it = json.get("obj_map0").iterator();
			it.next();
			it.remove();
		});
		
		// Reorder list
		modifyImprintCompare(holder, deep, space, (thing, json) -> {
			ArrayNode list = (ArrayNode) json.get("ref_list0");
			ArrayNode reverse = mapper.createArrayNode();
			for (int i = list.size()-1; i >= 0; i--) {
				reverse.add(list.get(i));
			}
			json.set("ref_list0", reverse);
		});
		modifyImprintCompare(holder, deep, space, (thing, json) -> {
			ArrayNode list = (ArrayNode) json.get("val_list0");
			ArrayNode reverse = mapper.createArrayNode();
			for (int i = list.size()-1; i >= 0; i--) {
				reverse.add(list.get(i));
			}
			json.set("val_list0", reverse);
		});
		modifyImprintCompare(holder, deep, space, (thing, json) -> {
			ArrayNode list = (ArrayNode) json.get("obj_list0");
			ArrayNode reverse = mapper.createArrayNode();
			for (int i = list.size()-1; i >= 0; i--) {
				reverse.add(list.get(i));
			}
			json.set("obj_list0", reverse);
		});
		
		// Modify deep element
		modifyImprintCompare(holder, deep, space, (thing, json) -> ((ObjectNode) json.get("obj_list0").get(0)).put("val1", 3));
		modifyImprintCompare(holder, deep, space, (thing, json) -> ((ObjectNode) json.get("obj_map0").iterator().next()).put("val1", 2));
		
		// Imprint the same thing, but with a reference collection undeclared.
		// Along with another misc change to make it seen as an actual change
		// It should keep the existing collection, since the incoming imprint doesn't declare it
		space.clear();
		space.remember(holder, deep);
		space.imprint(deep);
		ObjectNode j = deep.toJson(JSON_CONFIG);
		j.remove("ref_list0");
		j.remove("ref_map0");
		space.startDiff();
		space.imprint(new DeepCollectionsTest.Builder(DeepCollectionsTest.from(j, JSON_CONFIG)).val0(deep.val0+1).build());
		Assert.assertTrue(ThingUtil.listEquals(Thing.Equality.STATE, space.get(deep).ref_list0, deep.ref_list0));
		Assert.assertTrue(ThingUtil.mapEquals(Thing.Equality.STATE, space.get(deep).ref_map0, deep.ref_map0));
		
		// TODO some other things to write tests for
		// Add an element to tail of list
		// Add an element to beginning of list
		// Add an element to map
		// A lot of the same ones above but working with a deeper element instead of the root one
	}
	
	/**
	 * Verifies that the space keeps the structure of this change between imprinting and getting
	 */
	private <T extends Thing> void modifyImprintCompare(Holder holder, T thing, Space space, Modify<T> modify) {
		// Get the imprinted starting version of the thing. Since the space might derive something, we want to get the post-imprinted state
		space.clear();
		space.remember(holder, thing);
		space.imprint(thing);
		thing = space.get(thing);
		
		// Make a new instance that has the modification
		ObjectNode json = thing.toJson(JSON_CONFIG);
		modify.modify(thing, json);
		thing = (T) thing.getCreator().create(json, JSON_CONFIG);
		
		// Imprint the change
		space.imprint(thing);
		
		// Ensure that the space returns the change when getting it after the imprint
		SyncAsserts.equalsState(thing, space.get(thing));
	}
	
	interface Modify<T> {
		void modify(T thing, ObjectNode json);
	}

	@Test
	public void changes_within_a_field_that_is_an_interface_of_identifiable_things() {
		// given
		Space space = instance();
		InterfaceAllIdentifiableImpl1 start = new InterfaceAllIdentifiableImpl1.Builder().id_i("A").state("1").build();
		InterfaceAllIdentifiableImpl1 replace = start.builder().state("2").build();
		OpenUsages parent = new OpenUsages.Builder().interface_of_ids(start).build();

		// initial imprint
		space.remember(Holder.persistent("holder"), parent);
		space.imprint(parent);
		// Double check the initial imprint is correct
		SyncAsserts.equalsState(parent, space.get(parent));
		SyncAsserts.equalsState(start, space.get(start));

		// imprint the change
		space.imprint(replace);

		// then
		SyncAsserts.equalsState(replace, space.get(replace));
		// Double check the change propagated to its parent
		SyncAsserts.equalsState(replace, space.get(parent).interface_of_ids);
	}

	@Test
	public void changes_within_a_field_that_is_an_interface_of_a_mixture_of_identifiable_and_non_identifiable_things() {
		// given
		Space space = instance();
		InterfaceMixIdentifiableId start = new InterfaceMixIdentifiableId.Builder().id("A").state_i("1").build();
		InterfaceMixIdentifiableId replace = start.builder().state_i("2").build();
		ArrayList<InterfaceMixIdentifiable> list = new ArrayList<>();
		list.add(start);
		list.add(new InterfaceMixIdentifiableNon.Builder().state("non").build());
		OpenUsages parent = new OpenUsages.Builder().interface_of_mix_ids_list(list).build();

		// initial imprint
		space.remember(Holder.persistent("holder"), parent);
		space.imprint(parent);
		// Double check the initial imprint is correct
		SyncAsserts.equalsState(parent, space.get(parent));
		SyncAsserts.equalsState(start, space.get(start));

		// imprint the change
		space.imprint(replace);

		// then
		SyncAsserts.equalsState(replace, space.get(replace));
		Assert.assertEquals(list, space.get(parent).interface_of_mix_ids_list);
		// Double check the change propagated to its parent
		SyncAsserts.equalsState(replace, space.get(parent).interface_of_mix_ids_list.get(0));
	}

	@Test
	public void changes_within_a_deep_field_that_is_an_interface_of_a_mixture_of_identifiable_and_non_identifiable_things() {
		// given
		Space space = instance();
		SomethingWithIdentity start = new SomethingWithIdentity.Builder().id("A").state("1").build();
		SomethingWithIdentity replace = start.builder().state("2").build();
		InterfaceMixIdentifiable wrapper = new InterfaceMixIdentifiableDeep.Builder()
						.state_contains_id(new NestedIdentity.Builder()
								.nested(start).build()
						).build();
		ArrayList<InterfaceMixIdentifiable> list = new ArrayList<>();
		list.add(wrapper);
		list.add(new InterfaceMixIdentifiableNon.Builder().state("non").build());
		OpenUsages parent = new OpenUsages.Builder().interface_of_mix_ids_list(list).build();

		// initial imprint
		space.remember(Holder.persistent("holder"), parent);
		space.imprint(parent);
		// Double check the initial imprint is correct
		SyncAsserts.equalsState(parent, space.get(parent));
		SyncAsserts.equalsState(start, space.get(start));

		// imprint the change
		space.imprint(replace);

		// then
		SyncAsserts.equalsState(replace, space.get(replace));
		Assert.assertEquals(list, space.get(parent).interface_of_mix_ids_list);
		// Double check the change propagated to its parent
		InterfaceMixIdentifiableDeep deep = (InterfaceMixIdentifiableDeep) space.get(parent).interface_of_mix_ids_list.get(0);
		SyncAsserts.equalsState(replace, deep.state_contains_id.nested);
	}

}