package com.pocket.sdk.util

import android.content.Context
import android.widget.TextView
import com.pocket.sdk.api.value.MarkdownString
import com.pocket.ui.text.CustomTypefaceSpan
import com.pocket.ui.text.Fonts
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonSpansFactory
import org.commonmark.node.Emphasis
import org.commonmark.node.StrongEmphasis

class MarkdownHandler(
    context: Context,
    onLinkClicked: (link: String) -> Unit,
) {

    private val markwon = Markwon.builder(context)
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                builder
                    .setFactory(Emphasis::class.java) {_, _ ->
                        CustomTypefaceSpan(Fonts.get(context, Fonts.Font.GRAPHIK_LCG_REGULAR_ITALIC))
                    }
                    .setFactory(StrongEmphasis::class.java) {_, _ ->
                        CustomTypefaceSpan(Fonts.get(context, Fonts.Font.GRAPHIK_LCG_MEDIUM))
                    }
            }

            override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                builder.linkResolver { _, link ->
                    onLinkClicked(link)
                }
            }
        })
        .build()

    private val parser = MarkdownString.Parser { mdString ->
        val node = markwon.parse(mdString.value)
        markwon.render(node)
    }

    fun TextView.setMarkdownString(markdownString: MarkdownString) {
        markwon.setParsedMarkdown(this, markdownString.parsed(parser))
    }
}