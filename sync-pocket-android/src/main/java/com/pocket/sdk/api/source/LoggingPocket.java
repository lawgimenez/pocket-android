package com.pocket.sdk.api.source;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.PocketRemoteStyle;
import com.pocket.sync.action.Action;
import com.pocket.sync.source.PendingResult;
import com.pocket.sync.source.result.RemotePriority;
import com.pocket.sync.source.result.SyncException;
import com.pocket.sync.source.subscribe.Changes;
import com.pocket.sync.source.subscribe.PublishingSubscriber;
import com.pocket.sync.source.subscribe.Subscriber;
import com.pocket.sync.source.subscribe.Subscription;
import com.pocket.sync.source.threads.Publisher;
import com.pocket.sync.space.Holder;
import com.pocket.sync.spec.Syncable;
import com.pocket.sync.thing.Thing;
import com.pocket.util.java.JsonUtil;
import com.pocket.util.java.Logs;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A utility for debugging {@link Pocket} through various levels of logging.
 * Use one of the static methods like {@link #debug(Config, Logger)} for some presets or customize using the constructor.
 * <p>
 * Wraps a {@link Pocket} instance and logs as it is used. This is not meant to be used outside of a dev or team build,
 * as it likely has significant performance impacts due to the extra wrapping and work it does.
 */
public class LoggingPocket extends Pocket {
	
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
	
	public enum Include {
		/** Log when non-local, non-endpoint actions are submitted. In v3 style, in a pretty print format. (Useful for QA to see when and what actions that are fired) */
		ACTIONS,
		/** Log all calls to the Pocket instance in a compact format. (Has no effect if included with {@link #METHOD_CALLS_EXPANDED} */
		METHOD_CALLS_COMPACT,
		/** Log all calls to the Pocket instance, with things and actions in a expanded pretty print format. */
		METHOD_CALLS_EXPANDED,
		/** (For use with {@link #METHOD_CALLS_COMPACT} or {@link #METHOD_CALLS_EXPANDED}) Include a short stack of what invoked the method call. */
		METHOD_CALLS_STACK,
		/** (For use with {@link #METHOD_CALLS_COMPACT} or {@link #METHOD_CALLS_EXPANDED}) Log the result of each method call, in compact or expanded form depending on the METHOD_CALLS_ style included. */
		METHOD_CALLS_RESULTS,
		/** Log how long each method call took to invoke. (This is not how long it took to return its async result, but how long it took to just invoke the method itself. */
		INVOKE_TIME,
		/** Log how long each request took to have a success or failure response. This is included within {@link #METHOD_CALLS_RESULTS} so if that is already included this has no additional effect. */
		RESPONSE_TIME,
	}
	
	/**
	 * Very verbose logging for active debugging or troubleshooting.
	 */
	public static LoggingPocket debug(Config pktconfig, Logger logger) {
		return new LoggingPocket(pktconfig, logger,
				Include.METHOD_CALLS_EXPANDED, Include.METHOD_CALLS_STACK, Include.METHOD_CALLS_RESULTS, Include.INVOKE_TIME);
	}
	
	/**
	 * Lots of info for debugging, but in a more compact format
	 */
	public static LoggingPocket debugCompact(Config pktconfig, Logger logger) {
		return new LoggingPocket(pktconfig, logger, Include.METHOD_CALLS_COMPACT, Include.METHOD_CALLS_RESULTS);
	}
	
	/**
	 * Fairly verbose but less detail recommended for general development when you maybe sometimes want to look up what happened after the fact.
	 */
	public static LoggingPocket dev(Config pktconfig, Logger logger) {
		return new LoggingPocket(pktconfig, logger, Include.METHOD_CALLS_COMPACT, Include.INVOKE_TIME);
	}
	
	/**
	 * Logs invocation durations
	 */
	public static LoggingPocket profiling(Config pktconfig, Logger logger) {
		return new LoggingPocket(pktconfig, logger, Include.INVOKE_TIME, Include.RESPONSE_TIME);
	}
	
	/**
	 * Just logs actions for QA to see. (Make sure the logger has visible outputs in QA builds)
	 */
	public static LoggingPocket qa(Config pktconfig, Logger logger) {
		return new LoggingPocket(pktconfig, logger, Include.ACTIONS);
	}
	
	private final Logger logger;
	private final Set<Include> style = new HashSet<>();
	/** An incrementing id to uniquely identify each call. */
	private final AtomicInteger event = new AtomicInteger();
	
	/**
	 * @param pktconfig The config for the pocket instance
	 * @param logger Where to output logs
	 * @param include What information to include. Also see the static methods for some common cases. <b>If you don't want any logging, do not use this class at all! Just make a normal {@link Pocket} instance. This is not meant for production or user facing builds.</b>
	 */
	public LoggingPocket(Config pktconfig, Logger logger, Include... include) {
		super(pktconfig);
		this.logger = logger;
		Collections.addAll(style, include);
	}
	
	public interface Logger {
		void log(String log);
	}

	@Override
	public <T extends Thing> PendingResult<T, SyncException> sync(T thing, Action... actions) {
		return invoke("sync", () -> super.sync(thing, actions), thing, actions);
	}
	
	@Override
	public <T extends Thing> PendingResult<T, SyncException> syncRemote(T thing, Action... actions) {
		return invoke("syncRemote", () -> super.syncRemote(thing, actions), thing, actions);
	}
	
	@Override
	public <T extends Thing> PendingResult<T, SyncException> syncLocal(T thing, Action... actions) {
		return invoke("syncLocal", () -> super.syncLocal(thing, actions), thing, actions);
	}
	
	@Override
	public PendingResult<Void, SyncException> syncActions(RemotePriority type) {
		return invoke("syncActions", () -> super.syncActions(type), type);
	}
	
	@Override
	public PendingResult<Void, Throwable> remember(Holder holder, Thing... identities) {
		return invoke("remember", () -> super.remember(holder, identities), holder, identities);
	}
	
	@Override
	public PendingResult<Void, Throwable> forget(Holder holder, Thing... identities) {
		return invoke("forget", () -> super.forget(holder, identities), holder, identities);
	}
	
	@Override
	public PendingResult<boolean[], Throwable> contains(Thing... things) {
		return invoke("contains", () -> super.contains(things), (Object[]) things);
	}
	
	@Override
	public PendingResult<boolean[], Throwable> contains(String... idkeys) {
		return invoke("contains", () -> super.contains(idkeys), (Object[]) idkeys);
	}
	
	@Override
	public <T extends Thing> Subscription subscribe(Changes<T> change, Subscriber<T> subscriber) {
		return invoke("subscribe", change, subscriber, null,
				(t, s, f) -> LoggingPocket.super.subscribe(change, s));
	}
	
	@Override
	public <T extends Thing> Subscription bindLocal(T thing, Subscriber<T> subscriber, BindingErrorCallback onFailure) {
		return invoke("bindLocal", thing, subscriber, onFailure, LoggingPocket.super::bindLocal);
	}
	
	@Override
	public <T extends Thing> Subscription bind(T thing, Subscriber<T> subscriber, BindingErrorCallback onFailure) {
		return invoke("bind", thing, subscriber, onFailure, LoggingPocket.super::bind);
	}
	
	
	
	
	
	private interface InvokeBind<V, T extends Thing> {
		Subscription invoke(V target, Subscriber<T> subscriber, BindingErrorCallback onFailure);
	}
	
	private interface Invoke<R, E extends Throwable> {
		PendingResult<R, E> invoke();
	}
	
	/**
	 * Invokes the method and handles logging of the method and its responses.
	 * @param method The name of the method (just for displaying in logs)
	 * @param invoke An interface that will invoke the method
	 * @param params The params passed to the method (just for displaying in logs, not used functionally)
	 * @return A pending result for the method.
	 */
	private <R, E extends Throwable> PendingResult<R, E> invoke(String method, Invoke<R,E> invoke, Object... params)  {
		long start = System.currentTimeMillis();
		String id = StringUtils.leftPad(String.valueOf(event.getAndAdd(1)), 6, "0");
		
		// Handle pre invocation logs.
		
		if (style.contains(Include.METHOD_CALLS_EXPANDED)) {
			StringBuilder log = new StringBuilder().append(id).append(" : ").append(DATE_FORMAT.format(new Date(start))).append("\n");
			if (style.contains(Include.METHOD_CALLS_STACK)) log.append("\tINVOKED FROM").append(Logs.stack(5)).append("\n");
			log.append("\t").append(method).append("(");
			List<Object> elements = new ArrayList<>();
			for (Object p : params) {
				if (p instanceof Object[]) {
					Collections.addAll(elements, (Object[]) p);
				} else if (p instanceof Collection) {
					elements.addAll((Collection) p);
				} else {
					elements.add(p);
				}
			}
			if (!elements.isEmpty()) {
				log.append("\n");
				for (Object e : elements) {
					if (e instanceof Thing) {
						appendExpanded(log, e, "\t\t");
						log.append(",\n");
					} else {
						log.append("\t\t").append(e).append(",\n");
					}
				}
			}
			log.append("\t)\n");
			logger.log(log.toString());
			
		} else if (style.contains(Include.METHOD_CALLS_COMPACT)) {
			StringBuilder log = new StringBuilder().append(id).append(" : ").append(DATE_FORMAT.format(new Date(start)));
			log.append(" ").append(method).append("(");
			for (int i = 0; i < params.length; i++) {
				Object p = params[i];
				if (p instanceof Object[]) {
					log.append(Arrays.toString((Object[]) p));
				} else {
					log.append(p);
				}
				if (i < params.length-1) log.append(", ");
			}
			log.append(")");
			if (style.contains(Include.METHOD_CALLS_STACK)) log.append("\t").append(Logs.stack(5)).append("\n");
			logger.log(log.toString());
		}
		
		if (style.contains(Include.ACTIONS)) {
			StringBuilder log = new StringBuilder("Actions : ").append(DATE_FORMAT.format(new Date(start))).append("\n");
			boolean output = false;
			for (Object e : params) {
				if (e instanceof Action[]) {
					for (Action a : (Action[]) e) {
						if (a.remote().style == PocketRemoteStyle.LOCAL || a.remote().address != null) continue;
						appendExpanded(log, V3Source.toV3ActionJson(a), "\t\t");
						log.append("\n");
						output = true;
					}
				}
			}
			if (output) logger.log(log.toString());
		}

		// Invoke the method
		
		long beforeInvoke = System.currentTimeMillis();
		PendingResult<R, E> pending = invoke.invoke();
		long afterInvoke = System.currentTimeMillis();
		
		if (style.contains(Include.INVOKE_TIME)) {
			logger.log(id + " INVOKED IN " + (afterInvoke-beforeInvoke) + "ms");
		}
		
		// Handle result logging if needed
		
		if (style.contains(Include.METHOD_CALLS_RESULTS) || style.contains(Include.RESPONSE_TIME)) {
			// Wrap so we can capture the results
			return new PendingResult<R, E>() {
				
				boolean logged;
				
				private void result(R result, E error) {
					if (logged) return;
					logged = true;
					logResult(id, method, start, error != null ? error : result);
				}
				
				@Override
				public R get() throws E {
					return pending.get();
				}
				
				@Override
				public PendingResult<R, E> onSuccess(SuccessCallback<R> successCallback) {
					pending.onSuccess(result -> {
						result(result, null);
						successCallback.onSuccess(result);
					});
					return this;
				}
				
				@Override
				public PendingResult<R, E> onFailure(ErrorCallback<E> failureCallback) {
					pending.onFailure(error -> {
						result(null, error);
						failureCallback.onError(error);
					});
					return this;
				}
				
				@Override
				public PendingResult<R, E> onComplete(CompleteCallback callback) {
					pending.onComplete(callback);
					return this;
				}
				
				@Override
				public void abandon() {
					pending.abandon();
				}
				
				@Override
				public PendingResult<R, E> publisher(Publisher publisher) {
					pending.publisher(publisher);
					return this;
				}
			}
					// Setup result logging even if the caller doesn't set callbacks
					.onSuccess(r -> {})
					.onFailure(e -> {});
			
		} else {
			return pending;
		}
	}
	
	/**
	 * Invokes the method and handles logging of the method and its responses.
	 * A variant for subscribe/bind methods.
	 *
	 * @param method The name of the method (just for displaying in logs)
	 * @param target The object to subscribe/bind to
	 * @param onFailure The error callback (for bind methods)
	 * @param invoke An interface that will invoke the method
	 * @return A subscription from the binding/subscribing
	 */
	private <V, T extends Thing> Subscription invoke(String method, V target, Subscriber<T> subscriber, BindingErrorCallback onFailure, InvokeBind<V, T> invoke) {
		long start = System.currentTimeMillis();
		String id = StringUtils.leftPad(String.valueOf(event.getAndAdd(1)), 6, "0");
		StringBuilder log = new StringBuilder().append(id).append(" : ").append(DATE_FORMAT.format(new Date(start)));
		
		if (style.contains(Include.METHOD_CALLS_EXPANDED)) {
			log.append("\n");
			if (style.contains(Include.METHOD_CALLS_STACK)) log.append("\t INVOKED FROM ").append(Logs.stack(5)).append("\n");
			log.append("\t").append(method).append("(\n");
			appendExpanded(log, target, "\t\t");
			log.append("\n");
			log.append("\t)");
			logger.log(log.toString());
			
		} else if (style.contains(Include.METHOD_CALLS_COMPACT)) {
			log.append(" ").append(method).append("(").append(target).append(")");
			if (style.contains(Include.METHOD_CALLS_STACK)) log.append("\t").append(Logs.stack(5)).append("\n");
			logger.log(log.toString());
		}
		
		Subscriber<T> wrappedSubscriber = subscriber;
		BindingErrorCallback wrappedError = onFailure;
		
		if (style.contains(Include.METHOD_CALLS_RESULTS) || style.contains(Include.RESPONSE_TIME)) {
			wrappedSubscriber = updated -> {
				logResult(id, method, start, updated);
				subscriber.onUpdate(updated);
				
			};
			if (subscriber instanceof PublishingSubscriber) {
				wrappedSubscriber = new PublishingSubscriber<>(wrappedSubscriber, ((PublishingSubscriber)subscriber).publisher);
			}
			wrappedError = (error, subscription) -> {
				logResult(id, method, start, error);
				if (onFailure != null) onFailure.onBindingError(error, subscription);
				
			};
		}
		
		long beforeInvoke = System.currentTimeMillis();
		Subscription sub = invoke.invoke(target, wrappedSubscriber, wrappedError);
		long afterInvoke = System.currentTimeMillis();
		
		if (style.contains(Include.INVOKE_TIME)) {
			logger.log(id + " INVOKED IN " + (afterInvoke-beforeInvoke) + "ms");
		}
		
		return sub;
	}
	
	/**
	 * Output a result log
	 * @param id The event's id
	 * @param method The event's method name (for displaying in logs)
	 * @param start When the method was first invoked
	 * @param result The result of the method, either the value or a Throwable
	 */
	private void logResult(String id, String method, long start, Object result) {
		long now = System.currentTimeMillis();
		long duration = now - start;
		StringBuilder log = new StringBuilder().append(id).append(" ").append(method).append(" RESULT IN ").append(duration).append("ms");
		if (style.contains(Include.METHOD_CALLS_RESULTS)) {
			log.append(" -> ");
			log.append(result instanceof Throwable ? "Failed : " : "Success : ");
			if (style.contains(Include.METHOD_CALLS_EXPANDED) && result instanceof Thing) {
				appendExpanded(log, result, "\t\t");
			} else {
				log.append(result);
			}
		} else if (style.contains(Include.RESPONSE_TIME)) {
			// Already included above, nothing additional to add
		}
		logger.log(log.toString());
	}
	
	/**
	 * Append the thing to the string builder in a pretty print format.
	 * @param builder Where to append
	 * @param thing The object to print, if it is a Thing or JsonNode it will be pretty printed, otherwise just normal toString()
	 * @param indent An indent to apply to each line or blank "" to not have an indent.
	 */
	private static void appendExpanded(StringBuilder builder, Object thing, String indent) {
		String header = "";
		if (thing instanceof Thing) {
			header = ((Thing) thing).type();
			thing = ((Thing) thing).toJson(Syncable.NO_ALIASES);
		}
		if (thing instanceof ObjectNode) {
			String expanded = JsonUtil.prettyPrint((ObjectNode) thing);
			expanded = StringUtils.replace(expanded, "\n", "\n"+indent); // Add the indent to each line.
			builder.append(indent).append(header).append(expanded);
		} else {
			builder.append(indent).append(thing);
		}
	}
	
	
}
