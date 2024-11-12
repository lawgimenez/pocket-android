package com.pocket.sync;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.pocket.sdk.api.generated.Modeller;
import com.pocket.sdk.api.spec.PocketSpec;
import com.pocket.sync.action.Action;
import com.pocket.sync.source.result.RemotePriority;
import com.pocket.sync.source.threads.JavaThreadPools;
import com.pocket.sync.space.Holder;
import com.pocket.sync.space.persist.DumbStorage;
import com.pocket.sync.space.persist.SqliteBinaryStorage;
import com.pocket.sync.thing.FlatUtils;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.thing.ThingUtil;
import com.pocket.sync.value.protect.OnlyForTestingEncrypter;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TODO Documentation
 */
@RunWith(RobolectricTestRunner.class)
public class StorageTest {
	
	@Test
	public void store_and_restore() throws Exception {
		DumbStorage storage = new SqliteBinaryStorage(RuntimeEnvironment.application, "yo", new JavaThreadPools(), Modeller.OBJECT_MAPPER, new OnlyForTestingEncrypter());
		
		AtomicReference<Throwable> failure = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(1);
		
		// Things
		List<Thing> things = new ArrayList<>(FlatUtils.flatten(Arrays.asList(
				ThingMock.thing().feed(),
				ThingMock.thing().getNotifications()
		)));
		
		// Holders
		Set<Pair<Holder, Object>> holders = new HashSet<>();
		holders.add(Pair.of(Holder.persistent("persistent"), "idkey1"));
		holders.add(Pair.of(Holder.persistent("persistent"), "idkey2"));
		holders.add(Pair.of(Holder.session("session"), "idkey3"));

		// Actions
		Map<Action, RemotePriority> actions = new HashMap<>();
		
		// Invalid
		Set<String> invalid = new HashSet<>();
		invalid.add("invalid_idkey");
		
		storage.store(things, null, holders, null, actions, null, invalid, null, latch::countDown, error -> {
			failure.set(error);
			latch.countDown();
		});
		
		latch.await();
		if (failure.get() != null) throw new RuntimeException(failure.get());
		
		List<Thing> restoredThings = new ArrayList<>();
		Multimap<Holder, Object> restoredHolders = MultimapBuilder.hashKeys().arrayListValues().build();
		Map<Action, RemotePriority> restoredActions = new HashMap<>();
		Set<String> restoredInvalid = new HashSet<>();
		storage.restore(new PocketSpec(), restoredThings::add, restoredHolders::putAll, restoredActions::putAll, restoredInvalid::addAll);
		
		// Sort both in some predictable way so we can compare them easier
		Collections.sort(things, (o1, o2) -> o1.idkey().compareTo(o2.idkey()));
		Collections.sort(restoredThings, (o1, o2) -> o1.idkey().compareTo(o2.idkey()));
		Assert.assertTrue(ThingUtil.listEquals(Thing.Equality.FLAT, new ArrayList<>(things), restoredThings));
		
		Assert.assertEquals(actions, restoredActions);
		Assert.assertEquals(invalid, restoredInvalid);
		
		Assert.assertEquals(3, restoredHolders.size());
		Assert.assertTrue(restoredHolders.get(Holder.persistent("persistent")).contains("idkey1"));
		Assert.assertTrue(restoredHolders.get(Holder.persistent("persistent")).contains("idkey2"));
		Assert.assertTrue(restoredHolders.get(Holder.persistent("session")).contains("idkey3"));
	}
	
}
