package com.pocket.sync;

import com.pocket.sdk.api.generated.enums.ItemStatus;
import com.pocket.sdk.api.generated.thing.Author;
import com.pocket.sdk.api.generated.thing.Feed;
import com.pocket.sdk.api.generated.thing.FeedItem;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.generated.thing.Post;
import com.pocket.sdk.api.generated.thing.Profile;
import com.pocket.sdk.api.generated.thing.Video;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.api.value.UrlString;
import com.pocket.sync.action.Action;
import com.pocket.sync.test.generated.action.EqualityAction;
import com.pocket.sync.test.generated.thing.DeepCollectionsTest;
import com.pocket.sync.test.generated.thing.Depth1;
import com.pocket.sync.test.generated.thing.HasComplexIdentityWithNonIdThing;
import com.pocket.sync.test.generated.thing.HasComplexIdentityWithThing;
import com.pocket.sync.test.generated.thing.HasIdentityWithNonIdThing;
import com.pocket.sync.test.generated.thing.HasIdentityWithThing;
import com.pocket.sync.test.generated.thing.HasNestedIdentity;
import com.pocket.sync.test.generated.thing.NestedIdentity;
import com.pocket.sync.test.generated.thing.SomethingElseWithoutIdentity;
import com.pocket.sync.test.generated.thing.SomethingWithIdentity;
import com.pocket.sync.test.generated.thing.SomethingWithoutIdentity;
import com.pocket.sync.thing.Thing;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests various versions of identity and equality to make sure equals() is performing as expected
 */
public class EqualityTest {
	
	@Test
	public void identity() {
		Thing a = new Profile.Builder()
				.uid("123456")
				.is_following(false)
				.build();
		
		Thing b = new Profile.Builder()
				.uid("123456")
				.is_following(true)
				.build();
		
		Thing c = new Profile.Builder()
				.uid("123456")
				.build();
		
		Thing d = new Post.Builder()
				.post_id("654321")
				.build();
		
		// Equals
		Assert.assertEquals("default equals should be identity", a, b);
		Assert.assertTrue(a.equals(Thing.Equality.IDENTITY, b));
		Assert.assertTrue(a.equals(Thing.Equality.IDENTITY, c));
		Assert.assertTrue(b.equals(Thing.Equality.IDENTITY, c));
		Assert.assertFalse(a.equals(Thing.Equality.IDENTITY, d));
		
		// Hashcode
		Assert.assertEquals("default hashcode should be identity", a.hashCode(), b.hashCode());
		Assert.assertEquals(a.hashCode(Thing.Equality.IDENTITY), b.hashCode(Thing.Equality.IDENTITY));
		Assert.assertEquals(a.hashCode(Thing.Equality.IDENTITY), c.hashCode(Thing.Equality.IDENTITY));
		Assert.assertEquals(b.hashCode(Thing.Equality.IDENTITY), c.hashCode(Thing.Equality.IDENTITY));
		Assert.assertNotEquals(a.hashCode(Thing.Equality.IDENTITY), d.hashCode(Thing.Equality.IDENTITY));
	}
	
	@Test
	public void state() {
		Thing a = new Item.Builder()
				.item_id("123456")
				.given_url(new UrlString("pocket.co"))
				.time_added(Timestamp.fromMillis(123456))
				.build();
		
		Thing b = new Item.Builder()
				.item_id("123456")
				.given_url(new UrlString("getpocket.com"))
				.time_added(Timestamp.fromMillis(123456))
				.build();
		
		Thing c = new Item.Builder()
				.item_id("123456")
				.build();
		
		Thing d = new Item.Builder()
				.item_id("654321")
				.build();
		
		Thing e = new Item.Builder()
				.item_id("123456")
				.given_url(new UrlString("getpocket.com"))
				.time_added(Timestamp.fromMillis(123456))
				.build();
		
		// Equals
		Assert.assertTrue(b.equals(Thing.Equality.STATE, e));
		Assert.assertFalse(a.equals(Thing.Equality.STATE, b));
		Assert.assertFalse(a.equals(Thing.Equality.STATE, c));
		Assert.assertFalse(a.equals(Thing.Equality.STATE, d));
		
		// Hashcode
		Assert.assertEquals(b.hashCode(Thing.Equality.STATE), e.hashCode(Thing.Equality.STATE));
		Assert.assertNotEquals(a.hashCode(Thing.Equality.STATE), b.hashCode(Thing.Equality.STATE));
		Assert.assertNotEquals(a.hashCode(Thing.Equality.STATE), c.hashCode(Thing.Equality.STATE));
		Assert.assertNotEquals(a.hashCode(Thing.Equality.STATE), d.hashCode(Thing.Equality.STATE));
	}
	
	@Test
	public void hashcodeDifferences() throws IOException {
		Thing thing = ThingMock.thing().feed();
		int identity = thing.hashCode(Thing.Equality.IDENTITY);
		int state = thing.hashCode(Thing.Equality.STATE);
		int flat = thing.hashCode(Thing.Equality.FLAT);
		Assert.assertNotEquals(identity, state);
		Assert.assertNotEquals(flat, state);
	}
	
	@Test
	public void parentStatesIgnored() {
		Author bob9 = new Author.Builder()
				.author_id(1)
				.name("Bob")
				.build();
		
		Author bob4 = new Author.Builder()
				.author_id(1)
				.name("Bob")
				.build();
		
		Author bob = new Author.Builder()
				.author_id(1)
				.name("Bob")
				.build();
		
		// Equality
		Assert.assertTrue(bob9.equals(Thing.Equality.STATE, bob4));
		Assert.assertTrue(bob4.equals(Thing.Equality.STATE, bob));
		Assert.assertTrue(bob9.equals(Thing.Equality.IDENTITY, bob4));
		Assert.assertTrue(bob4.equals(Thing.Equality.IDENTITY, bob));
		
		// Hashcode
		Assert.assertEquals(bob9.hashCode(Thing.Equality.STATE), bob4.hashCode(Thing.Equality.STATE));
		Assert.assertEquals(bob4.hashCode(Thing.Equality.STATE), bob.hashCode(Thing.Equality.STATE));
		Assert.assertEquals(bob9.hashCode(Thing.Equality.IDENTITY), bob4.hashCode(Thing.Equality.IDENTITY));
		Assert.assertEquals(bob4.hashCode(Thing.Equality.IDENTITY), bob.hashCode(Thing.Equality.IDENTITY));
	}
	
	
	@Test
	public void lists() throws Exception {
		Feed feed = ThingMock.thing().feed();
		List<FeedItem> list = new ArrayList<>(feed.feed);
		list.set(1, new FeedItem.Builder(list.get(1))
				.sort_id(100000)
				.build());
		Feed feed2 = feed.builder().feed(list).build();
		
		// Equality
		Assert.assertEquals("same identity", feed, feed2);
		Assert.assertFalse("different state", feed.equals(Thing.Equality.STATE, feed2));
		
		// Hashcode
		Assert.assertEquals("same identity", feed.hashCode(), feed2.hashCode());
		Assert.assertNotEquals("different state", feed.hashCode(Thing.Equality.STATE), feed2.hashCode(Thing.Equality.STATE));
	}
	
	@Test
	public void maps() throws Exception {
		Map<String, Depth1> map = new HashMap<>();
		map.put("1", new Depth1.Builder().val1(1).build());
		map.put("2", new Depth1.Builder().val1(2).build());
		DeepCollectionsTest before = new DeepCollectionsTest.Builder().id("1").obj_map0(map).build();
		map.put("3", new Depth1.Builder().val1(3).build());
		DeepCollectionsTest after = before.builder().obj_map0(map).build();
		
		// Equality
		Assert.assertEquals("same identity", before, after);
		Assert.assertFalse("different state", before.equals(Thing.Equality.STATE, after));

		// Hashcode
		Assert.assertEquals("same identity", before.hashCode(), after.hashCode());
		Assert.assertNotEquals("different state", before.hashCode(Thing.Equality.STATE), after.hashCode(Thing.Equality.STATE));
	}
	
	@Test
	public void nonIdentifibles() throws Exception {
		Thing a = new Video.Builder().video_id(1).build();
		Thing b = new Video.Builder().video_id(2).build();
		Thing c = new Video.Builder().video_id(1).build();
		
		Assert.assertFalse("Must use a non-identifible thing for this test.", a.isIdentifiable());
		Assert.assertFalse("Must use a non-identifible thing for this test.", b.isIdentifiable());
		Assert.assertFalse("Must use a non-identifible thing for this test.", c.isIdentifiable());
		
		// Equality
		Assert.assertNotEquals(a, b);
		Assert.assertEquals(a, c);
		
		// Hashcode
		Assert.assertNotEquals(a.hashCode(), b.hashCode());
		Assert.assertEquals(a.hashCode(), c.hashCode());
	}
	
	@Test
	public void flat() throws Exception {
		Feed one = ThingMock.thing().feed();
		
		// Create a variant that has a slight state change within an inner identifiable thing.
		List<FeedItem> list = new ArrayList<>(one.feed);
		list.set(1, new FeedItem.Builder(list.get(1))
				.sort_id(100000)
				.build());
		Thing two = one.builder().feed(list).build();
		
		// Create a variant with a top level state change
		Thing three = one.builder().count(123).build();
		
		// Create a variant with a different ordering of feed
		list = new ArrayList<>(one.feed);
		list.add(list.remove(0));
		Thing four = one.builder().count(123).build();
		
		// Equality
		Assert.assertTrue(one.equals(Thing.Equality.FLAT, two));
		Assert.assertFalse(one.equals(Thing.Equality.FLAT, three));
		Assert.assertFalse(one.equals(Thing.Equality.FLAT, four));
		
		// Hashcode
		Assert.assertEquals(one.hashCode(Thing.Equality.FLAT), two.hashCode(Thing.Equality.FLAT));
		Assert.assertNotEquals(one.hashCode(Thing.Equality.FLAT), three.hashCode(Thing.Equality.FLAT));
		Assert.assertNotEquals(one.hashCode(Thing.Equality.FLAT), four.hashCode(Thing.Equality.FLAT));
	}
	
	@Test
	public void state_declared() {
		Assert.assertTrue(
				new Item.Builder().status(ItemStatus.UNREAD).build()
						.equals(Thing.Equality.STATE_DECLARED,
				new Item.Builder().status(ItemStatus.UNREAD).is_article(false).build()));
		
		Assert.assertFalse(
				new Item.Builder().status(ItemStatus.UNREAD).build()
						.equals(Thing.Equality.STATE_DECLARED,
				new Item.Builder().status(ItemStatus.ARCHIVED).build()));
	}
	
	/**
	 * Test identity equality when an identifying field contains a identifiable thing.
	 */
	@Test
	public void identityWithAThing() {
		HasIdentityWithThing t1;
		HasIdentityWithThing t2;
		
		// Exact same
		t1 = new HasIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("1").state("a").build())
				.state("s")
				.build();
		t2 = new HasIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("1").state("a").build())
				.state("s")
				.build();
		Assert.assertEquals(t1, t2);
		Assert.assertEquals(t1.idkey(), t2.idkey());
		
		// Different top level states, but same identity
		t1 = new HasIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("1").state("a").build())
				.state("s1")
				.build();
		t2 = new HasIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("1").state("a").build())
				.state("s2")
				.build();
		Assert.assertEquals(t1, t2);
		Assert.assertEquals(t1.idkey(), t2.idkey());
		
		// Different states in all things, but same identity
		t1 = new HasIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("1").state("a1").build())
				.state("s1")
				.build();
		t2 = new HasIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("1").state("a2").build())
				.state("s2")
				.build();
		Assert.assertEquals(t1, t2);
		Assert.assertEquals(t1.idkey(), t2.idkey());
		
		// Same states in all things, but different identities
		t1 = new HasIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("1").state("a").build())
				.state("s")
				.build();
		t2 = new HasIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("2").state("a").build())
				.state("s")
				.build();
		Assert.assertNotEquals(t1, t2);
		Assert.assertNotEquals(t1.idkey(), t2.idkey());
	}
	
	/**
	 * Test identity equality when an identifying field contains a non-identifiable thing.
	 */
	@Test
	public void identityWithNonIdThing() {
		HasIdentityWithNonIdThing t1;
		HasIdentityWithNonIdThing t2;
		
		// Exact same
		t1 = new HasIdentityWithNonIdThing.Builder()
				.id(new SomethingWithoutIdentity.Builder().value("a").obj(new SomethingElseWithoutIdentity.Builder().value("b").build()).build())
				.state("s")
				.build();
		t2 = new HasIdentityWithNonIdThing.Builder()
				.id(new SomethingWithoutIdentity.Builder().value("a").obj(new SomethingElseWithoutIdentity.Builder().value("b").build()).build())
				.state("s")
				.build();
		Assert.assertEquals(t1, t2);
		Assert.assertEquals(t1.idkey(), t2.idkey());
		
		// Different top level states, but same identity
		t1 = new HasIdentityWithNonIdThing.Builder()
				.id(new SomethingWithoutIdentity.Builder().value("a").obj(new SomethingElseWithoutIdentity.Builder().value("b").build()).build())
				.state("s1")
				.build();
		t2 = new HasIdentityWithNonIdThing.Builder()
				.id(new SomethingWithoutIdentity.Builder().value("a").obj(new SomethingElseWithoutIdentity.Builder().value("b").build()).build())
				.state("s2")
				.build();
		Assert.assertEquals(t1, t2);
		Assert.assertEquals(t1.idkey(), t2.idkey());
		
		// Same states in all things, but different identities
		t1 = new HasIdentityWithNonIdThing.Builder()
				.id(new SomethingWithoutIdentity.Builder().value("a1").obj(new SomethingElseWithoutIdentity.Builder().value("b").build()).build())
				.state("s")
				.build();
		t2 = new HasIdentityWithNonIdThing.Builder()
				.id(new SomethingWithoutIdentity.Builder().value("a2").obj(new SomethingElseWithoutIdentity.Builder().value("b").build()).build())
				.state("s")
				.build();
		Assert.assertNotEquals(t1, t2);
		Assert.assertNotEquals(t1.idkey(), t2.idkey());
		
		// Same states in all things, but different identities - second variant
		t1 = new HasIdentityWithNonIdThing.Builder()
				.id(new SomethingWithoutIdentity.Builder().value("a1").obj(new SomethingElseWithoutIdentity.Builder().value("b1").build()).build())
				.state("s")
				.build();
		t2 = new HasIdentityWithNonIdThing.Builder()
				.id(new SomethingWithoutIdentity.Builder().value("a2").obj(new SomethingElseWithoutIdentity.Builder().value("b2").build()).build())
				.state("s")
				.build();
		Assert.assertNotEquals(t1, t2);
		Assert.assertNotEquals(t1.idkey(), t2.idkey());
	}
	
	/**
	 * Test identity equality when an identifying field contains a identifiable thing and is part of many identifying fields.
	 */
	@Test
	public void complexIdentityWithAThing() {
		HasComplexIdentityWithThing t1;
		HasComplexIdentityWithThing t2;
		
		// Exact same
		t1 = new HasComplexIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("1").state("a").build())
				.id2("i")
				.state("s")
				.build();
		t2 = new HasComplexIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("1").state("a").build())
				.id2("i")
				.state("s")
				.build();
		Assert.assertEquals(t1, t2);
		Assert.assertEquals(t1.idkey(), t2.idkey());
		
		// Different top level states, but same identity
		t1 = new HasComplexIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("1").state("a").build())
				.id2("i")
				.state("s1")
				.build();
		t2 = new HasComplexIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("1").state("a").build())
				.id2("i")
				.state("s2")
				.build();
		Assert.assertEquals(t1, t2);
		Assert.assertEquals(t1.idkey(), t2.idkey());
		
		// Different states in all things, but same identity
		t1 = new HasComplexIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("1").state("a1").build())
				.id2("i")
				.state("s1")
				.build();
		t2 = new HasComplexIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("1").state("a2").build())
				.id2("i")
				.state("s2")
				.build();
		Assert.assertEquals(t1, t2);
		Assert.assertEquals(t1.idkey(), t2.idkey());
		
		// Same states in all things, but different identities in nested thing
		t1 = new HasComplexIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("1").state("a").build())
				.id2("i")
				.state("s")
				.build();
		t2 = new HasComplexIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("2").state("a").build())
				.id2("i")
				.state("s")
				.build();
		Assert.assertNotEquals(t1, t2);
		Assert.assertNotEquals(t1.idkey(), t2.idkey());
		
		// Same states in all things, but different identities in root field
		t1 = new HasComplexIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("1").state("a").build())
				.id2("i1")
				.state("s")
				.build();
		t2 = new HasComplexIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("1").state("a").build())
				.id2("i2")
				.state("s")
				.build();
		Assert.assertNotEquals(t1, t2);
		Assert.assertNotEquals(t1.idkey(), t2.idkey());
		
		// Same states in all things, but different identities both places
		t1 = new HasComplexIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("1").state("a").build())
				.id2("i1")
				.state("s")
				.build();
		t2 = new HasComplexIdentityWithThing.Builder()
				.id(new SomethingWithIdentity.Builder().id("2").state("a").build())
				.id2("i2")
				.state("s")
				.build();
		Assert.assertNotEquals(t1, t2);
		Assert.assertNotEquals(t1.idkey(), t2.idkey());
	}
	
	/**
	 * Test identity equality when an identifying field contains a non-identifiable thing and is part of many identifying fields.
	 */
	@Test
	public void complexIdentityWithNonIdThing() {
		HasComplexIdentityWithNonIdThing t1;
		HasComplexIdentityWithNonIdThing t2;
		
		// Exact same
		t1 = new HasComplexIdentityWithNonIdThing.Builder()
				.id(new SomethingWithoutIdentity.Builder().value("a").obj(new SomethingElseWithoutIdentity.Builder().value("b").build()).build())
				.id2("i")
				.state("s")
				.build();
		t2 = new HasComplexIdentityWithNonIdThing.Builder()
				.id(new SomethingWithoutIdentity.Builder().value("a").obj(new SomethingElseWithoutIdentity.Builder().value("b").build()).build())
				.id2("i")
				.state("s")
				.build();
		Assert.assertEquals(t1, t2);
		Assert.assertEquals(t1.idkey(), t2.idkey());
		
		// Different top level states, but same identity
		t1 = new HasComplexIdentityWithNonIdThing.Builder()
				.id(new SomethingWithoutIdentity.Builder().value("a").obj(new SomethingElseWithoutIdentity.Builder().value("b").build()).build())
				.id2("i")
				.state("s1")
				.build();
		t2 = new HasComplexIdentityWithNonIdThing.Builder()
				.id(new SomethingWithoutIdentity.Builder().value("a").obj(new SomethingElseWithoutIdentity.Builder().value("b").build()).build())
				.id2("i")
				.state("s2")
				.build();
		Assert.assertEquals(t1, t2);
		Assert.assertEquals(t1.idkey(), t2.idkey());
		
		// Same states in all things, but different identities in nested thing
		t1 = new HasComplexIdentityWithNonIdThing.Builder()
				.id(new SomethingWithoutIdentity.Builder().value("a").obj(new SomethingElseWithoutIdentity.Builder().value("b1").build()).build())
				.id2("i")
				.state("s")
				.build();
		t2 = new HasComplexIdentityWithNonIdThing.Builder()
				.id(new SomethingWithoutIdentity.Builder().value("a").obj(new SomethingElseWithoutIdentity.Builder().value("b2").build()).build())
				.id2("i")
				.state("s")
				.build();
		Assert.assertNotEquals(t1, t2);
		Assert.assertNotEquals(t1.idkey(), t2.idkey());
		
		// Same states in all things, but different identities in root field
		t1 = new HasComplexIdentityWithNonIdThing.Builder()
				.id(new SomethingWithoutIdentity.Builder().value("a").obj(new SomethingElseWithoutIdentity.Builder().value("b").build()).build())
				.id2("i1")
				.state("s")
				.build();
		t2 = new HasComplexIdentityWithNonIdThing.Builder()
				.id(new SomethingWithoutIdentity.Builder().value("a").obj(new SomethingElseWithoutIdentity.Builder().value("b").build()).build())
				.id2("i2")
				.state("s")
				.build();
		Assert.assertNotEquals(t1, t2);
		Assert.assertNotEquals(t1.idkey(), t2.idkey());
		
		// Same states in all things, but different identities both places
		t1 = new HasComplexIdentityWithNonIdThing.Builder()
				.id(new SomethingWithoutIdentity.Builder().value("a1").obj(new SomethingElseWithoutIdentity.Builder().value("b1").build()).build())
				.id2("i1")
				.state("s")
				.build();
		t2 = new HasComplexIdentityWithNonIdThing.Builder()
				.id(new SomethingWithoutIdentity.Builder().value("a2").obj(new SomethingElseWithoutIdentity.Builder().value("b2").build()).build())
				.id2("i2")
				.state("s")
				.build();
		Assert.assertNotEquals(t1, t2);
		Assert.assertNotEquals(t1.idkey(), t2.idkey());
	}
	
	/**
	 * Test identity equality when an identifying field contains a collection of identifiable things.
	 */
	@Test
	public void nestedIdentityInCollection() {
		HasNestedIdentity t1;
		HasNestedIdentity t2;
		
		// Exact same
		t1 = new HasNestedIdentity.Builder()
				.id(Collections.singletonList(new SomethingWithIdentity.Builder().id("1").state("a").build()))
				.state("s")
				.build();
		t2 = new HasNestedIdentity.Builder()
				.id(Collections.singletonList(new SomethingWithIdentity.Builder().id("1").state("a").build()))
				.state("s")
				.build();
		Assert.assertEquals(t1, t2);
		Assert.assertEquals(t1.idkey(), t2.idkey());
		
		// Different top level states, but same identity
		t1 = new HasNestedIdentity.Builder()
				.id(Collections.singletonList(new SomethingWithIdentity.Builder().id("1").state("a").build()))
				.state("s1")
				.build();
		t2 = new HasNestedIdentity.Builder()
				.id(Collections.singletonList(new SomethingWithIdentity.Builder().id("1").state("a").build()))
				.state("s2")
				.build();
		Assert.assertEquals(t1, t2);
		Assert.assertEquals(t1.idkey(), t2.idkey());
		
		// Different states in all things, but same identity
		t1 = new HasNestedIdentity.Builder()
				.id(Collections.singletonList(new SomethingWithIdentity.Builder().id("1").state("a1").build()))
				.state("s1")
				.build();
		t2 = new HasNestedIdentity.Builder()
				.id(Collections.singletonList(new SomethingWithIdentity.Builder().id("1").state("a2").build()))
				.state("s2")
				.build();
		Assert.assertEquals(t1, t2);
		Assert.assertEquals(t1.idkey(), t2.idkey());
		
		// Same states in all things, but different identities
		t1 = new HasNestedIdentity.Builder()
				.id(Collections.singletonList(new SomethingWithIdentity.Builder().id("1").state("a").build()))
				.state("s")
				.build();
		t2 = new HasNestedIdentity.Builder()
				.id(Collections.singletonList(new SomethingWithIdentity.Builder().id("2").state("a").build()))
				.state("s")
				.build();
		Assert.assertNotEquals(t1, t2);
		Assert.assertNotEquals(t1.idkey(), t2.idkey());
	}
	
	/**
	 * Test identity equality when an identifying field contains a non-identifiable thing that contains an identifiable thing.
	 */
	@Test
	public void nestedIdentityInObject() {
		HasNestedIdentity t1;
		HasNestedIdentity t2;
		
		// Exact same
		t1 = new HasNestedIdentity.Builder()
				.nested(new NestedIdentity.Builder().nested(new SomethingWithIdentity.Builder().id("1").state("a").build()).build())
				.state("s")
				.build();
		t2 = new HasNestedIdentity.Builder()
				.nested(new NestedIdentity.Builder().nested(new SomethingWithIdentity.Builder().id("1").state("a").build()).build())
				.state("s")
				.build();
		Assert.assertEquals(t1, t2);
		Assert.assertEquals(t1.idkey(), t2.idkey());
		
		// Different top level states, but same identity
		t1 = new HasNestedIdentity.Builder()
				.nested(new NestedIdentity.Builder().nested(new SomethingWithIdentity.Builder().id("1").state("a").build()).build())
				.state("s1")
				.build();
		t2 = new HasNestedIdentity.Builder()
				.nested(new NestedIdentity.Builder().nested(new SomethingWithIdentity.Builder().id("1").state("a").build()).build())
				.state("s2")
				.build();
		Assert.assertEquals(t1, t2);
		Assert.assertEquals(t1.idkey(), t2.idkey());
		
		// Different states in all things, but same identity
		t1 = new HasNestedIdentity.Builder()
				.nested(new NestedIdentity.Builder().nested(new SomethingWithIdentity.Builder().id("1").state("a1").build()).build())
				.state("s1")
				.build();
		t2 = new HasNestedIdentity.Builder()
				.nested(new NestedIdentity.Builder().nested(new SomethingWithIdentity.Builder().id("1").state("a2").build()).build())
				.state("s2")
				.build();
		Assert.assertEquals(t1, t2);
		Assert.assertEquals(t1.idkey(), t2.idkey());
		
		// Same states in all things, but different identities
		t1 = new HasNestedIdentity.Builder()
				.nested(new NestedIdentity.Builder().nested(new SomethingWithIdentity.Builder().id("1").state("a").build()).build())
				.state("s")
				.build();
		t2 = new HasNestedIdentity.Builder()
				.nested(new NestedIdentity.Builder().nested(new SomethingWithIdentity.Builder().id("2").state("a").build()).build())
				.state("s")
				.build();
		Assert.assertNotEquals(t1, t2);
		Assert.assertNotEquals(t1.idkey(), t2.idkey());
	}
	
	@Test
	public void action() {
		Action a1;
		Action a2;
		
		// Same
		a1 = new EqualityAction.Builder()
				.val1("yo")
				.val2(1)
				.array(Arrays.asList("hi", "hey", "yo"))
				.id_thing(new SomethingWithIdentity.Builder().state("s").id("i").build())
				.non_id_thing(new SomethingWithoutIdentity.Builder().value("v").obj(new SomethingElseWithoutIdentity.Builder().value("v").build()).build())
				.id_thing_array(Arrays.asList(new SomethingWithIdentity.Builder().state("s2").id("i2").build(), new SomethingWithIdentity.Builder().state("s3").id("i3").build()))
				.build();
		a2 = new EqualityAction.Builder()
				.val1("yo")
				.val2(1)
				.array(Arrays.asList("hi", "hey", "yo"))
				.id_thing(new SomethingWithIdentity.Builder().state("s").id("i").build())
				.non_id_thing(new SomethingWithoutIdentity.Builder().value("v").obj(new SomethingElseWithoutIdentity.Builder().value("v").build()).build())
				.id_thing_array(Arrays.asList(new SomethingWithIdentity.Builder().state("s2").id("i2").build(), new SomethingWithIdentity.Builder().state("s3").id("i3").build()))
				.build();
		Assert.assertEquals(a1, a2);
		
		// Same but the identifying object has different state. Since actions compare state, this should be unequal
		a1 = new EqualityAction.Builder()
				.val1("yo")
				.val2(1)
				.array(Arrays.asList("hi", "hey", "yo"))
				.id_thing(new SomethingWithIdentity.Builder().state("s1").id("i").build())
				.non_id_thing(new SomethingWithoutIdentity.Builder().value("v").obj(new SomethingElseWithoutIdentity.Builder().value("v").build()).build())
				.id_thing_array(Arrays.asList(new SomethingWithIdentity.Builder().state("s2").id("i2").build(), new SomethingWithIdentity.Builder().state("s3").id("i3").build()))
				.build();
		a2 = new EqualityAction.Builder()
				.val1("yo")
				.val2(1)
				.array(Arrays.asList("hi", "hey", "yo"))
				.id_thing(new SomethingWithIdentity.Builder().state("s2").id("i").build())
				.non_id_thing(new SomethingWithoutIdentity.Builder().value("v").obj(new SomethingElseWithoutIdentity.Builder().value("v").build()).build())
				.id_thing_array(Arrays.asList(new SomethingWithIdentity.Builder().state("s2").id("i2").build(), new SomethingWithIdentity.Builder().state("s3").id("i3").build()))
				.build();
		Assert.assertNotEquals(a1, a2);
		
		// Same but some in the second are undeclared
		a1 = new EqualityAction.Builder()
				.val1("yo")
				.val2(1)
				.array(Arrays.asList("hi", "hey", "yo"))
				.id_thing(new SomethingWithIdentity.Builder().state("s").id("i").build())
				.non_id_thing(new SomethingWithoutIdentity.Builder().value("v").obj(new SomethingElseWithoutIdentity.Builder().value("v").build()).build())
				.id_thing_array(Arrays.asList(new SomethingWithIdentity.Builder().state("s2").id("i2").build(), new SomethingWithIdentity.Builder().state("s3").id("i3").build()))
				.build();
		a2 = new EqualityAction.Builder()
				.val1("yo")
				.array(Arrays.asList("hi", "hey", "yo"))
				.id_thing(new SomethingWithIdentity.Builder().state("s").id("i").build())
				.build();
		Assert.assertNotEquals(a1, a2);
		
		// Very different
		a1 = new EqualityAction.Builder()
				.val2(1)
				.non_id_thing(new SomethingWithoutIdentity.Builder().value("v").obj(new SomethingElseWithoutIdentity.Builder().value("v").build()).build())
				.build();
		a2 = new EqualityAction.Builder()
				.val1("abcdef")
				.build();
		Assert.assertNotEquals(a1, a2);
	}
	
	
	
}