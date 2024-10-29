package com.pocket.sdk.tts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import com.ideashower.readitlater.BuildConfig
import com.pocket.app.App
import com.pocket.sdk.api.generated.enums.PositionType
import com.pocket.sdk.api.generated.thing.Position
import com.pocket.sdk.api.value.Timestamp
import com.pocket.sdk.api.value.UrlString
import com.pocket.sdk.tts.ArticleUtteranceParser.ArticleTTSUtterances
import com.pocket.sdk.tts.ArticleUtteranceParser.OnParsedListener
import com.pocket.sdk.tts.ListenPlayer.OnLoaded
import com.pocket.sdk.util.wakelock.WakeLockHolder
import com.pocket.sdk.util.wakelock.WakeLockHolder.LivelinessCheck
import com.pocket.sync.thing.Thing
import com.pocket.util.android.IntentUtils
import com.pocket.util.android.registerReceiverCompat
import com.pocket.util.java.Logs
import com.pocket.util.java.StopWatch
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.apache.commons.lang3.StringUtils
import org.threeten.bp.Duration
import java.util.*
import kotlin.math.max
import kotlin.math.min

class TTSPlayer internal constructor(private val mContext: Context?) : ListenPlayer {
    private val mReadySubject: Subject<Any> = BehaviorSubject.create()
    private val mStartedUtterances: Subject<Utterance> = PublishSubject.create()
    private val mCompletions: Subject<Any> = PublishSubject.create()
    private val mErrors: Subject<ListenError> = PublishSubject.create()
    private val mVoices: MutableSet<VoiceCompat.Voice> = HashSet()
    private val mPrefSpeed: Float
    private val mPrefPitch: Float
    private var mTts: TextToSpeech? = null
    private var mIsReady = false
    private var mIsShutdown = false
    private var mIsInitializing = false
    private var mLoadingUrl: String? = null
    private var mIsPendingPlayback = false
    private var mIsPlaying = false
    private var mUtterances: ArticleTTSUtterances? = null
    private var mCurrentTrack: Track? = null
    private var mCurrentPosition = 0
    private var mQueued = EMPTY_QUEUE
    private val mQueueLock = Any()
    private var mCurrentVoice: VoiceCompat.Voice? = null
    private var mSpeechRate = 0f
    private var mSpeechPitch = 0f
    private var mProgress = 0f
    private val mTimeSpent = StopWatch()
    private var mProgressUpdater: Runnable? = null

    /**
     * Do not tell listeners about a pause or play event while this is true. This is used to avoid telling listeners about
     * cases where we are pausing temporarily to change settings and then instantly resuming.
     */
    private var mIsTemporarilyPausing = false
    private var mTTSLanguageInstalledReceiver: BroadcastReceiver? = null
    private var mIsReset = true
    private var mWakeLockHolder: WakeLockHolder? = null

    init {
        mPrefSpeed = App.getApp().prefs().ARTICLE_TTS_SPEED.get()
        mPrefPitch = App.getApp().prefs().ARTICLE_TTS_PITCH.get()
        setWakeLockEnabled(true)
        initialize()
    }

    private fun initialize() {
        if (mIsInitializing || mIsReady) {
            return
        }
        if (!App.getApp().pktcache().isLoggedIn) {
            onInitError(ListenError.LOGGED_OUT)
            return
        }
        val checkIntent = Intent()
        checkIntent.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
        if (IntentUtils.isActivityIntentAvailable(mContext, checkIntent)) {
            // Device has TTS but need to check level of support
            mIsInitializing = true
            val engine = TtsEngines().preferredTtsEngine
            val listener = OnInitListener { status: Int ->
                mIsInitializing = false
                if (status == TextToSpeech.SUCCESS) {
                    updateAvailableLocales()
                    if (!mVoices.isEmpty()) {
                        // Ready for use!
                        onReady()
                    } else {
                        onInitError(ListenError.NO_VOICES)
                    }
                } else {
                    onInitError(ListenError.INIT_FAILED)
                }
            }
            mTts = TextToSpeech(mContext, listener, engine.name)
        } else {
            // Device is missing TTS
            onInitError(ListenError.NO_TTS_INSTALLED)
        }
    }

    private fun onReady() {
        mIsReady = true
        mTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String) {
                if (mIsPlaying) {
                    onUtteranceCompleted(utteranceId)
                } else {
                    // On some older APIs like 4.4 Kitkat, onDone is incorrectly invoked when calling TextToSpeech.stop().
                }
            }

            override fun onError(utteranceId: String) {
                onUtteranceError()
            }

            override fun onError(utteranceId: String, errorCode: Int) {
                onUtteranceError()
            }

            override fun onStart(utteranceId: String) {}
        })
        restorePreferences()
        mTTSLanguageInstalledReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (TextToSpeech.Engine.ACTION_TTS_DATA_INSTALLED != intent.action) {
                    return
                }
                updateAvailableLocales()
            }
        }
        mContext?.registerReceiverCompat(
            mTTSLanguageInstalledReceiver,
            IntentFilter(TextToSpeech.Engine.ACTION_TTS_DATA_INSTALLED),
            ContextCompat.RECEIVER_EXPORTED,
        )
        mReadySubject.onNext(Unit)
    }

    /**
     * Completely disable this player instance. Any connected clients will be forced to
     * disconnect and stop using this player instance.
     *
     *
     * It is safe to invoke this method multiple times. It will only perform the shutdown for the first call.
     */
    fun shutdown() {
        if (DEBUG) Logs.d(TAG, "shutdown")
        if (mIsShutdown) return
        mIsShutdown = true
        mIsReady = false
        if (mTTSLanguageInstalledReceiver != null && mContext != null) {
            mContext.unregisterReceiver(mTTSLanguageInstalledReceiver)
            mTTSLanguageInstalledReceiver = null
        }
        if (mTts != null) {
            release()
            mTts?.shutdown()
        }
        commitPrefs()
    }

    /**
     * Restore the user's last settings for TTS playback. This includes rate and language.
     */
    private fun restorePreferences() {
        mCurrentVoice = restoreVoicePreference()
        if (mCurrentVoice != null) {
            VoiceCompat.setVoice(mTts, mCurrentVoice)
        } else {
            // TODO handle this case where there is no default?
        }
        mSpeechRate = mPrefSpeed
        mSpeechPitch = mPrefPitch
        mTts?.setSpeechRate(mSpeechRate)
        mTts?.setPitch(mSpeechPitch)
    }

    private fun restoreVoicePreference(): VoiceCompat.Voice? {
        var voice: VoiceCompat.Voice? = null
        val language = App.getApp().prefs().ARTICLE_TTS_LANGUAGE.get()
        if (language != null) {
            val country = App.getApp().prefs().ARTICLE_TTS_COUNTRY.get()
            val variant = App.getApp().prefs().ARTICLE_TTS_VARIANT.get()
            val locale = Locale(
                language,
                StringUtils.defaultString(country),
                StringUtils.defaultString(variant)
            )
            voice = VoiceCompat.findBestMatch(
                locale,
                App.getApp().prefs().ARTICLE_TTS_VOICE.get(),
                mTts
            )
        }
        if (voice == null) {
            voice = VoiceCompat.findBestMatch(mTts)
        }
        return voice
    }

    private fun onInitError(error: ListenError) {
        mErrors.onNext(error)
    }

    private fun updateAvailableLocales() {
        val updated = VoiceCompat.getVoices(mTts)
        if (updated != mVoices) {
            mVoices.clear()
            mVoices.addAll(updated)
        }
    }

    private val isNotReady: Boolean
        private get() = !mIsReady

    override fun load(track: Track, loaded: OnLoaded) {
        mCurrentTrack = track
        val url = track.openUrl
        reset()
        mIsReset = false
        mLoadingUrl = url
        ArticleUtteranceParser(url, object : OnParsedListener {
            override fun onArticleUtterancesParsed(url: String, result: ArticleTTSUtterances) {
                if (!StringUtils.equals(mLoadingUrl, url) || !mIsReady) {
                    return  // Changed since this request was made, ignore.
                }
                mLoadingUrl = null
                mUtterances = result
                if (mIsPendingPlayback) {
                    play()
                }
                loaded.onArticleLoaded()
            }

            override fun onArticleUtterancesParserError(url: String, error: ListenError) {
                if (!StringUtils.equals(mLoadingUrl, url) || !mIsReady) {
                    return  // Changed since this request was made, ignore.
                }
                mLoadingUrl = null
                mErrors.onNext(error)
            }
        }).parse()
    }

    override fun preloadNext(itemId: String) {
        // Not supported/not needed.
    }

    /**
     * Stop any playback and unload any article.
     */
    override fun release() {
        reset()
        setWakeLockEnabled(false)
        shutdown()
    }

    /**
     * Reset the player state back to ready but unloaded
     */
    private fun reset() {
        if (mIsReset) return  // Ignore
        mIsReset = true
        pause()
        mUtterances = null
        mCurrentPosition = 0
        synchronized(mQueueLock) { mQueued = EMPTY_QUEUE }
        mIsPendingPlayback = false
        mProgress = 0f
        mTimeSpent.reset()
    }

    /**
     * Resume playback at the last position or beginning if it hasn't begun yet.
     *
     * If called while loading the article, it will begin playing immediately after finishing loading.
     * If called before calling [ListenPlayer.load], an exception will be thrown.
     */
    override fun play() {
        play(mCurrentPosition)
    }

    /**
     * Play starting from a specific node index.
     *
     * If called while loading the article, it will begin playing immediately after finishing loading.
     * If called before calling [ListenPlayer.load], an exception will be thrown.
     */
    override fun playFromNodeIndex(nodeIndex: Int) {
        val position: Int = if (nodeIndex == 0) {
            0
        } else {
            val u = mUtterances?.getByNodeIndex(nodeIndex)
            // TODO I've seen this happen in the wild, perhaps the article changes or something,
            //  find a way to fall back to percent?
            u?.position ?: 0
        }
        play(position)
    }

    /**
     * @param position The listen index of where to play from
     */
    private fun play(position: Int) {
        if (mUtterances == null) {
            // Not loaded
            if (mLoadingUrl != null) {
                mCurrentPosition = position
                mIsPendingPlayback = true
                // Will play after loading is complete
            } else {
                throw RuntimeException("No article is loaded")
            }
        } else {
            mIsPendingPlayback = false
            synchronized(mQueueLock) {
                mQueued = EMPTY_QUEUE
                mTts?.speak("", TextToSpeech.QUEUE_FLUSH, null)
            }
            val utterances = mUtterances!!.utterances
            val size = utterances.size
            if (size == 0) {
                return  // Nothing to say
            }
            mCurrentPosition = min(position, utterances.size - 1)
            onUtteranceStarted(utterances[mCurrentPosition])
            queueNextUtterances()
            updateProgress()
        }
    }

    /**
     * Queues up the next few utterances.
     *
     *
     * Breaking it up like this is to avoid talking after the app crashes or our process
     * is killed, leaving the user no obvious way to turn it off. This way at least it will
     * only speak for a short period of time afterwards.
     */
    private fun queueNextUtterances() {
        synchronized(mQueueLock) {
            val utterances: List<Utterance> = mUtterances?.utterances ?: emptyList()
            val start = max(
                mCurrentPosition,
                mQueued + 1
            ) // Either the current position when starting, or next one to queue
            val end = min(
                utterances.size - 1,
                mCurrentPosition + QUEUED_BUFFER
            ) // Either the last utterance, or needed buffer
            if (mQueued < end) {
                // Need to add some more to the buffer
                for (i in start..end) {
                    val utterance = utterances[i]
                    if (utterance.isHeader) {
                        mTts?.playSilence(
                            SILENCE_PRE_HEADER.toLong(),
                            TextToSpeech.QUEUE_ADD,
                            null
                        )
                    }
                    mTts?.speak(utterance.text, TextToSpeech.QUEUE_ADD, utterance.params)
                    mTts?.playSilence(
                        if (utterance.isHeader) SILENCE_POST_HEADER else SILENCE_POST_NODE,
                        TextToSpeech.QUEUE_ADD,
                        null
                    )
                    if (i == utterances.size - 1) {
                        // Last utterance.
                        mTts?.playSilence(SILENCE_END.toLong(), TextToSpeech.QUEUE_ADD, null)
                    }
                }
                mQueued = end
            }
        }
    }

    private fun onUtteranceError() {
        pause()
        mErrors.onNext(ListenError.SPEECH_ERROR)
    }

    override fun pause() {
        if (isNotReady) {
            return  // Ignore
        }
        if (!mIsTemporarilyPausing) {
            saveProgress()
        }
        mTts?.stop()
        setIsPlaying(false)
    }

    override fun seekTo(position: Duration) {
        // We don't know how to skip an exact time with TTS, so let's just skip one utterance forwards or backwards.
        if (position.compareTo(elapsed) > 0) {
            seek(1)
        } else {
            seek(-1)
        }
    }

    /**
     * Move the speaking position
     *
     * @param increment negative values will go backwards, positive forwards by one utterance from the current position
     */
    private fun seek(increment: Int) {
        // We don't know how to skip an exact time with TTS, so let's just skip one utterance forwards or backwards.
        var increment = increment
        increment = if (increment > 0) 1 else -1
        val newPosition = mCurrentPosition + increment
        if (!isLoaded || newPosition < 0 || newPosition >= (mUtterances?.utterances?.size ?: 0)) {
            // Invalid TODO handle
        } else {
            if (mIsPlaying) {
                mIsTemporarilyPausing = true
                pause()
                play(newPosition)
                mIsTemporarilyPausing = false
            } else {
                mCurrentPosition = newPosition
                updateProgress()
            }
        }
    }

    override fun isPlaying(): Boolean {
        return mIsPlaying
    }

    /**
     * Is there an article loaded into the player?
     */
    override fun isLoaded(): Boolean {
        return mUtterances != null
    }

    override fun getDuration(): Duration {
        // TTSPlayer doesn't know the actual time it takes to read an item.
        // We'll just fake with utterance lengths. The UI should degrade to show approximate progress not exact times.
        return if (mUtterances != null) {
            Duration.ofSeconds(mUtterances!!.length.toLong())
        } else Duration.ZERO
    }

    override fun getElapsed(): Duration {
        // TTSPlayer doesn't know the actual time it takes to read utterances.
        // We'll just fake with utterance lengths. The UI should degrade to show approximate progress not exact times.
        return if (mCurrentPosition <= 0 || mUtterances == null) {
            Duration.ZERO
        } else if (mCurrentPosition >= mUtterances!!.utterances.size) {
            Duration.ofSeconds(mUtterances!!.length.toLong())
        } else {
            Duration.ofSeconds(mUtterances!![mCurrentPosition - 1].endPosition.toLong())
        }
    }

    /**
     *
     * Set the voice for TTS. If current playing, it will restart the current utterance with the new voice and continue.
     * If not currently playing, it will stay paused.
     */
    override fun setVoice(listenVoice: ListenState.Voice) {
        val voice = listenVoice as VoiceCompat.Voice
        val wasPlaying = isPlaying
        mIsTemporarilyPausing = true
        pause()
        VoiceCompat.setVoice(mTts, voice)
        mCurrentVoice = voice
        setPitch(1f) // Reset pitch to restore the default sound for this voice. Leave speed as is because that is more of a user preference that "sound"
        if (wasPlaying) {
            play()
        }
        mIsTemporarilyPausing = false
    }

    override fun getVoice(): VoiceCompat.Voice? {
        return mCurrentVoice
    }

    override fun getVoices(): Set<VoiceCompat.Voice> {
        return mVoices
    }

    /**
     * Change the speech rate. If current playing, it will restart the current utterance at the new rate and continue.
     * If not currently playing, it will stay paused.
     *
     * @see TextToSpeech.setSpeechRate
     */
    override fun setSpeed(speechRate: Float) {
        val wasPlaying = isPlaying
        mIsTemporarilyPausing = true
        pause()
        mSpeechRate = speechRate
        mTts?.setSpeechRate(speechRate)
        if (wasPlaying) {
            play()
        }
        mIsTemporarilyPausing = false
    }

    /**
     * Change the speech pitch. If current playing, it will restart the current utterance at the new pitch and continue.
     * If not currently playing, it will stay paused.
     *
     * @see TextToSpeech.setPitch
     */
    override fun setPitch(pitch: Float) {
        val wasPlaying = isPlaying
        mIsTemporarilyPausing = true
        pause()
        mSpeechPitch = pitch
        mTts?.setPitch(pitch)
        if (wasPlaying) {
            play()
        }
        mIsTemporarilyPausing = false
    }

    /**
     * Since TextToSpeech.isSpeaking() only works properly if there is one utterance queued, we have to keep
     * track of this manually.
     */
    private fun setIsPlaying(isPlaying: Boolean) {
        val changed = mIsPlaying != isPlaying
        if (changed) {
            mIsPlaying = isPlaying
        }
        if (changed && !mIsTemporarilyPausing) {
            if (isPlaying) {
                mTimeSpent.resume()
            } else {
                mTimeSpent.pause()
            }
            if (DEBUG) Logs.d(TAG, "setIsPlaying changed $isPlaying")
        }
    }

    private fun onUtteranceStarted(utterance: Utterance) {
        setIsPlaying(true)
        mStartedUtterances.onNext(utterance)
    }

    /**
     * Note this will likely be called from off the main thread.
     *
     * @param utteranceId is the [Utterance.position] of the [Utterance] that just completed.
     */
    private fun onUtteranceCompleted(utteranceId: String) {
        mCurrentPosition = utteranceId.toInt()

        // Update progress and post so it runs on the main thread
        if (mProgressUpdater == null) {
            mProgressUpdater = Runnable { updateProgress() }
        }
        mProgressUpdater?.let { App.getApp().threads().handler.post(it) }
        if (mCurrentPosition == mUtterances!!.utterances.size - 1) {
            // We are completely finished speaking all text
            mCompletions.onNext(Unit)
            setIsPlaying(false)
            saveProgress()

            /*
			if (mAutoPlayNext) {
				// Article finished
				mReader.post(new Runnable(){
					@Override
					public void run() {
						mFrag.onArticleTTSComplete();
						updateIsPlaying();
					}
				});
			}
			*/
        } else {
            mCurrentPosition++
            onUtteranceStarted(mUtterances!![mCurrentPosition])
            queueNextUtterances()
        }
    }

    private fun updateProgress() {
        val progress: Float = if (mCurrentPosition > 0 && mUtterances != null) {
            mUtterances!![mCurrentPosition - 1].endPosition / mUtterances!!.length.toFloat()
        } else {
            0f
        }
        if (progress == mProgress) {
            return  // Hasn't changed
        }
        mProgress = progress
    }

    private fun commitPrefs() {
        mCurrentVoice?.let { currentVoice ->
            App.getApp().prefs().ARTICLE_TTS_COUNTRY.set(currentVoice.locale?.country)
            App.getApp().prefs().ARTICLE_TTS_LANGUAGE.set(currentVoice.locale?.language)
            App.getApp().prefs().ARTICLE_TTS_VARIANT.set(currentVoice.locale?.variant)
            App.getApp().prefs().ARTICLE_TTS_VOICE.set(currentVoice.name)
        }
    }

    /**
     * Saves the user's progress within this article.
     */
    private fun saveProgress() {
        if (isNotReady || !isLoaded) {
            return  // Nothing to save
        }
        updateProgress()
        val time = System.currentTimeMillis() / 1000
        val percent = (mProgress * 100).toInt()
        val nodeIndex = currentNodeIndex
        val newTimeSpent = (mTimeSpent.extract() / 1000).toInt()
        val pocket = App.from(mContext).pocket()
        val oldPosition = mCurrentTrack?.articlePosition?.syncPosition
        val newPosition = Position.Builder()
            .view(PositionType.ARTICLE)
            .node_index(nodeIndex)
            .percent(percent)
            .time_updated(Timestamp(time))
            .time_spent(oldPosition?.time_spent?.plus(newTimeSpent) ?: newTimeSpent)
            .build()
        if (newPosition != oldPosition) {
            pocket.sync<Thing?>(
                null,
                pocket.spec().actions().scrolled()
                    .view(newPosition.view)
                    .node_index(newPosition.node_index)
                    .page(newPosition.page)
                    .percent(newPosition.percent)
                    .section(newPosition.section)
                    .time_updated(newPosition.time_updated)
                    .time_spent(newTimeSpent)
                    .time(Timestamp.now())
                    .item_id(mCurrentTrack?.itemId)
                    .url(UrlString(mCurrentTrack?.idUrl))
                    .build()
            )
        }
    }

    /**
     * @return The closest (searching backwards from [.mCurrentPosition]) nodeIndex or 0 if none found or nothing is loaded.
     */
    private val currentNodeIndex: Int
        get() {
            if (isNotReady || !isLoaded) {
                return 0
            }
            for (i in mCurrentPosition downTo 0) {
                val nodeIndex = mUtterances!![i].nodeIndex
                if (nodeIndex != 0) {
                    return nodeIndex
                }
            }
            return 0
        }

    private fun setWakeLockEnabled(enabled: Boolean) {
        if (mWakeLockHolder == null) {
            mWakeLockHolder = WakeLockHolder.withLivelinessCheck(
                "TTSPlayer",
                10, object : LivelinessCheck {
                    private var previous: ListenState? = null
                    override fun keepAlive(): Boolean {
                        // As long as things are changing, we can assume they are still listening
                        // But if we see the listen state is not changing, then something must have gone wrong.
                        val now = App.from(mContext).listen().state()
                        val changed = now != previous
                        previous = now
                        return changed
                    }
                },
                null
            )
        }
        if (DEBUG) Logs.d(TAG, "setWakeLockEnabled $enabled")
        if (enabled) {
            App.from(mContext).wakelocks().acquire(mWakeLockHolder)
        } else {
            App.from(mContext).wakelocks().release(mWakeLockHolder)
        }
    }

    override fun getReadies(): Observable<*> {
        return mReadySubject
    }

    override fun getStartedUtterances(): Observable<Utterance> {
        return mStartedUtterances
    }

    override fun getCompletions(): Observable<*> {
        return mCompletions
    }

    override fun getErrors(): Observable<ListenError> {
        return mErrors
    }

    override fun getProgressUpdates(): Observable<*> {
        return mStartedUtterances
    }

    override fun getBufferingUpdates(): Observable<Float> {
        return Observable.just(0f)
    }

    companion object {
        val DEBUG = BuildConfig.DEBUG && false
        const val TAG = "TTSPlayer"

        /**
         * The amount of silence (in milliseconds) to play before speaking a header node's text
         */
        private const val SILENCE_PRE_HEADER = 444

        /**
         * The amount of silence (in milliseconds) to play after speaking a header node's text
         */
        private const val SILENCE_POST_HEADER = 555L

        /**
         * The amount of silence (in milliseconds) to play after speaking a normal node's text
         */
        private const val SILENCE_POST_NODE = 333L

        /**
         * The amount of silence (in milliseconds) to play at the end of an article to help
         * add a clearer transition between articles.
         */
        private const val SILENCE_END = 4000

        /** The number of utterances to keep queued up in the text to speech engine ahead of the current speaking position.  */
        private const val QUEUED_BUFFER = 3
        private const val EMPTY_QUEUE = -1
    }
}