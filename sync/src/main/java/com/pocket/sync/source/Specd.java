package com.pocket.sync.source;

import com.pocket.sync.spec.Spec;

/**
 * Something, likely a {@link Source}, that provides its {@link Spec}, a description of what it can do.
 */
public interface Specd {
	/**
	 * @return Describes what this {@link Source} can do.
	 */
	Spec spec();
}
