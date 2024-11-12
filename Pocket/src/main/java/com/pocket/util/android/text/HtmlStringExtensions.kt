package com.pocket.util.android.text

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.pocket.app.settings.Theme
import com.pocket.sdk.api.value.HtmlString
import com.pocket.sync.value.AndroidParser
import com.pocket.ui.R

fun HtmlString.toHighlightedSpannableString(
    @ColorInt highlightColor: Int
): CharSequence {
    val builder = SpannableStringBuilder(HtmlString.parsed(this, AndroidParser))
    val spans = builder.getSpans(0, builder.length, StyleSpan::class.java)
    for (oldSpan in spans) {
        val style = oldSpan.style
        if (style == Typeface.ITALIC || style == Typeface.BOLD_ITALIC) {
            // This is likely our server's <em> tag.
            val newSpan = BackgroundColorSpan(highlightColor)
            builder.setSpan(
                newSpan,
                builder.getSpanStart(oldSpan),
                builder.getSpanEnd(oldSpan),
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            builder.removeSpan(oldSpan)
        }
    }
    return builder
}

fun HtmlString.toTealHighlightedSpannableString(
    theme: Theme,
    context: Context,
): CharSequence = toHighlightedSpannableString(
    if (theme.isDark(context)) {
        ContextCompat.getColor(context, R.color.pkt_dm_teal_1)
    } else {
        ContextCompat.getColor(context, R.color.pkt_teal_5)
    }
)