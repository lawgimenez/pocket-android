package com.pocket.sync.source.subscribe;

import com.pocket.sync.thing.Thing;
import com.pocket.sync.source.Source;

/**
 * Someone that will be notified each time there is a new version of a specific {@link Thing} know to a {@link Source}.
 */
public interface Subscriber<T extends Thing> {
	/** A {@link Thing} has changed. Here is the latest. */
	void onUpdate(T thing);
}
