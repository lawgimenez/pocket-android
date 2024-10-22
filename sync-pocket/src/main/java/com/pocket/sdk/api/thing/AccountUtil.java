package com.pocket.sdk.api.thing;

import com.pocket.sdk.api.generated.thing.Getuser;
import com.pocket.sdk.api.spec.PocketSpec;

/**
 * Tools for working with {@link com.pocket.sdk.api.generated.thing.Account}
 */
public class AccountUtil {
	
	/** Convenience method for creating an empty instance of {@link Getuser} which can be a bit verbose because of the required hash parameter. */
	public static Getuser getuser(PocketSpec spec) {
		return spec.things().getuser().hash("9dJDjsla49la").build();
	}
	
}
