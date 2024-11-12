package com.pocket.sdk.api.source;

import com.pocket.sdk.Pocket;
import com.pocket.sync.source.Source;
import com.pocket.sync.source.subscribe.Changes;
import com.pocket.sync.source.subscribe.Subscription;
import com.pocket.sync.thing.Thing;

import io.reactivex.Observable;

/**
 * Rx extensions for working with a {@link Source}
 */
public class RxSource {
	
	public static <T extends Thing> Observable<T> subscribe(Pocket source, Changes<T> change) {
		return Observable.create(emitter -> {
			Subscription sub = source.subscribe(change, emitter::onNext);
			emitter.setCancellable(sub::stop);
		});
	}
	
	public static <T extends Thing> Observable<T> bind(Pocket source, T thing) {
		return Observable.create(emitter -> {
			Subscription sub = source.bind(thing, emitter::onNext, (error, subscription) -> emitter.onError(error));
			emitter.setCancellable(sub::stop);
		});
	}
	
}
