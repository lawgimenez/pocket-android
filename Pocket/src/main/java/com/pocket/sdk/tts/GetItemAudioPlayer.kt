package com.pocket.sdk.tts

import android.content.Context
import androidx.core.os.LocaleListCompat
import com.pocket.app.AppThreads
import com.pocket.sdk.Pocket
import com.pocket.sdk.api.endpoint.ApiException
import com.pocket.sdk.api.generated.enums.AudioFileStatus
import com.pocket.sdk.api.generated.thing.GetItemAudio
import com.pocket.sdk.api.generated.thing.ItemAudioFile
import com.pocket.sync.rx.RxSyncResult
import com.pocket.sync.rx.toObservable
import com.pocket.sync.source.result.SyncException
import com.pocket.sync.space.Holder
import com.pocket.util.java.addTo
import com.pocket.util.prefs.FloatPreference
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Holds the getItemAudio thing synced in [GetItemAudioPlayer.preloadNext] so that we don't have to
 * sync it with the remote again in [GetItemAudioPlayer.load].
 */
private val GET_ITEM_AUDIO_HOLDER = Holder.session("getItemAudio")

private const val FORMAT_OPUS = "opus-mkv"
private const val FORMAT_MP3 = "mp3"

/** The only streaming voice supported by v3/getItemAudio for now.  */
private val STREAMING_VOICE = object : ListenState.Voice {
    private val TYPE = ListenState.Voice.Type("v3/getItemAudio")
    override fun getType() = TYPE
    override fun getName(): String? = null
    override fun getLocale(): Locale? = null
    override fun isNetworkConnectionRequired() = true
}


internal class GetItemAudioPlayer(
    private val pocket: Pocket,
    private val streamingPlayer: AndroidMediaPlayer
) : ListenPlayer, StreamingPlayer by streamingPlayer {

    constructor(
        context: Context,
        pocket: Pocket,
        threads: AppThreads,
        android: ListenMediaSession,
        initialSpeed: Float,
        lowestReportedFailingSpeed: FloatPreference
    ) : this(
        pocket,
        AndroidMediaPlayer(context, threads, android, initialSpeed, lowestReportedFailingSpeed)
    )

    private val loadRequests = PublishSubject.create<LoadItemRequest>()
    private val errors = PublishSubject.create<ListenError>()
    private val disposables = CompositeDisposable()
    private val lock = Any()

    init {
        loadRequests.distinctUntilChanged { request -> request.itemId }
            .debounce(300, TimeUnit.MILLISECONDS)
            .switchMap { request ->
                pocket.sync(buildGetItemAudioThing(request.itemId))
                    .toObservable()
                    .map { LoadItemAudioRequest(it, request.onLoaded) }
            }
            .subscribe {
                when(it.result) {
                    is RxSyncResult.Success<GetItemAudio, *> -> loadItemAudio(it.result.value, it.onLoaded)
                    is RxSyncResult.Failure<*, *> -> {
                        if (ApiException.unwrap(it.result.error) != null) {
                            errors.onNext(ListenError.SERVER_ERROR)
                        } else {
                            errors.onNext(ListenError.NETWORK_ERROR)
                        }
                    }
                }
            }
            .addTo(disposables)

        streamingPlayer.errors
            .map { it.toListenError() }
            .subscribe(errors)
    }

    override fun load(track: Track, loaded: ListenPlayer.OnLoaded?) {
        synchronized(lock) {
            if (isLoaded) {
                streamingPlayer.reset()
            }

            track.itemId?.let { loadRequests.onNext(LoadItemRequest(it, loaded)) }
        }
    }

    private fun loadItemAudio(result: GetItemAudio, loaded: ListenPlayer.OnLoaded?) {
        synchronized(lock) {
            pocket.forget(GET_ITEM_AUDIO_HOLDER, result)

            var opus: ItemAudioFile? = null
            var mp3: ItemAudioFile? = null

            for (file in result.files!!) {
                if (file.status != AudioFileStatus.AVAILABLE) continue

                if (FORMAT_OPUS == file.format) {
                    opus = file

                } else if (FORMAT_MP3 == file.format) {
                    mp3 = file
                }
            }

            when {
                // Use OPUS if available.
                opus != null -> streamingPlayer.load(opus.url, loaded)
                // Fallback to mp3.
                mp3 != null -> streamingPlayer.load(mp3.url, loaded)
                // Fail if no file found.
                else -> errors.onNext(ListenError.NETWORK_ERROR)
            }
        }
    }

    override fun preloadNext(itemId: String?) {
        if (itemId == null) {
            streamingPlayer.preloadNext(null)
            return
        }

        val getItemAudio = buildGetItemAudioThing(itemId)
        pocket.remember(GET_ITEM_AUDIO_HOLDER, getItemAudio)
        pocket.sync(getItemAudio)
            .onSuccess { result ->
                for (file in result.files!!) {
                    // Try to preload only the OPUS version. If not available now, it might be available
                    // by the time we try to load it normally.
                    if (file.status != AudioFileStatus.AVAILABLE || FORMAT_OPUS != file.format) {
                        continue
                    }

                    streamingPlayer.preloadNext(file.url)
                    return@onSuccess

                }
            }
    }

    private fun buildGetItemAudioThing(itemId: String): GetItemAudio {
        val defaultLocale = LocaleListCompat.getDefault().get(0)
        return pocket.spec().things().itemAudio
            .version("2")
            .itemId(itemId)
            .accent_locale(defaultLocale!!.toLanguageTag())
            .build()
    }

    override fun getVoice() = STREAMING_VOICE
    override fun getVoices(): MutableSet<VoiceCompat.Voice> = Collections.emptySet()

    override fun getReadies(): Observable<*> = Observable.just(Unit)
    override fun getErrors(): Observable<ListenError> = errors

    override fun getStartedUtterances(): Observable<Utterance> {
        // This is used by ListenFollowAlong, but this player doesn't support it, so don't emit anything.
        return Observable.empty()
    }

    override fun playFromNodeIndex(nodeIndex: Int) {
        // Ignore nodeIndex, it's specific to TTSPlayer
        play()
    }

    override fun setVoice(voice: ListenState.Voice?) {
        // Ignore for now, since we don't support multiple streaming voices.
        // This probably belongs on Engine class anyway? Maybe?
    }

    override fun release() {
        disposables.clear()
        streamingPlayer.release()
    }
}

private fun AndroidMediaPlayer.Error.toListenError(): ListenError {
    return when (this) {
        AndroidMediaPlayer.Error.IO -> ListenError.NETWORK_ERROR

        AndroidMediaPlayer.Error.TIMED_OUT, AndroidMediaPlayer.Error.UNKNOWN -> ListenError.TIMED_OUT

        AndroidMediaPlayer.Error.MALFORMED,
        AndroidMediaPlayer.Error.UNSUPPORTED,
        AndroidMediaPlayer.Error.SERVER_DIED,
        AndroidMediaPlayer.Error.SYSTEM -> ListenError.MEDIA_PLAYER
    }
}

private class LoadItemRequest(val itemId: String, val onLoaded: ListenPlayer.OnLoaded?)
private class LoadItemAudioRequest(
        val result: RxSyncResult<GetItemAudio, SyncException>,
        val onLoaded: ListenPlayer.OnLoaded?
)
