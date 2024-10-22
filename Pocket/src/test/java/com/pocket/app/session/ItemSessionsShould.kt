package com.pocket.app.session

import com.pocket.app.AppMode
import com.pocket.sdk.api.generated.enums.ItemSessionTriggerEvent
import com.pocket.sdk.api.generated.thing.ActionContext
import com.pocket.util.java.Clock
import com.pocket.util.prefs.MemoryPrefStore
import com.pocket.util.prefs.Prefs
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.verifyZeroInteractions

class ItemSessionsShould {
    private val analytics = mock(ItemSessions.Analytics::class.java)
    private val itemSessions = ItemSessions(AppMode.PRODUCTION,
            analytics,
            Clock.SYSTEM,
            Prefs(MemoryPrefStore(), MemoryPrefStore()))

    
    @Test fun `fire start action when a new segment started`() {
        // given
        val url = "some.url"
        val id = "some-id"
        val startEvent = ItemSessionTriggerEvent.OPENED_WEB
        
        // when
        itemSessions.startSegment(object : Session.Segment {}, url, id, startEvent, UI_CONTEXT)
        
        // then
        verify(analytics).fireStartAction(anyLong(), eq(url), eq(id), eq(startEvent), eq(UI_CONTEXT))
        verifyNoMoreInteractions(analytics)
    }
    
    @Test fun `fire end action when the only segment closed`() {
        // given
        val segment = object : Session.Segment {}
        val url = "some.url"
        val id = "some-id"
        val endEvent = ItemSessionTriggerEvent.CLOSED_READER
        itemSessions.startSegment(segment, url, id, ItemSessionTriggerEvent.OPENED_WEB, UI_CONTEXT)
        
        // when
        itemSessions.hardCloseSegment(segment, url, id, endEvent, UI_CONTEXT)
        
        // then
        verify(analytics).fireEndAction(anyLong(), eq(url), eq(id), eq(endEvent), any(), eq(UI_CONTEXT))
    }
    
    @Test fun `fire pause and continue actions when segment soft closed and restarted`() {
        // given
        val segment = object : Session.Segment {}
        val url = "some.url"
        val id = "some-id"
        val pauseEvent = ItemSessionTriggerEvent.CLOSED_READER
        val restartEvent = ItemSessionTriggerEvent.OPENED_WEB
    
        // when
        itemSessions.startSegment(segment, url, id, ItemSessionTriggerEvent.OPENED_WEB, UI_CONTEXT)
        itemSessions.softCloseSegment(segment, url, id, pauseEvent, UI_CONTEXT)
        itemSessions.startSegment(segment, url, id, restartEvent, UI_CONTEXT)
    
        // then
        verify(analytics).firePauseAction(anyLong(), eq(url), eq(id), eq(pauseEvent), any(), eq(UI_CONTEXT))
        verify(analytics).fireContinueAction(anyLong(), eq(url), eq(id), eq(restartEvent), eq(UI_CONTEXT))
    }
    
    @Test fun `fire actions correctly when multiple segments started and stopped`() {
        val url = "some.url"
        val id = "some-id"
        val startEvent = ItemSessionTriggerEvent.OPENED_WEB
        val endEvent = ItemSessionTriggerEvent.REACH_END_LISTEN
        
        val segment1 = object : Session.Segment {}
        itemSessions.startSegment(segment1, url, id, startEvent, UI_CONTEXT)
        verify(analytics).fireStartAction(anyLong(), eq(url), eq(id), eq(startEvent), eq(UI_CONTEXT))
    
        val segment2 = object : Session.Segment {}
        itemSessions.startSegment(segment2, url, id, ItemSessionTriggerEvent.START_LISTEN, UI_CONTEXT)
        verifyZeroInteractions(analytics)
        
        itemSessions.hardCloseSegment(segment1, url, id, ItemSessionTriggerEvent.CLOSED_READER, UI_CONTEXT)
        verifyZeroInteractions(analytics)
    
        itemSessions.hardCloseSegment(segment2, url, id, endEvent, UI_CONTEXT)
        verify(analytics).fireEndAction(anyLong(), eq(url), eq(id), eq(endEvent), any(), eq(UI_CONTEXT))
        verifyNoMoreInteractions(analytics)
    }
    
    @Test fun `pause a reading session when user leaves the app`() {
        // given
        val url = "some.url"
        val startEvent = ItemSessionTriggerEvent.OPENED_WEB
        itemSessions.startSegment(ItemSessions.READING_SEGMENT, url, "some-id", startEvent, UI_CONTEXT)
    
        // when
        itemSessions.onUserGone(null)
    
        // then
        verify(analytics)
            .firePauseAction(anyLong(), eq(url), any(), eq(ItemSessionTriggerEvent.CLOSED_APP), any(), any())
    }
    
    @Test fun `end a listening session when a reading session is started`() {
        // given
        val url = "listening-url"
        val startEvent: ItemSessionTriggerEvent = ItemSessionTriggerEvent.START_LISTEN
        itemSessions.startSegment(ItemSessions.LISTENING_SEGMENT, url, "listening-id", startEvent, UI_CONTEXT)
        
        // when
        val triggerEvent = ItemSessionTriggerEvent.OPENED_ARTICLE
        itemSessions.startSegment(ItemSessions.READING_SEGMENT, "reading-url", "reading-id", triggerEvent, UI_CONTEXT)
        
        // then
        verify(analytics).fireEndAction(anyLong(), eq(url), any(), eq(triggerEvent), any(), any())
    }
    
    @Test fun `return non null id for an active session`() {
        // given
        val url = "some.url"
    
        // when
        val startEvent = ItemSessionTriggerEvent.OPENED_WEB
        itemSessions.startSegment(object : Session.Segment {}, url, "some-id", startEvent, UI_CONTEXT)
    
        // then
        assertThat(itemSessions.getSessionId(url)).isNotNull()
    }
    
    @Test fun `return null id for a closed session`() {
        // given
        val url = "some.url"
    
        // when
        val segment = object : Session.Segment {}
        val startEvent = ItemSessionTriggerEvent.OPENED_WEB
        itemSessions.startSegment(segment, url, "some-id", startEvent, UI_CONTEXT)
        itemSessions.hardCloseSegment(segment, url, "some-id", ItemSessionTriggerEvent.ERROR, UI_CONTEXT)
    
        // then
        assertThat(itemSessions.getSessionId(url)).isNull()
    }
    
    @Test fun `return null id for a wrong url`() {
        // given
        val url1 = "some.url"
        val url2 = "different.url"
        assertThat(url1).isNotEqualTo(url2)
    
        // when
        val segment = object : Session.Segment {}
        val startEvent = ItemSessionTriggerEvent.OPENED_WEB
        itemSessions.startSegment(segment, url1, "some-id", startEvent, UI_CONTEXT)
    
        // then
        assertThat(itemSessions.getSessionId(url2)).isNull()
    }
    
    companion object {
        val UI_CONTEXT: ActionContext = ActionContext.Builder().build()
    }
}
