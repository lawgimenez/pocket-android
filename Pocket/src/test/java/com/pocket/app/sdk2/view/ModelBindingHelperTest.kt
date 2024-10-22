package com.pocket.app.sdk2.view

import com.pocket.BaseCoroutineTest
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.api.generated.thing.SearchMatch
import com.pocket.sdk.api.value.HtmlString
import com.pocket.sdk.api.value.UrlString
import com.pocket.sdk2.view.ModelBindingHelper
import com.pocket.util.StringLoader
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelBindingHelperTest : BaseCoroutineTest() {

    @SpyK
    private val stringLoader = mockk<StringLoader>(relaxed = true)

    private lateinit var subject: ModelBindingHelper

    private val testItem = Item.Builder()
        .title("title")
        .domain("domain.com")
        .build()

    @BeforeTest
    fun setup() {
        subject = ModelBindingHelper(
            stringLoader = stringLoader
        )
    }

    @Test
    fun `title when not searching`() {
        assertEquals(
            expected = HtmlString("title"),
            actual = subject.title(
                item = testItem,
                searchMatch = null,
                isSearching = false,
                useLocalHighlights = false,
                searchValue = ""
            )
        )
    }

    @Test
    fun `title WHEN searching AND using local highlights AND there is a search match`() {
        assertEquals(
            expected = HtmlString("t<em>it</em>le"),
            actual = subject.title(
                item = testItem,
                searchMatch = null,
                isSearching = true,
                useLocalHighlights = true,
                searchValue = "it"
            )
        )
    }

    @Test
    fun `title WHEN searching AND using local highlights AND there is not a search match`() {
        assertEquals(
            expected = HtmlString("title"),
            actual = subject.title(
                item = testItem,
                searchMatch = null,
                isSearching = true,
                useLocalHighlights = true,
                searchValue = "meh"
            )
        )
    }

    @Test
    fun `title WHEN searching AND not using local highlights AND item has highlights`() {
        val searchMatch = SearchMatch.Builder()
            .title(HtmlString("title"))
            .build()

        assertEquals(
            expected = HtmlString("title"),
            actual = subject.title(
                item = testItem,
                searchMatch = searchMatch,
                isSearching = true,
                useLocalHighlights = false,
                searchValue = "" // value doesn't matter here
            )
        )
    }

    @Test
    fun `title WHEN searching AND not using local highlights AND item does not have highlights`() {
        assertEquals(
            expected = HtmlString("title"),
            actual = subject.title(
                item = testItem,
                searchMatch = null,
                isSearching = true,
                useLocalHighlights = false,
                searchValue = "" // value doesn't matter here
            )
        )
    }

    @Test
    fun `title WHEN title is null`() {
        val testItem = Item.Builder()
            .build()

        subject.title(
            item = testItem,
            searchMatch = null,
            isSearching = true,
            useLocalHighlights = true,
            searchValue = "test"
        )
    }

    @Test
    fun `domain WHEN not searching`() {
        assertEquals(
            expected = HtmlString("domain.com"),
            actual = subject.domain(
                item = testItem,
                searchMatch = null,
                isSearching = false,
                useLocalHighlights = false,
                searchValue = ""
            )
        )
    }

    @Test
    fun `domain WHEN searching, using local highlights, and domain contains search text`() {
        assertEquals(
            expected = HtmlString("<em>domain</em>.com"),
            actual = subject.domain(
                item = testItem,
                searchMatch = null,
                isSearching = true,
                useLocalHighlights = true,
                searchValue = "domain"
            )
        )
    }

    @Test
    fun `domain WHEN searching, using local highlights, and given url contains search text`() {
        val testItem = Item.Builder()
            .domain("domain")
            .given_url(UrlString("test.com"))
            .build()

        assertEquals(
            expected = HtmlString("<em>domain</em>"),
            actual = subject.domain(
                item = testItem,
                searchMatch = null,
                isSearching = true,
                useLocalHighlights = true,
                searchValue = "test"
            )
        )
    }

    @Test
    fun `domain WHEN searching, has search match value, and domain contains search text`() {
        val searchMatch = SearchMatch.Builder()
            .url(HtmlString("<em>domain</em>.com"))
            .build()

        assertEquals(
            expected = HtmlString("<em>domain</em>.com"),
            actual = subject.domain(
                item = testItem,
                searchMatch = searchMatch,
                isSearching = true,
                useLocalHighlights = false,
                searchValue = "domain"
            )
        )
    }

    @Test
    fun `domain WHEN searching, has search match value, and domain does not contain search text`() {
        val searchMatch = SearchMatch.Builder()
            .url(HtmlString("<em>test</em>.com"))
            .build()

        assertEquals(
            expected = HtmlString("<em>domain.com</em>"),
            actual = subject.domain(
                item = testItem,
                searchMatch = searchMatch,
                isSearching = true,
                useLocalHighlights = false,
                searchValue = "test"
            )
        )
    }

    @Test
    fun `domain WHEN items domain is null`() {
        val testItem = Item.Builder()
            .build()

        subject.domain(
            item = testItem,
            searchMatch = null,
            isSearching = true,
            useLocalHighlights = true,
            searchValue = "test"
        )
    }
}