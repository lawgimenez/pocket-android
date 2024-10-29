package com.pocket.sdk.api.spec;

import com.pocket.sdk.api.generated.PocketBaseSpec;
import com.pocket.sync.spec.Spec;

/**
 * Pocket's {@link Spec}.
 */
public class PocketSpec extends PocketBaseSpec {
	
	public PocketSpec() {
		super(new Deriver(), new Applier());
	}

}
