package com.pocket.app.reader.internal.article

import android.content.Context
import android.util.AttributeSet
import android.view.ActionMode
import android.webkit.WebView
import com.pocket.app.reader.internal.article.textselection.ArticleActionModeCallback
import com.pocket.util.android.Clipboard
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ArticleWebView(
    context: Context,
    attrs: AttributeSet?,
) : WebView(
    context,
    attrs
) {

    @Inject
    lateinit var clipboard: Clipboard

    var onHighlightActionModeClicked: (() -> Unit)? = null
    var onShareActionModeClicked: ((String?) -> Unit)? = null

    override fun startActionMode(callback: ActionMode.Callback): ActionMode {
        return super.startActionMode(ArticleActionModeCallback(
            callback = callback,
            context = context,
            webView = this,
            clipboard = clipboard,
            onHighlightActionModeClicked = onHighlightActionModeClicked,
            onShareActionModeClicked = onShareActionModeClicked,
        ))
    }

    override fun startActionMode(callback: ActionMode.Callback, type: Int): ActionMode {
        return super.startActionMode(ArticleActionModeCallback(
            callback = callback,
            context = context,
            webView = this,
            clipboard = clipboard,
            onHighlightActionModeClicked = onHighlightActionModeClicked,
            onShareActionModeClicked = onShareActionModeClicked,
        ),
            type
        )
    }

    fun executeJS(command: String) {
        evaluateJavascript(command) {}
    }
}