package com.pocket.sync.space;

import com.pocket.sync.space.mutable.MutableSpace;
import com.pocket.sync.spec.Spec;

public class MutableSpaceTest extends SpaceTest {
	
	@Override
	protected MutableSpace instance(String name, Spec spec) {
		return new MutableSpace().setSpec(spec);
	}
	
}