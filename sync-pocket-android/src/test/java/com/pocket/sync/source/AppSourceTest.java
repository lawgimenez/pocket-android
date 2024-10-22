package com.pocket.sync.source;

import com.pocket.sync.SyncTestsSpec;
import com.pocket.sync.action.Action;
import com.pocket.sync.source.result.Status;
import com.pocket.sync.source.result.SyncResult;
import com.pocket.sync.source.threads.JavaThreadPools;
import com.pocket.sync.source.threads.Publisher;
import com.pocket.sync.space.Holder;
import com.pocket.sync.space.mutable.MutableSpace;
import com.pocket.sync.spec.Resolver;
import com.pocket.sync.thing.Thing;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class AppSourceTest {
	
	@Test
	public void await() throws Throwable {
		// given
		Random random = new Random();
		SyncTestsSpec spec = new SyncTestsSpec();
		// This remote source will wait a random amount of time and then return a successful response of whatever it was given.
		FullResultSource remote = new FullResultSource() {
			@Override
			public <T extends Thing> SyncResult<T> syncFull(T thing, Action... actions) {
				try {
					Thread.sleep((long) (random.nextFloat() * 500));
				} catch (InterruptedException ignore) {}
				SyncResult.Builder<T> sr = new SyncResult.Builder<>(thing, actions);
				if (thing != null) sr.thing(thing);
				for (Action action : actions) {
					sr.action(action, Status.SUCCESS, null, null);
				}
				return sr.build();
			}
		};
		AppSource source = new AppSource(spec, new MutableSpace(null).setSpec(spec), remote, Resolver.BASIC, Publisher.CALLING_THREAD, new JavaThreadPools());
		Holder holder = Holder.persistent("holder");
		
		// Loop a few times to get more randomness and to verify repeated usage works
		for (int repeat = 0; repeat < 3; repeat++) {
			source.forget(holder);
			
			// when
			// Fire off a bunch of tasks that should trigger a backlog of local and remote work
			int fill = random.nextInt(100)+30;
			System.out.println("Trying with " + fill + " tasks");
			AtomicInteger count = new AtomicInteger(fill);
			for (int i = 0; i < fill; i++) {
				Thing t = spec.things().somethingWithIdentity().id(i+"").build();
				source.remember(holder, t);
				source.sync(t).onSuccess(s -> count.decrementAndGet());
			}
			
			// then
			// Await the work and verify all of the work was completed when this releases
			source.await().get();
			Assert.assertEquals(0, count.get());
		}
		
		// Finally await in a case where there should be no pending tasks, to make sure it doesn't infinitely hold in that case
		source.await().get();
	}
}