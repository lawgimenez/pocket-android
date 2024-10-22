package com.pocket.sync.source;

import com.pocket.sync.source.threads.PendingImpl;
import com.pocket.sync.source.threads.Publisher;

/**
 * Represents the asynchronous result of some work.
 *
 * @param <T> The type of value returned when successful. (Can be {@link Void} if there is no returned result.)
 * @param <E> The type of error that will be returned if failed.
 */
public interface PendingResult<T, E extends Throwable> {
	static <T, E extends Throwable> PendingResult<T, E> of(T value) {
		PendingImpl<T, E> pending = new PendingImpl<>(Publisher.CALLING_THREAD);
		pending.success(value);
		return pending;
	}

	/**
	 * Block the current thread until the operation is complete, throwing any errors it encounters or returning the result.
	 * If already completed, returns (or throws) immediately.
	 */
	T get() throws E;
	
	/**
	 * Set a callback to be invoked if the work is successful. Overrides any previously set callback.
	 * If already completed, the callback will be invoked immediately.
	 * Note: depending on the implementation, it may use a {@link Publisher} and invoke callbacks on some other thread.
	 */
	PendingResult<T, E> onSuccess(SuccessCallback<T> successCallback);
	
	/**
	 * Set a callback to be invoked if the work fails. Overrides any previously set callback.
	 * If already completed, the callback will be invoked immediately.
	 * Note: depending on the implementation, it may use a {@link Publisher} and invoke callbacks on some other thread.
	 */
	PendingResult<T, E> onFailure(ErrorCallback<E> failureCallback);
	
	/**
	 * Set a callback to be invoked after a result is determined, regardless of the result.
	 * Invoked after either {@link #onSuccess(SuccessCallback)} or {@link #onFailure(ErrorCallback)} is invoked.
	 * If already completed, the callback will be invoked immediately.
	 * Note: depending on the implementation, it may use a {@link Publisher} and invoke callbacks on some other thread.
	 */
	PendingResult<T, E> onComplete(CompleteCallback callback);
	
	/**
	 * Set a {@link Publisher} to be used when invoking callbacks, overriding whatever is default.
	 * If null, {@link Publisher#CALLING_THREAD} will be used.
	 */
	PendingResult<T, E> publisher(Publisher publisher);
	
	/**
	 * Declare that this result is no longer needed.
	 * Implementations may use this as an indication to stop doing some work. See each implementations for details.
	 * After invoking this, no callbacks will invoked and {@link #get()} will always return null.
	 */
	void abandon();
	
	interface SuccessCallback<T> {
		void onSuccess(T result);
	}
	
	interface ErrorCallback<T extends Throwable> {
		void onError(T error);
	}
	
	interface CompleteCallback {
		void onComplete();
	}
	
}
