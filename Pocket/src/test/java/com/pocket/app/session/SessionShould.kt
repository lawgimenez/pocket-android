package com.pocket.app.session

import com.pocket.sdk.preferences.InMemoryLongPreference
import com.pocket.util.java.Milliseconds
import com.pocket.util.java.MutableClock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class SessionShould {
    private val segment = object : Session.Segment {}
    
    private lateinit var clock: MutableClock
    
    @Before fun setUp() {
        clock = MutableClock(Milliseconds.YEAR)
    }
    
    @Test fun `keep the same id while a segment is open`() {
        val session = Session(Milliseconds.HOUR, InMemoryLongPreference(), InMemoryLongPreference(), clock)
    
        session.startSegment(segment)
        val firstId = session.sid
        clock.time += Milliseconds.YEAR
        val secondId = session.sid
        
        assertThat(secondId).isEqualTo(firstId)
    }
    
    @Test fun `change id when previous session expires`() {
        val expiration = Milliseconds.HOUR
        val session = Session(expiration, InMemoryLongPreference(), InMemoryLongPreference(), clock)
    
        session.startSegment(segment)
        val firstId = session.sid
        clock.time += Milliseconds.MINUTE
        session.softCloseSegment(segment)
        clock.time += expiration
        session.startSegment(segment)
        val secondId = session.sid
        
        assertThat(secondId).isNotEqualTo(firstId)
    }
    
    @Test fun `keep the same id if new segment opens within expiration`() {
        val session = Session(Milliseconds.HOUR, InMemoryLongPreference(), InMemoryLongPreference(), clock)
    
        session.startSegment(segment)
        val firstId = session.sid
        clock.time += Milliseconds.MINUTE
        session.softCloseSegment(segment)
        clock.time += Milliseconds.MINUTE
        session.startSegment(segment)
        val secondId = session.sid
        
        assertThat(secondId).isEqualTo(firstId)
    }
    
    @Test fun `stay open for two overlapping segments`() {
        val session = Session(Milliseconds.HOUR, InMemoryLongPreference(), InMemoryLongPreference(), clock)
    
        val first = object : Session.Segment {}
        session.startSegment(first)
        val firstId = session.sid
        clock.time += Milliseconds.MINUTE
        val second = object : Session.Segment {}
        session.startSegment(second)
        clock.time += Milliseconds.MINUTE
        session.softCloseSegment(first)
        clock.time += Milliseconds.YEAR
        val secondId = session.sid
        
        assertThat(secondId).isEqualTo(firstId)
    }
    
    @Test fun `immediately expire if hard-closed`() {
        val session = Session(Milliseconds.HOUR, InMemoryLongPreference(), InMemoryLongPreference(), clock)
    
        session.startSegment(segment)
        val firstId = session.sid
        clock.time += Milliseconds.MINUTE
        session.hardCloseSegment(segment)
        clock.time += Milliseconds.MINUTE
        session.startSegment(segment)
        val secondId = session.sid
        
        assertThat(secondId).isNotEqualTo(firstId)
    }
    
    @Test fun `remember time spent by the user after closing or pausing`() {
        val session = Session(Milliseconds.HOUR, InMemoryLongPreference(), InMemoryLongPreference(), clock)
        session.enableTimeTracking(InMemoryLongPreference())
    
        session.startSegment(segment)
        val timeSpent = Milliseconds.MINUTE * 13
        clock.time += timeSpent
        session.hardCloseSegment(segment)
        
        assertThat(session.timeSpent.seconds).isEqualTo(Milliseconds.toSeconds(timeSpent))
    }
    
    @Test fun `remember time spent just for active segments (excluding pauses)`() {
        val session = Session(Milliseconds.HOUR, InMemoryLongPreference(), InMemoryLongPreference(), clock)
        session.enableTimeTracking(InMemoryLongPreference())
        
        session.startSegment(segment)
        val firstSegmentDuration = Milliseconds.MINUTE * 13
        clock.time += firstSegmentDuration
        session.softCloseSegment(segment)
        clock.time += Milliseconds.MINUTE * 5
        session.startSegment(segment)
        val secondSegmentDuration = Milliseconds.MINUTE * 7
        clock.time += secondSegmentDuration
        session.softCloseSegment(segment)
        
        assertThat(session.timeSpent.seconds)
            .isEqualTo(Milliseconds.toSeconds(firstSegmentDuration + secondSegmentDuration))
    }
}
