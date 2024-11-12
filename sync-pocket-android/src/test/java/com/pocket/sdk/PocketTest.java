package com.pocket.sdk;

import com.pocket.sdk.api.generated.enums.CxtEvent;
import com.pocket.sdk.api.generated.enums.ItemStatus;
import com.pocket.sdk.api.generated.enums.SuggestionsType;
import com.pocket.sdk.api.generated.thing.Feed;
import com.pocket.sdk.api.generated.thing.GetSuggestedFollows;
import com.pocket.sdk.api.generated.thing.GetUnleashAssignments;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.generated.thing.UnleashContext;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sync.action.Action;
import com.pocket.sync.source.subscribe.Changes;
import com.pocket.sync.source.subscribe.Subscription;
import com.pocket.sync.space.Holder;
import com.pocket.sync.thing.Thing;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * TODO Documentation
 */
public class PocketTest extends AbsPocketTest {

	@Ignore("The test went SPLOINK when security made us delete our old test accounts. We could switch to another account, but really, the test shouldn't hit the live production API.")
	@Test
	public void unleash() throws Exception {
		PocketInstance instance = newPocket();
		Assert.assertNotNull(instance.pocket.sync(new GetUnleashAssignments.Builder().context(new UnleashContext.Builder().sessionId("test").build()).build()).get());
	}
	
	@Ignore("The test went SPLOINK when security made us delete our old test accounts. We could switch to another account, but really, the test shouldn't hit the live production API.")
	@Test
	public void gsf() throws Exception {
		PocketInstance instance = newPocket();
		Assert.assertNotNull(instance.pocket.sync(new GetSuggestedFollows.Builder().version("2").social_service(SuggestionsType.POCKET).build()).get());
	}

	@Ignore("The test went SPLOINK when security made us delete our old test accounts. We could switch to another account, but really, the test shouldn't hit the live production API.")
	@Test
	public void automaticallyObtainsGuidFromSyncingThing() throws Exception {
		Pocket pocket = newPocket().pocket;

		// Sync something, which should obtain the guid first
		pocket.sync(pocket.spec().things().feed().version("4").build()).get();

		Assert.assertNotNull("guid not automatically obtained", pocket.sync(pocket.spec().things().loginInfo().build()).get().guid);
	}

	@Ignore("The test went SPLOINK when security made us delete our old test accounts. We could switch to another account, but really, the test shouldn't hit the live production API.")
	@Test
	public void automaticallyObtainsGuidFromSyncingAction() throws Exception {
		PocketInstance instance = newPocket();
		Pocket pocket = instance.pocket;

		// Sync an action, which should obtain the guid first
		Action action = pocket.spec().actions().pv()
				.event(CxtEvent.OPEN)
				.time(Timestamp.now())
				.build();
		pocket.syncRemote(null, action).get();
		
		Assert.assertNotNull("guid not automatically obtained", pocket.sync(pocket.spec().things().loginInfo().build()).get().guid);

		// Now also validate a thing can be synced afterwards
		instance.setNetworkEnabled(true);
		pocket.sync(pocket.spec().things().feed().version("4").build()).get();

		instance.setNetworkEnabled(false); // Block network to ensure the get is from the cache
		Assert.assertNotNull("guid not automatically obtained", pocket.sync(pocket.spec().things().loginInfo().build()).get().guid);
	}

	@Ignore("The test went SPLOINK when security made us delete our old test accounts. We could switch to another account, but really, the test shouldn't hit the live production API.")
	@Test
	public void persists() throws Exception {
		PocketInstance instance = newPocket();
		Thing thing = new Feed.Builder().count(30).version("4").build();
		instance.pocket.remember(Holder.persistent("holder"), thing);
		thing = instance.pocket.sync(thing).get();
		instance.setNetworkEnabled(false);
		Thing cached = instance.pocket.sync(thing).get();
		Assert.assertTrue(thing.equals(Thing.Equality.STATE, cached));
	}
	
	@Ignore("The test went SPLOINK when security made us delete our old test accounts. We could switch to another account, but really, the test shouldn't hit the live production API.")
	@Test
	public void save() throws Exception {
		PocketInstance instance = newPocket();
		Pocket pocket = instance.pocket;
		
		Feed recs = new Feed.IdBuilder()
				.version("4")
				.count(1)
				.build();
		
		pocket.remember(Holder.persistent("holder"), recs);
		recs = pocket.sync(recs).get();
		
		Item first = recs.feed.get(0).item;
		Assume.assumeTrue(first.status != ItemStatus.UNREAD); // if it is saved already, this test is not effective
		
		recs = pocket.sync(recs, pocket.spec().actions().add()
					.item(first)
					.time(Timestamp.now())
					.build()).get();
		
		Assert.assertEquals(recs.feed.get(0).item.status, ItemStatus.UNREAD);
	}
}