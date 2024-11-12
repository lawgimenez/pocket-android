package com.pocket.app

import com.pocket.sdk.api.generated.enums.PositionType
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.api.generated.thing.Position
import com.pocket.sdk.api.value.UrlString
import com.pocket.util.prefs.LongPreference
import com.pocket.util.prefs.MemoryPrefStore
import com.pocket.util.prefs.Prefs
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.threeten.bp.*
import java.util.*

class ReviewPromptShould {
    private lateinit var prompt: ReviewPrompt
    
    private lateinit var prefs: Prefs
    private lateinit var stats: FeatureStats
    private lateinit var dontShowAgainBefore: LongPreference
    private lateinit var clock: MutableClock
    
    @Before fun setUp() {
        prefs = Prefs(MemoryPrefStore(), MemoryPrefStore())
        stats = FeatureStats(prefs)
        dontShowAgainBefore = prefs.forApp("dont_show_again_before", 0L)
        clock = MutableClock()

        prompt = ReviewPrompt(
            stats,
            AppMode.PRODUCTION,
            prefs,
            clock,
            object : ReviewPrompt.Analytics {
                override fun trackShow() {} 
                override fun trackSubmit() {} 
                override fun trackDismiss() {} 
            }
        )
    }
    
    @Test fun `not show by default`() {
        assertThat(prompt.shouldShow()).isFalse()
    }
    
    @Test fun `show when criteria met`() {
        // when
        prepareSuccessfulConditions()
        
        // then
        assertThat(prompt.shouldShow()).isTrue()
    }
    
    @Test fun `not show when not immediately after closing reader`() {
        // when
        prepareSuccessfulConditions()
        prompt.onResumeAnotherScreen()
        
        // then
        assertThat(prompt.shouldShow()).isFalse()
    }
    
    @Test fun `not show when item not scrolled enough`() {
        // when
        prepareSuccessfulConditions()
        prompt.onResumeAnotherScreen()
        prompt.onExitReader(mockItemWithPosition(89))
    
        // then
        assertThat(prompt.shouldShow()).isFalse()
    }
    
    @Test fun `not show if reader not used enough`() {
        // when
        prepareSuccessfulConditions()
        prefs.clear() // Reset usage back to zero
        trackUse(3)

        // then
        assertThat(prompt.shouldShow()).isFalse()
    }
    
    @Test fun `not show if prompted recently`() {
        // when
        prepareSuccessfulConditions()
        clock.set { minusSeconds(1) }
    
        // then
        assertThat(prompt.shouldShow()).isFalse()
    }
    
    @Test fun `show only after more than 3 months passed since last time`() {
        // when
        prepareSuccessfulConditions()
        prompt.onShow()
        
        // then
        assertThat(prompt.shouldShow()).isFalse()
        
        clock.set { plusMonths(3) }
        assertThat(prompt.shouldShow()).isFalse()
        
        clock.set { plusSeconds(1) }
        assertThat(prompt.shouldShow()).isTrue()
    }
    
    @Test fun `should not show again for a very long time after review`() {
        // when
        prepareSuccessfulConditions()
        prompt.onShow()
        prompt.onReview()
        
        // then
        clock.set { plusYears(10) }
        assertThat(prompt.shouldShow()).isFalse()
    }

    private fun trackUse(times: Int) {
        repeat(times) {
            stats.trackUse(FeatureStats.Feature.READER)
        }
    }
    
    private fun prepareSuccessfulConditions() {
        val instant = Instant.EPOCH
        dontShowAgainBefore.set(instant.epochSecond)
        clock.setTime(instant.plusSeconds(1))
        trackUse(4)
        prompt.onExitReader(mockItemWithPosition(90))
    }
    
    private fun mockItemWithPosition(percent: Int): Item? {
        return Item.Builder()
                    .given_url(UrlString("url"))
                    .positions(Collections.singletonMap(
                            PositionType.ARTICLE.toString(),
                            Position.Builder().view(PositionType.ARTICLE).percent(percent).build()))
                    .build()
    }
}

class MutableClock : Clock() {
    private var clock = fixed(Instant.MIN)
    
    private fun fixed(instant: Instant) = fixed(instant, ZoneId.ofOffset("", ZoneOffset.MAX))
    
    fun setTime(instant: Instant) {
        clock = fixed(instant)
    }
    
    inline fun set(block: ZonedDateTime.() -> ZonedDateTime) {
        setTime(ZonedDateTime.ofInstant(instant(), zone).block().toInstant())
    }
    
    override fun withZone(zone: ZoneId?): Clock? = clock.withZone(zone)
    override fun getZone(): ZoneId? = clock.zone
    override fun instant(): Instant = clock.instant()
}
