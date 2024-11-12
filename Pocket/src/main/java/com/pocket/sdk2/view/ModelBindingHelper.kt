package com.pocket.sdk2.view

import android.content.Context
import androidx.annotation.PluralsRes
import com.ideashower.readitlater.R
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.api.generated.thing.SearchMatch
import com.pocket.sdk.api.thing.ItemUtil
import com.pocket.sdk.api.value.HtmlString
import com.pocket.sdk.tts.Track
import com.pocket.util.StringLoader
import org.threeten.bp.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("MagicNumber")
class ModelBindingHelper @Inject constructor(
    private val stringLoader: StringLoader,
) {

    constructor(context: Context): this(StringLoader(context))

    /**
     * Gets a time to read estimate string
     * e.g. "5 min"
     */
    fun timeEstimate(item: Item): String? =
        generateDisplayViewingTimeEstimate(
            ItemUtil.viewingTime(item),
            R.plurals.nm_time_estimate,
            0
        )

    /**
     * Same as above but with a slightly different string
     * e.g. "5 min read"
     */
    fun timeToReadEstimate(item: Item): String? =
        generateDisplayViewingTimeEstimate(
            ItemUtil.viewingTime(item),
            R.plurals.nm_time_to_read_estimate,
            0
        )

    fun timeLeftEstimate(data: Item?): String? =
        generateDisplayViewingTimeEstimate(
            ItemUtil.viewingTime(data),
            R.plurals.nm_time_left_estimate,
            ItemUtil.getPercent(data)
        )

    fun listenDurationEstimate(data: Track?): String? =
        generateDisplayViewingTimeEstimate(
            data?.listenDurationEstimate?.let { Duration.ofSeconds(it.toLong()) },
            R.plurals.nm_time_estimate,
            0,
        )

    private fun generateDisplayViewingTimeEstimate(
        duration: Duration?,
        @PluralsRes pluralString: Int,
        scrolledPercent: Int,
    ): String? {
        if (duration == null) return null
        val minutes: Int = (
                (duration.toMinutes() +
                        if (duration.seconds % 60 >= 30) {
                            1
                        } else {
                            0
                        }
                ) * (100f - scrolledPercent) / 100f).toInt()
        return stringLoader.getQuantityString(pluralString, minutes, minutes)
    }

    fun title(
        item: Item,
        searchMatch: SearchMatch?,
        isSearching: Boolean,
        useLocalHighlights: Boolean,
        searchValue: String
    ): HtmlString =
        if (isSearching) {
            if (useLocalHighlights) {
                highlight(item.display_title ?: "", searchValue)
            } else {
                searchMatch?.title ?: HtmlString(item.display_title ?: "")
            }
        } else {
            HtmlString(item.display_title ?: "")
        }

    // we only get back the highlighted url from the server, but we might display
    // the host name instead
    @Suppress("ComplexMethod")
    fun domain(
        item: Item,
        searchMatch: SearchMatch?,
        isSearching: Boolean,
        useLocalHighlights: Boolean,
        searchValue: String
    ): HtmlString =
        when {
            isSearching
                    && useLocalHighlights
                    && item.display_domain?.contains(searchValue, ignoreCase = true) ?: false ->
                highlight(item.display_domain!!, searchValue)
            // we look at given_url and resolved_url when searching locally.  See the class
            // Deriver.java in the sync-pocket module
            isSearching
                    && useLocalHighlights
                    && (item.given_url?.url?.contains(searchValue, ignoreCase = true) ?: false
                    || item.resolved_url?.url?.contains(searchValue, ignoreCase = true) ?: false) ->
                HtmlString("<em>${item.display_domain ?: ""}</em>")
            isSearching
                    && !searchMatch?.url?.value.isNullOrBlank()
                    && item.display_domain?.contains(searchValue, ignoreCase = true) ?: false ->
                highlight(item.display_domain!!, searchValue)
            isSearching && !searchMatch?.url?.value.isNullOrBlank() ->
                HtmlString("<em>${item.display_domain ?: ""}</em>")
            else -> HtmlString(item.display_domain ?: "")
        }

    private fun highlight(text: String, highlightedText: String): HtmlString =
        if (highlightedText.isNotBlank()
            && text.contains(highlightedText, ignoreCase = true)
        ) {
            val startIndex = text.indexOf(
                string = highlightedText,
                ignoreCase = true
            )
            val substring = text.substring(
                startIndex = startIndex,
                endIndex = startIndex + highlightedText.length
            )
            HtmlString(
                text.replace(
                    oldValue = highlightedText,
                    newValue = "<em>$substring</em>",
                    ignoreCase = true
                )
            )
        } else {
            HtmlString(text)
        }
}
