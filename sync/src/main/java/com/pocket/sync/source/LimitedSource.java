package com.pocket.sync.source;

import com.pocket.sync.spec.Syncable;

/**
 * A source, such as a remote source, that only supports a limited subset of things and actions.
 * Could consider making this part of the {@link com.pocket.sync.spec.Spec} API some how?
 */
public interface LimitedSource {
    boolean isSupported(Syncable syncable);
}
