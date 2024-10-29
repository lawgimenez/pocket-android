package com.pocket.sync.space;

import com.pocket.sync.thing.Thing;

public class Change<T extends Thing> {
	public final T previous;
	public final T latest;
	public Change(T previous, T latest) {
		this.previous = previous;
		this.latest = latest;
	}
}
