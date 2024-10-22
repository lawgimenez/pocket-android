package com.pocket.sync.space;

import android.content.Context;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.pocket.sync.spec.Spec;
import com.pocket.sync.thing.Thing;
import com.pocket.util.java.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TODO Documentation
 */
public class SpaceBenchmarks {
	
	public interface SpaceProvider {
		Space newInstance(Context context, String name);
	}
	
	public static class Variant {
		public final Spec spec;
		public final SpaceProvider mem;
		public Variant(Spec spec, SpaceProvider mem) {
			this.spec = spec;
			this.mem = mem;
		}
	}
	
	private static void log(String log) {
		System.out.println("SpaceBenchmarks " + log);
	}

	public static void compareImplementations(int iterations, List<Thing> things, Context context, Variant... v) throws Exception {
		log("======= ROUND ONE ========");
		compare(iterations, things, context, v);
		
		log("======= ROUND TWO ========");
		compare(iterations, things, context, v);
	}

	private static void compare(int iterations, List<Thing> things, Context context, Variant... variants) throws Exception {
		ListMultimap<Integer, String> winners = MultimapBuilder.hashKeys().arrayListValues().build();
		run("getCold", winners, variants, (Variant v, Holder h) -> getCold(iterations, things, context, v, h));
		run("getWarm", winners, variants, (Variant v, Holder h) -> getWarm(iterations, things, context, v, h));
		run("getHot", winners, variants, (Variant v, Holder h) -> getHot(iterations, things, context, v, h));
		run("imprintNew", winners, variants, (Variant v, Holder h) -> imprintNew(iterations, things, context, v, h));
		run("imprintSame", winners, variants, (Variant v, Holder h) -> imprintSame(iterations, things, context, v, h));
		run("imprintSameDerived", winners, variants, (Variant v, Holder h) -> imprintSameDerived(iterations, things, context, v, h));
		
		log("~ WINNERS ~");
		for (int i = 0; i < variants.length; i++) {
			log("MEM "+ i+":");
			for (String s : winners.get(i)) {
				log(s);
			}
			if (winners.get(i).isEmpty()) log("None");
			log("");
		}
		
	}
	
	private static void run(String name, ListMultimap<Integer, String> winners, Variant[] v, Test test) throws Exception {
		for (Holder holder : Arrays.asList(Holder.session("session"), Holder.persistent("persist"))) {
			StopWatch[] results = new StopWatch[v.length];
			long slowestAvg = 0;
			int slowest = 0;
			long fatestAvg = Long.MAX_VALUE;
			int fastest = 0;
			for (int i = 0; i < v.length; i++) {
				StopWatch result = test.run(v[i], holder);
				results[i] = result;
				if (result.avgNanos() > slowestAvg) {
					slowest = i;
					slowestAvg = result.avgNanos();
				} else if (result.avgNanos() < fatestAvg) {
					fastest = i;
					fatestAvg = result.avgNanos();
				}
				
			}
			String testname = name + " " + holder.key();
			for (int i = 0; i < v.length; i++) {
				StopWatch result = results[i];
				String log = testname + " MEM " + i + " : " + result.prettyPrint();
				String summary = String.format("%.2f", slowestAvg/(double)result.avgNanos()) + "x faster avg by " + StopWatch.formatted(slowestAvg-result.avgNanos(), 1, 3) + " ms";
				if (i != slowest) {
					log += " | " + summary;
				}
				log(log);
				if (i == fastest) {
					winners.put(i, testname + " " + summary);
				}
			}
		}
	}
	
	interface Test {
		StopWatch run(Variant variables, Holder holder) throws Exception;
	}
	
	private static StopWatch imprintNew(int iterations, List<Thing> things, Context context, Variant v, Holder holder) throws Exception {
		Space space = v.mem.newInstance(context, "bench" + System.currentTimeMillis()).setSpec(v.spec);
		StopWatch watch = new StopWatch();
		
		System.gc();
		Thread.sleep(200);
		
		for (int i = 0; i < iterations; i++) {
			space.clear();
			for (Thing thing : things) {
				space.remember(holder, thing);
			}
			for (Thing thing : things) {
				watch.resume();
				space.imprint(thing);
				watch.pause();
			}
		}
		
		space.release();
		return watch;
	}
	
	private static StopWatch imprintSame(int iterations, List<Thing> things, Context context, Variant v, Holder holder) throws Exception {
		Space space = v.mem.newInstance(context, "bench" + System.currentTimeMillis()).setSpec(v.spec);
		StopWatch watch = new StopWatch();
		
		System.gc();
		Thread.sleep(200);
		
		for (int i = 0; i < iterations; i++) {
			space.clear();
			for (Thing thing : things) {
				space.remember(holder, thing);
				space.imprint(thing);
			}
			for (Thing thing : things) {
				watch.resume();
				space.imprint(thing);
				watch.pause();
			}
		}
		
		space.release();
		return watch;
	}
	
	private static StopWatch imprintSameDerived(int iterations, List<Thing> things, Context context, Variant v, Holder holder) throws Exception {
		Space space = v.mem.newInstance(context, "bench" + System.currentTimeMillis()).setSpec(v.spec);
		StopWatch watch = new StopWatch();
		
		System.gc();
		Thread.sleep(200);
		
		for (int i = 0; i < iterations; i++) {
			space.clear();
			List<Thing> imprinted = new ArrayList<>();
			for (Thing thing : things) {
				space.remember(holder, thing);
				space.imprint(thing);
				imprinted.add(space.get(thing));
			}
			for (Thing thing : imprinted) {
				watch.resume();
				space.imprint(thing);
				watch.pause();
			}
		}
		
		space.release();
		return watch;
	}
	
	private static StopWatch getCold(int iterations, List<Thing> things, Context context, Variant v, Holder holder) throws Exception {
		String name = "bench" + System.currentTimeMillis();
		StopWatch watch = new StopWatch();
		
		System.gc();
		Thread.sleep(200);
		
		Space space = v.mem.newInstance(context, name).setSpec(v.spec);
		space.clear();
		for (Thing thing : things) {
			space.remember(holder, thing);
			space.imprint(thing);
		}
		space.release();
		
		for (int i = 0; i < iterations; i++) {
			space = v.mem.newInstance(context, name).setSpec(v.spec);
			for (Thing thing : things) {
				watch.resume();
				Thing t = space.get(thing);
				if (t == null) throw new RuntimeException("not restored!");
				watch.pause();
			}
			space.release();
		}
		return watch;
	}
	
	private static StopWatch getWarm(int iterations, List<Thing> things, Context context, Variant v, Holder holder) throws Exception {
		Space space = v.mem.newInstance(context, "bench" + System.currentTimeMillis()).setSpec(v.spec);
		StopWatch watch = new StopWatch();
		
		System.gc();
		Thread.sleep(200);
		
		space.clear();
		for (Thing thing : things) {
			space.remember(holder, thing);
			space.imprint(thing);
		}
		
		for (int i = 0; i < iterations; i++) {
			for (Thing thing : things) {
				watch.resume();
				space.get(thing);
				watch.pause();
			}
		}
		
		space.release();
		return watch;
	}
	
	private static StopWatch getHot(int iterations, List<Thing> things, Context context, Variant v, Holder holder) throws Exception {
		Space space = v.mem.newInstance(context, "bench" + System.currentTimeMillis()).setSpec(v.spec);
		StopWatch watch = new StopWatch();
		
		System.gc();
		Thread.sleep(200);
		
		space.clear();
		for (Thing thing : things) {
			space.remember(holder, thing);
			space.imprint(thing);
		}
		
		for (int i = 0; i < iterations; i++) {
			for (Thing thing : things) {
				watch.resume();
				space.get(thing);
				watch.pause();
			}
		}
		
		space.release();
		return watch;
	}
	
	// TODO add a worst case scenario where it imprints same, but with a ton of changes, including lists
	
}
