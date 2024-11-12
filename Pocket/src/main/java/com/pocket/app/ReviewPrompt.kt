package com.pocket.app

import android.content.Context
import com.pocket.sdk.Pocket
import com.pocket.sdk.api.generated.action.Pv
import com.pocket.sdk.api.generated.enums.CxtEvent
import com.pocket.sdk.api.generated.enums.CxtView
import com.pocket.sdk.api.generated.enums.PositionType
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk2.analytics.context.Interaction
import com.pocket.util.prefs.BooleanPreference
import com.pocket.util.prefs.LongPreference
import com.pocket.util.prefs.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import org.threeten.bp.Clock
import org.threeten.bp.Instant
import org.threeten.bp.Period
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A simple prompt to ask our users to leave a Play Store review.
 *
 * Spec doc: https://docs.google.com/document/d/1haPtE5_M_lcvgXElEjL0KTnDT7Vdc1yFpltZbDLF0Jw/edit
 */
@Singleton
class ReviewPrompt constructor(
    private val stats: FeatureStats,
    private val mode: AppMode,
    prefs: Preferences,
    private val clock: Clock,
    private val analytics: Analytics,
) {

    @Inject
    constructor(
        stats: FeatureStats,
        mode: AppMode,
        prefs: Preferences,
        clock: Clock,
        pocket: Pocket,
        @ApplicationContext context: Context,
    ) : this(stats, mode, prefs, clock, object : Analytics {
        override fun trackShow() = track()
        override fun trackSubmit() = track(CxtEvent.SUBMIT)
        override fun trackDismiss() = track(CxtEvent.CANCEL)
        private fun track(event: CxtEvent? = null) {
            val interaction = Interaction.on(context)
            val action = Pv.Builder()
                .view(CxtView.REVIEW_PROMPT)
                .context(interaction.context)
                .time(interaction.time)
            if (event != null) {
                action.event(event)
            }
            pocket.sync(null, action.build())
        }
    })

    private val dontShowAgainBefore: LongPreference = prefs.forUser("rateplz_notagain", 0L)
    private val forcePreference: BooleanPreference = prefs.forApp("dcfig_rateplz_frc", false)
    private var justExitedReaderHavingFinishedArticle = false

    companion object {
        private val DONT_SHOW_AGAIN_PERIOD = Period.ofMonths(3)
    }

    /**
     * Call to check if all the conditions are met and we can show the prompt.
     * This should be checked on any screen that the user can come back to after exiting Reader, before this screen
     * is fully resumed, so that the prompt can be shown immediately after exiting Reader.
     */
    fun shouldShow(): Boolean {
        if (mode.isForInternalCompanyOnly && forcePreference.get()) {
            forcePreference.set(false)
            return true
        }

        return justExitedReaderHavingFinishedArticle
                && dontShowAgainBefore.get() < Instant.now(clock).epochSecond
                && stats.getUseCount(FeatureStats.Feature.READER) >= 4
    }

    /** Call when the prompt was successfully shown */
    fun onShow() {
        dontShowAgainBefore.set(
            ZonedDateTime.now(clock)
                .plus(DONT_SHOW_AGAIN_PERIOD)
                .toEpochSecond()
        )
        analytics.trackShow()
    }

    /**
     * Call when the review prompt was attempted. The Google review prompt API does not notify us whether it was actually successfully shown,
     * or whether they've left a review, but assume they have and don't show again.
     */
    fun onReview() {
        // If they agreed to review, don't bother them ever again.
        dontShowAgainBefore.set(Long.MAX_VALUE)
        analytics.trackSubmit()
    }

    /** Call when the user dismissed the prompt */
    fun onDismiss() {
        analytics.trackDismiss()
    }

    /**
     * Call when an error occurs in the Google review prompt request flow.  The API does not give an indication of why an error occurred, but
     * don't attempt another show until our DONT_SHOW_AGAIN_PERIOD elapsed period has occurred.
     */
    fun onReviewPromptError() {
        dontShowAgainBefore.set(
            ZonedDateTime.now(clock)
                .plus(DONT_SHOW_AGAIN_PERIOD)
                .toEpochSecond()
        )
    }

    /** Call when any screen resumes so we know we're not in the "just exited Reader" state any more */
    fun onResumeAnotherScreen() {
        justExitedReaderHavingFinishedArticle = false
    }

    /** Call when exiting Reader so we know user just exited it and so we can check if the item meets criteria */
    fun onExitReader(item: Item?) {
        val positionPercent =
            item?.positions?.get(PositionType.ARTICLE.toString())?.percent ?: item?.positions?.get(
                PositionType.WEB.toString()
            )?.percent ?: 0
        if (positionPercent < 90) return

        justExitedReaderHavingFinishedArticle = true
    }

    fun devPref(): BooleanPreference {
        return forcePreference
    }

    interface Analytics {
        fun trackShow()
        fun trackSubmit()
        fun trackDismiss()
    }
}
