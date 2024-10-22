package com.pocket.sync.source;

/**
 * A {@link Source} that connects to another {@link Source}, like a client app that connects to a backend server.
 * <p>
 * This doesn't provide any methods by default since the actual form can be different for different use cases.
 * This just indicates that this is a source that is backed by another.
 * <p>
 * Implementations will likely have methods that let you have more control over syncing locally vs remotely.
 */
public interface RemoteBackedSource extends Source {}
