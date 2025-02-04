package com.pocket.sdk.util

import android.content.Context
import com.pocket.ui.text.CustomTypefaceSpan
import com.pocket.ui.text.Fonts
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonSpansFactory
import org.commonmark.node.Emphasis
import org.commonmark.node.StrongEmphasis

class MarkdownFormatter(
    context: Context,
    onLinkClicked: (context: Context, link: String) -> Unit,
) {

    private val markwon = Markwon.builder(context)
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                builder
                    .setFactory(Emphasis::class.java) { _, _ ->
                        CustomTypefaceSpan(Fonts.get(context, Fonts.Font.GRAPHIK_LCG_REGULAR_ITALIC))
                    }
                    .setFactory(StrongEmphasis::class.java) { _, _ ->
                        CustomTypefaceSpan(Fonts.get(context, Fonts.Font.GRAPHIK_LCG_MEDIUM))
                    }
            }

            override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                builder.linkResolver { view, link ->
                    onLinkClicked(view.context, link)
                }
            }
        })
        .build()

    fun format(markdown: String) = markwon.toMarkdown(markdown)
}
