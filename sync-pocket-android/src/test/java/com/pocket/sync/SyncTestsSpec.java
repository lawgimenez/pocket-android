package com.pocket.sync;


import com.pocket.sync.action.Action;
import com.pocket.sync.space.Diff;
import com.pocket.sync.space.Space;
import com.pocket.sync.test.generated.SyncTestsApplier;
import com.pocket.sync.test.generated.SyncTestsBaseSpec;
import com.pocket.sync.test.generated.SyncTestsDerives;
import com.pocket.sync.test.generated.thing.ReactiveImplementation;
import com.pocket.sync.test.generated.thing.ReactiveThing;
import com.pocket.sync.thing.Thing;

public class SyncTestsSpec extends SyncTestsBaseSpec {
	
	public SyncTestsSpec() {
		super(new Deriver(), new SyncTestsApplier() {
			@Override
			protected void unknown(Action action, Space space) {
			
			}
		});
	}
	
	private static class Deriver extends SyncTestsDerives {
		
		@Override
		public String derive__ReactiveThing__reactive_collection_field(ReactiveThing t, Diff diff, Space.Selector selector) {
			return now();
		}
		
		@Override
		public String derive__ReactiveThing__reactive_field(ReactiveThing t, Diff diff, Space.Selector selector) {
			return now();
		}
		
		@Override
		public String derive__ReactiveThing__reactive_self(ReactiveThing t, Diff diff, Space.Selector selector) {
			return now();
		}
		
		@Override
		public String derive__ReactiveThing__reactive_type(ReactiveThing t, Diff diff, Space.Selector selector) {
			return now();
		}
		
		@Override
		public String derive__ReactiveThing__reactive_type_field(ReactiveThing t, Diff diff, Space.Selector selector) {
			return now();
		}

		@Override
		public String derive__ReactiveImplementation__reactive_implementation(ReactiveImplementation t, Diff diff, Space.Selector selector) {
			return now();
		}

		@Override
		public String derive__ReactiveImplementation__reactive_implementation_field(ReactiveImplementation t, Diff diff, Space.Selector selector) {
			return now();
		}

		@Override
		public String derive__ReactiveImplementation__reactive_interface(ReactiveImplementation t, Diff diff, Space.Selector selector) {
			return now();
		}

		@Override
		public String derive__ReactiveImplementation__reactive_interface_field(ReactiveImplementation t, Diff diff, Space.Selector selector) {
			return now();
		}

		@Override
		public String derive__ReactiveImplementation__reactive_self_field(ReactiveImplementation t, Diff diff, Space.Selector selector) {
			return now();
		}

		@Override
		public String derive__ReactiveImplementation__reactive_self(ReactiveImplementation t, Diff diff, Space.Selector selector) {
			return now();
		}

		private String now() {
			return System.nanoTime()+"";
		}
		
		@Override
		public <T extends Thing> T derive(T thing, Space.Selector space) {
			return null;
		}
	}
	
}
