package com.pocket.sdk.api.source;

import com.pocket.sdk.api.endpoint.Credentials;
import com.pocket.sdk.api.generated.PocketRemoteStyle;
import com.pocket.sdk.api.generated.enums.SnowplowAppId;
import com.pocket.sdk.api.generated.thing.ArticleView;
import com.pocket.sdk.network.eclectic.EclecticHttp;
import com.pocket.sync.action.Action;
import com.pocket.sync.source.FullResultSource;
import com.pocket.sync.source.LimitedSource;
import com.pocket.sync.source.Remote;
import com.pocket.sync.source.SynchronousSource;
import com.pocket.sync.source.result.Result;
import com.pocket.sync.source.result.Status;
import com.pocket.sync.source.result.SyncException;
import com.pocket.sync.source.result.SyncResult;
import com.pocket.sync.spec.Syncable;
import com.pocket.sync.thing.Thing;
import com.pocket.util.java.Safe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Handles syncing to Pocket's various remote servers.
 * <p>
 * Stateless aside from some settings such as what {@link Credentials} it will use.
 * You must invoke {@link #setCredentials(Credentials)} before using.
 * <p>
 * This includes a few settings methods.
 * See {@link V3Source} and {@link AdzerkSource} for additional usage rules.
 */
public class PocketRemoteSource implements SynchronousSource, FullResultSource, LimitedSource {
	
	private final V3Source v3;
	private final ClientApiSource clientApi;
	private final AdzerkSource adzerk;
	private final SnowplowSource snowplow;

	public PocketRemoteSource(EclecticHttp httpClient) {
		this(httpClient,
				V3Source.PRODUCTION_SERVER,
				ArticleView.REMOTE.address,
				new SnowplowSource.Config(SnowplowSource.PRODUCTION_COLLECTOR, SnowplowSource.PRODUCTION_POST_PATH,
						SnowplowAppId.POCKET_ANDROID));
	}
	
	/**
	 * @param httpClient What to make http calls with
	 * @param v3 What server to use for v3 calls. See the server parameter in {@link V3Source}'s constructor.
	 * @param parser What server to use for parser calls. See the parser parameter in {@link V3Source}'s constructor.
	 * @param snowplow What collector server and application ID to use for sending Snowplow events. See {@link SnowplowSource.Config}.
	 */
	public PocketRemoteSource(
			EclecticHttp httpClient,
			String v3,
			String parser,
			SnowplowSource.Config snowplow
	) {
		this.v3 = new V3Source(httpClient, v3, parser);
		this.clientApi = new ClientApiSource(httpClient);
		this.adzerk = new AdzerkSource(httpClient);
		this.snowplow = new SnowplowSource(httpClient, snowplow);
		setCredentials(null);
	}

	public synchronized PocketRemoteSource setCredentials(Credentials credentials) {
		v3.setCredentials(credentials);
		clientApi.setCredentials(credentials);
		adzerk.setUserAgent(Safe.get(() -> credentials.getDevice().userAgent));
		snowplow.setCredentials(credentials);
		return this;
	}

	/** See {@link V3Source#setMaxActions(int)} */
	public synchronized PocketRemoteSource setMaxActions(int value) {
		v3.setMaxActions(value);
		snowplow.setMaxActions(value);
		return this;
	}
	
	@Override
	public synchronized <T extends Thing> T sync(T requested, Action... actions) throws SyncException {
		SyncResult<T> r = syncFull(requested, actions);
		if (r.hasFailures()) {
			throw new SyncException(r);
		} else {
			return r.returned_t;
		}
	}
	
	@Override
	public <T extends Thing> SyncResult<T> syncFull(T thing, Action... actions) {
		SyncResult.Builder<T> syncResult = new SyncResult.Builder<>(thing, actions);

		// Actions first
		if (actions.length > 0) {
			Action[] filtered;

			// Pocket Actions

			// v3
			filtered = filter(actions, v3);
			if (filtered.length > 0) applyResults(syncResult, v3.syncFull(null, filtered));
			// client api
			filtered = filter(actions, clientApi);
			if (filtered.length > 0) applyResults(syncResult, clientApi.syncFull(null, filtered));

			if (syncResult.hasNonDiscardedFailures()) {
				return syncResult.build(Status.NOT_ATTEMPTED);
			}

			// Actions that don't block sync, even if there is a failure

			// adzerk
			filtered = filter(actions, adzerk);
			if (filtered.length > 0) {
				SyncResult<T> adzerkResult = adzerk.syncFull(null, filtered);
				applyResults(syncResult, adzerkResult);

				// Avoid breaking sync with our servers if this 3rd party integration has issues.
				avoidBreakingSync(syncResult, adzerkResult);
			}

			// snowplow
			filtered = filter(actions, snowplow);
			if (filtered.length > 0) {
				SyncResult<T> snowplowResult = snowplow.syncFull(null, filtered);
				applyResults(syncResult, snowplowResult);

				// These are analytic actions only, they never change state, so prefer dropping
				// them instead of breaking sync completely.
				avoidBreakingSync(syncResult, snowplowResult);
			}
		}

		// Then get
		if (thing != null) {
			Remote et = thing.remote();
			SyncResult<Thing> thingResult;
			switch ((PocketRemoteStyle) et.style) {
				case V3:
				case PARSER:
					thingResult = v3.syncFull(thing);
					break;
				case CLIENT_API:
					thingResult = clientApi.syncFull(thing);
					break;
				case ADZERK:
					thingResult = adzerk.syncFull(thing);
					break;
				default:
					syncResult.thing(Status.FAILED, new ClassCastException(thing.type() + " had unknown style " + et.style), null);
					return syncResult.build();
			}

			for (Thing r : thingResult.resolved) {
				syncResult.resolved(r);
			}

			if (thingResult.returned_t != null) {
				syncResult.thing((T) thingResult.returned_t);
			} else if (thingResult.result_t != null) {
				syncResult.thing(thingResult.result_t.status, thingResult.result_t.cause, thingResult.result_t.message);
			} else {
				syncResult.thing(Status.FAILED, new ClassCastException(thing.type() + " had no result"), null);
			}
		}

		return syncResult.build(Status.IGNORED);
	}

	private <T extends Thing> void avoidBreakingSync(
			SyncResult.Builder<T> syncResult,
			SyncResult<T> result
	) {
		if (result.hasFailures() && syncResult.hasSuccesses()) {
			// If we know other sources were able to successfully connect, then consider
			// any FAILED actions from this source to be FAILED_DISCARD.
			//
			// This avoids breaking sync completely if this source is down
			// or returns unexpected responses. We will still try to
			// retry these actions if our connection is down and everything fails. But if
			// the connection is working and Pocket actions are working, then we will discard
			// failed actions so we can continue to work with other sources.
			for (Map.Entry<Action, Result> entry : result.result_a.entrySet()) {
				Result r = entry.getValue();
				if (r.status == Status.FAILED) {
					syncResult.action(entry.getKey(), Status.FAILED_DISCARD, r.cause, r.message);
				}
			}
		}
	}

	/** Filter the array, returning a new array, of only actions supported by this source. */
	private Action[] filter(Action[] actions, LimitedSource source) {
		List<Action> filtered = new ArrayList<>();
		for (Action a : actions) {
			if (source.isSupported(a)) {
				filtered.add(a);
			}
		}
		return filtered.isEmpty() ? new Action[0] : filtered.toArray(new Action[0]);
	}

	/** Copies all action results and resolved things from a result into the builder. */
	private <T extends Thing> void applyResults(SyncResult.Builder<T> into, SyncResult<T> from) {
		for (Map.Entry<Action, Result> entry : from.result_a.entrySet()) {
			into.action(entry.getKey(), entry.getValue());
		}
		for (Thing r : from.resolved) {
			into.resolved(r);
		}
	}

	@Override
	public boolean isSupported(Syncable syncable) {
		return v3.isSupported(syncable) ||
				clientApi.isSupported(syncable) ||
				adzerk.isSupported(syncable) ||
				snowplow.isSupported(syncable);
	}

	/**
	 * Helper for some additional clean up for {@link com.pocket.sync.source.Remote.RemoteCallDetails} for pocket stuff.
	 */
	public static void cleanupRemoteCallDetails(Remote.RemoteCallDetails details) {
		// Strip out some fields that aren't intended to be sent to remote endpoints
		if (details.thingOrAction instanceof Action) {
			details.remainingFieldsAliased.remove("action");
			details.remainingFieldsAliased.remove("context");
			details.remainingFieldsAliased.remove("time");
			details.remainingFields.remove("action");
			details.remainingFields.remove("context");
			details.remainingFields.remove("time");
		}
	}

}
