package com.pocket.sync;

import com.pocket.sync.spec.Syncable;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.Include;
import com.pocket.util.java.JsonUtil;

import org.junit.Assert;

import java.util.List;

/**
 * TODO Documentation
 */

public class SyncAsserts {
	
	public static void equalsState(Thing from, Thing to) {
		equalsState(from, to, true);
	}
	
	public static void equalsState(Thing from, Thing to, boolean fail) {
		if (from == null) {
			if (to != null) {
				Assert.fail("not equal, first is null, second is not");
			}
		} else if (!from.equals(Thing.Equality.STATE, to)) {
			System.out.println("Found changes: FROM -> TO");
			List<String> diff = JsonUtil.diff2(from.toJson(Syncable.NO_ALIASES, Include.DANGEROUS), to.toJson(Syncable.NO_ALIASES, Include.DANGEROUS), JsonUtil.EqualsFlag.ANY_NUMERICAL);
			for (String d : diff) {
				System.out.println(d);
			}
			if (fail) {
				Assert.fail("not equal");
			}
		}
	}
	
}
