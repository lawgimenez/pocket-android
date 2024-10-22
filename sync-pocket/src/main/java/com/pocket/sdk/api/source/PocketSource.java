package com.pocket.sdk.api.source;

import com.pocket.sdk.api.endpoint.Credentials;
import com.pocket.sdk.api.spec.PocketSpec;
import com.pocket.sync.source.AppSource;
import com.pocket.sync.source.threads.Publisher;
import com.pocket.sync.source.threads.ThreadPools;
import com.pocket.sync.space.Space;

/**
 * A Pocket Source implementation that can work and persist locally and is linked with the Pocket v3 API.
 * Use {@link #setCredentials(Credentials)} to control what account is used when communicating with v3.
 * Use {@link com.pocket.sdk.Pocket} if you want user management to be handled for you.
 */
public class PocketSource extends AppSource {
	
	private final PocketRemoteSource remote;
	
	public PocketSource(PocketSpec spec, Space space, PocketRemoteSource remote, Publisher publisher, ThreadPools threads) {
		super(spec, space, remote, new PocketResolver(), publisher, threads);
		this.remote = remote;
	}
	
	/**
	 * Set what credentials will be used on future calls to the v3 api.
	 * This is not persisted across process death.
	 */
	public PocketSource setCredentials(Credentials c) {
		remote.setCredentials(c);
		return this;
	}
	
}
