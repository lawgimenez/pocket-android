package com.pocket.sync.source.threads;

import com.pocket.sync.source.AppSource;
import com.pocket.sync.source.PendingResult;
import com.pocket.util.java.Logs;

import java.util.concurrent.CountDownLatch;

/**
 * A general purpose implementation of {@link PendingResult}.
 * To use, have your implementation create an instance of this, and invoke {@link #success(Object)} or {@link #fail(Throwable)} as needed.
 * Return the general interface, {@link PendingResult}, not this class type, otherwise they will have access to those setter methods.
 *
 * REVIEW, consider sharing code with {@link AppSource}'s implementation?
 */
public class PendingImpl<T, E extends Throwable> implements PendingResult<T,E> {

	/** A latch that will be used for implementing {@link #get}*/
	private final CountDownLatch latch = new CountDownLatch(1);
	/** Where to invoke callbacks. */
	private Publisher publisher;
	/** The success callback, if it has been set. */
	private PendingResult.SuccessCallback<T> onSuccess;
	/** The error callback, if it has been set. */
	private PendingResult.ErrorCallback<E> onFail;
	/** The complete callback, if it has been set. */
	private PendingResult.CompleteCallback onComplete;
	/** The result if it completed successfully */
	private T result;
	/** The error if it completed with a failure. */
	private E error;
	/** If it is completed */
	private boolean isComplete;
	/** If it was abandoned via {@link PendingResult#abandon()} */
	private boolean abandoned;
	/** See {@link #proxy(PendingResult)} */
	private PendingResult proxy;

	public PendingImpl(Publisher publisher) {
		this.publisher = publisher;
	}

	/**
	 * Set this was successful, with this result value.
	 */
	public synchronized void success(T result) {
		this.result = result;
		publish();
	}

	/**
	 * Set this failed, with this error.
	 */
	public synchronized void fail(E error) {
		this.error = error;
		publish();
	}

	/**
	 * If {@link #abandon()} is invoked on this instance, also invoke it on this other {@link PendingResult}
	 */
	public synchronized void proxy(PendingResult of) {
		this.proxy = of;
		if (abandoned && proxy != null) abandon();
	}

	@Override
	public synchronized PendingResult<T,E> publisher(Publisher publisher) {
		this.publisher = publisher != null ? publisher : Publisher.CALLING_THREAD;
		return this;
	}

	private synchronized void publish() {
		if (abandoned) return;
		isComplete = true;
		latch.countDown();
		if (error == null) {
			if (onSuccess != null) {
				publisher.publish(() -> {
					if (abandoned || onSuccess == null) return;
					onSuccess.onSuccess(result);
				});
			}
		} else {
			Logs.printStackTrace(error);
			if (onFail != null) {
				publisher.publish(() -> {
					if (abandoned || onFail == null) return;
					onFail.onError(error);
				});
			}
		}
		if (onComplete != null) {
			publisher.publish(() -> {
				if (abandoned || onComplete == null) return;
				onComplete.onComplete();
			});
		}
	}

	@Override
	public T get() throws E {
		try {
			latch.await();
			synchronized (this) {
				if (abandoned) return null;
				if (error != null) throw error;
				return result;
			}

		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public synchronized void abandon() {
		if (proxy != null) proxy.abandon();
		abandoned = true;
		onComplete = null;
		onSuccess = null;
		onFail = null;
		proxy = null;
	}

	@Override
	public synchronized PendingResult<T,E> onSuccess(SuccessCallback<T> successCallback) {
		if (abandoned) return this;
		onSuccess = successCallback;

		if (isComplete
		&& error == null
		&& onSuccess != null) {
			publisher.publish(() -> onSuccess.onSuccess(result));
		}
		return this;
	}

	@Override
	public synchronized PendingResult<T,E> onFailure(ErrorCallback<E> failureCallback) {
		if (abandoned) return this;
		onFail = failureCallback;

		if (isComplete
		&& error != null
		&& onFail != null) {
			publisher.publish(() -> onFail.onError(error));
		}
		return this;
	}

	@Override
	public synchronized PendingResult<T,E> onComplete(CompleteCallback callback) {
		if (abandoned) return this;
		onComplete = callback;

		if (isComplete) {
			publisher.publish(() -> onComplete.onComplete());
		}
		return this;
	}
}