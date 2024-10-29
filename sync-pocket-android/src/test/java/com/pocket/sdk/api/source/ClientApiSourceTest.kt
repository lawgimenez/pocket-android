package com.pocket.sdk.api.source;

import com.pocket.sdk.AbsPocketTest
import com.pocket.sdk.api.generated.thing.*
import org.junit.Test
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.test.Ignore
import kotlin.test.assertNotNull

class ClientApiSourceTest : AbsPocketTest() {

    @Ignore("The test went SPLOINK when security made us delete our old test accounts. We could switch to another account, but really, the test shouldn't hit the live production API.")
    @Test
    fun getSlateLineup() {
        val get = newPocket().pocket.sync(GetSlateLineup.Builder()
                .slateLineupId("af3a5196-be16-4bf7-b131-70981e43b132")
                .slateCount(4)
                .recommendationCount(8)
                .build())
                .get()
        assertLineup(get.lineup)
        assert(get.lineup?.slates?.size == 4)
        val firstSlate = assertSlate(get.lineup?.slates?.get(0))
        assert(firstSlate.recommendations?.size == 8)
        val firstRec = assertRec(firstSlate.recommendations?.get(0))
        assertItem(firstRec.item)
    }

    @Ignore("The test went SPLOINK when security made us delete our old test accounts. We could switch to another account, but really, the test shouldn't hit the live production API.")
    @Test
    fun getSlate() {
        val get = newPocket().pocket.sync(GetSlate.Builder()
                .slateId("2e3ddc90-8def-46d7-b85f-da7525c66fb1")
                .recommendationCount(8)
                .build())
                .get()
        val slate = assertSlate(get.slate)
        assert(slate.recommendations?.size == 8)
        val firstRec = assertRec(slate.recommendations?.get(0))
        assertItem(firstRec.item)
    }

    private fun assertLineup(lineup: SlateLineup?) {
        val declared = assertNotNull(lineup?.declared)
        assert(declared.experimentId)
        assert(declared.id)
        assert(declared.requestId)
        assert(declared.slates)
    }

    private fun assertSlate(slate: Slate?): Slate {
        assertNotNull(slate)
        val declared = assertNotNull(slate.declared)
        assert(declared.description)
        assert(declared.displayName)
        assert(declared.experimentId)
        assert(declared.id)
        assert(declared.recommendations)
        assert(declared.requestId)
        return slate
    }

    private fun assertRec(rec: Recommendation?): Recommendation {
        assertNotNull(rec)
        val declared = assertNotNull(rec.declared)
        assert(declared.curatedInfo)
        assert(declared.display_excerpt)
        assert(declared.display_thumbnail)
        assert(declared.display_title)
        assert(declared.id)
        assert(declared.item)
        return rec
    }

    private fun assertItem(item: Item?) {
        assertNotNull(item)
        println(item.toString())
        val declared = item.declared
        // the list of non deprecated fields currently supported by ParserServiceSlice.Item
        assert(declared.item_id)
        assert(declared.normal_url)
        assert(declared.resolved_url)
        assert(declared.amp_url)
        assert(declared.resolved_id)
        assert(declared.mime_type)
        assert(declared.encoding)
        assert(declared.title)
        assert(declared.excerpt)
        assert(declared.word_count)
        assert(declared.has_image)
        assert(declared.has_video)
        assert(declared.is_article)
        assert(declared.authors)
        assert(declared.images)
        assert(declared.videos)
        assert(declared.top_image_url)
        assert(declared.given_url)
        assert(declared.domain)
        assert(declared.domain_metadata)
    }
}