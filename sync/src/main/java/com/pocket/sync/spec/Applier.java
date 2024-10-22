package com.pocket.sync.spec;

import com.pocket.sync.action.Action;
import com.pocket.sync.space.Space;

/**
 * Something that can apply the effect of an {@link Action} to a {@link Space}.
 */
public interface Applier {
	void apply(Action action, Space space);
}
