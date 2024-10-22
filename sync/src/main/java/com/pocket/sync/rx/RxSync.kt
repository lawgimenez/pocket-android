package com.pocket.sync.rx

import com.pocket.sync.source.PendingResult
import io.reactivex.Observable

@JvmName("observableFrom")
fun <T, E : Throwable> PendingResult<T, E>.toObservable(): Observable<RxSyncResult<T, E>> {
    return Observable.create { emitter ->
        onSuccess { emitter.onNext(RxSyncResult.Success(it)) }
        onFailure { emitter.onNext(RxSyncResult.Failure(it)) }
        emitter.setCancellable { abandon() }
    }
}

sealed class RxSyncResult<T, E : Throwable> {
    class Success<T, E : Throwable>(val value: T) : RxSyncResult<T, E>()
    class Failure<T, E : Throwable>(val error: E) : RxSyncResult<T, E>()
    
    fun getValueOr(default: T): T {
        return when (this) {
            is Success -> value
            is Failure -> default
        }
    }
}
