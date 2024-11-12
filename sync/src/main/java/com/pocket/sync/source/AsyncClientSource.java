package com.pocket.sync.source;

/**
 * An {@link AsyncSource} implementation with many of the features a client side application likely would have.
 * @see ClientSource
 */
public interface AsyncClientSource extends AsyncSource, AsyncPersisted, Bindable, AsyncRemoteBackedSource, Specd {}
