package com.pocket.sync;

import com.pocket.sdk.api.generated.action.Favorite;
import com.pocket.sdk.api.generated.action.Unfavorite;
import com.pocket.sdk.api.generated.thing.LocalItems;
import com.pocket.sdk.api.spec.PocketSpec;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sync.action.Action;
import com.pocket.sync.source.result.RemotePriority;
import com.pocket.sync.source.threads.JavaThreadPools;
import com.pocket.sync.space.Holder;
import com.pocket.sync.space.mutable.MutableSpace;
import com.pocket.sync.space.persist.DumbStorage;
import com.pocket.sync.space.persist.SqliteBinaryStorage;
import com.pocket.sync.spec.Spec;
import com.pocket.sync.test.generated.Modeller;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.protect.OnlyForTestingEncrypter;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class MutableSpaceStorageTest {
	
	@Test
	public void store_and_restore() throws Exception {
		DumbStorage storage;
		Spec spec = new PocketSpec();
		storage = new SqliteBinaryStorage(RuntimeEnvironment.application, "pkt", new JavaThreadPools(), Modeller.OBJECT_MAPPER, new OnlyForTestingEncrypter());
		MutableSpace space = new MutableSpace(storage).setSpec(spec);
		
		Thing thing1 = ThingMock.thing().feed();
		Thing thing2 = ThingMock.thing().getNotifications();
		Thing match1 = new LocalItems.Builder().build();
		
		Holder holder1 = Holder.persistent("h1");
		Holder holder2 = Holder.persistent("h2");
		
		Action action1 = new Favorite.Builder().item_id("123456").time(Timestamp.now()).build();
		Action action2 = new Unfavorite.Builder().item_id("654321").time(Timestamp.now()).build();
		
		space.remember(holder1, thing1);
		space.remember(holder2, thing2);
		space.remember(holder2, match1);
		space.imprint(thing1);
		space.imprint(thing2);
		space.addAction(action1, RemotePriority.SOON);
		space.addAction(action2, RemotePriority.WHENEVER);
		space.addInvalid(thing1);
		
		thing1 = space.get(thing1);
		thing2 = space.get(thing2);
		
		// Make sure it finishes writing
		space.await(1, TimeUnit.MINUTES);
		
		// Reload
		storage = new SqliteBinaryStorage(RuntimeEnvironment.application, "pkt", new JavaThreadPools(), Modeller.OBJECT_MAPPER, new OnlyForTestingEncrypter());
		space = new MutableSpace(storage).setSpec(new PocketSpec());
		
		Assert.assertTrue(space.get(thing1).equals(Thing.Equality.STATE, thing1));
		Assert.assertTrue(space.get(thing2).equals(Thing.Equality.STATE, thing2));
		
		Assert.assertEquals(RemotePriority.SOON, space.getActions().get(action1));
		Assert.assertEquals(RemotePriority.WHENEVER, space.getActions().get(action2));
		
		Assert.assertTrue(thing1.equals(Thing.Equality.STATE, space.getInvalid().toArray()[0]));
	}
	
}