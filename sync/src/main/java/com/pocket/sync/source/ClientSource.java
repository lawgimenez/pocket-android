package com.pocket.sync.source;

/**
 * A synchronous {@link Source} implementation with many of the features a client side application likely would have.
 * @see AsyncClientSource
 */
public interface ClientSource extends SynchronousRemoteBackedSource, Persisted, Subscribeable, Specd {}
