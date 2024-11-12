package com.pocket.sdk.tts

import android.content.Context
import com.pocket.app.AppThreads
import com.pocket.sdk.Pocket
import com.pocket.util.prefs.FloatPreference

/**
 * Engine can create a [ListenPlayer] and answer some meta questions about the player,
 * like do we need to recreate the player or what the player can do.
 */
internal interface ListenEngine {
    /** @return true if we can continue to use the [player], false if we need to create a new one */
    fun isValid(player: ListenPlayer?): Boolean
    
    /** @return a new player instance */
    fun createPlayer(
        context: Context,
        pocket: Pocket,
        threads: AppThreads,
        android: ListenMediaSession,
        initialSpeed: Float,
        lowestReportedFailingSpeed: FloatPreference
    ): ListenPlayer
    
    /** @return extra features supported by players created with this engine */
    fun supportedFeatures(): Array<ListenState.Feature>
    
    object Streaming : ListenEngine {
        val features = arrayOf(
            ListenState.Feature.ACCURATE_DURATION_AND_ELAPSED,
            ListenState.Feature.PRELOADING,
        )

        override fun isValid(player: ListenPlayer?) = player is GetItemAudioPlayer
        
        override fun createPlayer(
            context: Context,
            pocket: Pocket,
            threads: AppThreads,
            android: ListenMediaSession,
            initialSpeed: Float,
            lowestReportedFailingSpeed: FloatPreference
        ): ListenPlayer {
            return GetItemAudioPlayer(
                context,
                pocket,
                threads,
                android,
                initialSpeed,
                lowestReportedFailingSpeed
            )
        }
        
        override fun supportedFeatures() = features
    }
    
    object Tts : ListenEngine {
        private val features = arrayOf(ListenState.Feature.MULTIPLE_VOICES)
    
        override fun isValid(player: ListenPlayer?) = player is TTSPlayer
        
        override fun createPlayer(
            context: Context,
            pocket: Pocket,
            threads: AppThreads,
            android: ListenMediaSession,
            initialSpeed: Float,
            lowestReportedFailingSpeed: FloatPreference
        ): ListenPlayer {
            return TTSPlayer(context)
        }
    
        override fun supportedFeatures() = features
    }
}
