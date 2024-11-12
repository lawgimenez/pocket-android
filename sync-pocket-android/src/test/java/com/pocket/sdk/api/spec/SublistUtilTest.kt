package com.pocket.sdk.api.spec

import com.pocket.sdk.api.generated.thing.Get
import com.pocket.sync.space.Space
import org.junit.Test
import org.mockito.Mockito.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SublistUtilTest {
    @Test fun `applyOffsetCount() offset + count within list size`() {
        // given
        val list = (1..30).toList()

        // when
        val sublist = SublistUtil.applyOffsetCount(list, 10, 10)

        // then
        assertEquals((11..20).toList(), sublist)
    }

    @Test fun `applyOffsetCount() offset + count exceeds list size`() {
        // given
        val list = (1..15).toList()

        // when
        val sublist = SublistUtil.applyOffsetCount(list, 10, 10)

        // then
        assertEquals((11..15).toList(), sublist)
    }

    @Test fun `applyOffsetCount() offset exceeds list size`() {
        // given
        val list = (1..5).toList()

        // when
        val sublist = SublistUtil.applyOffsetCount(list, 10, 10)

        // then
        assertEquals(emptyList(), sublist)
    }

    @Test fun `applyOffsetCount() count is 0`() {
        // given
        val list = (1..30).toList()

        // when
        val sublist = SublistUtil.applyOffsetCount(list, 10, 0)

        // then
        assertEquals(emptyList(), sublist)
    }

    @Test fun `applyOffsetCount() offset is equal to list size`() {
        // given
        val list = (1..10).toList()

        // when
        val sublist = SublistUtil.applyOffsetCount(list, 10, 10)

        // then
        assertEquals(emptyList(), sublist)
    }

    @Test fun `applyOffsetCount() show only the last element`() {
        // given
        val list = (1..11).toList()

        // when
        val sublist = SublistUtil.applyOffsetCount(list, 10, 10)

        // then
        assertEquals(listOf(11), sublist)
    }

    @Test fun `hasPreviousPages() complete`() {
        // given
        val p1 = Get.Builder().offset(0).count(30).build()
        val p2 = Get.Builder().offset(30).count(30).build()
        val p3 = Get.Builder().offset(60).count(30).build()
        val p4 = Get.Builder().offset(90).count(30).build()
        val space = mock(Space.Selector::class.java)
        `when`(space.contains(p1)).then { booleanArrayOf(true) }
        `when`(space.contains(p2)).then { booleanArrayOf(true) }
        `when`(space.contains(p3)).then { booleanArrayOf(true) }

        // when
        val result = SublistUtil.hasPreviousPages(p4, space)

        // then
        assertTrue(result)
        verify(space).contains(p1)
        verify(space).contains(p2)
        verify(space).contains(p3)
    }

    @Test fun `hasPreviousPages() incomplete`() {
        // given
        val p1 = Get.Builder().offset(0).count(30).build()
        val p2 = Get.Builder().offset(30).count(30).build()
        val p3 = Get.Builder().offset(60).count(30).build()
        val p4 = Get.Builder().offset(90).count(30).build()
        val space = mock(Space.Selector::class.java)
        `when`(space.contains(p1)).then { booleanArrayOf(false) }
        `when`(space.contains(p2)).then { booleanArrayOf(true) }
        `when`(space.contains(p3)).then { booleanArrayOf(true) }

        // when
        val result = SublistUtil.hasPreviousPages(p4, space)

        // then
        assertFalse(result)
        verify(space).contains(p1)
        verify(space).contains(p2)
        verify(space).contains(p3)
    }

    @Test fun `hasPreviousPages() unsupported`() {
        // given
        val p4 = Get.Builder().offset(50).count(30).build()
        val space = mock(Space.Selector::class.java)

        // when
        val result = SublistUtil.hasPreviousPages(p4, space)

        // then
        assertFalse(result)
    }
}