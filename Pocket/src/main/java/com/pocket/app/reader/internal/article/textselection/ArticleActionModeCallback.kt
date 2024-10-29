package com.pocket.app.reader.internal.article.textselection

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.provider.Browser
import android.text.TextUtils
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import com.ideashower.readitlater.R
import com.pocket.util.android.Clipboard
import com.pocket.util.android.IntentUtils
import com.pocket.util.android.WebViewUtil

/**
 * The "ActionMode" is the menu that shows up when you long press text in the webview
 * (or anywhere really, but in this case, it's for the article webview).
 *
 * This class inflates the menu.  It's custom because we need some specific extra actions
 * like the ability create permanent highlights, or to translate text.
 */
class ArticleActionModeCallback(
    private val callback: ActionMode.Callback,
    private val context: Context,
    private val webView: WebView,
    private val clipboard: Clipboard,
    private val onHighlightActionModeClicked: (() -> Unit)?,
    private val onShareActionModeClicked: ((String?) -> Unit)?,
) : ActionMode.Callback2() {

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        setupActionMode(mode, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        setupActionMode(mode, menu)
        return true
    }

    private fun setupActionMode(mode: ActionMode, menu: Menu) {
        menu.clear()
        mode.menuInflater?.inflate(R.menu.reader_text_selection, menu)
        if (!IntentUtils.hasGoogleTranslate(context)) {
            menu.removeItem(R.id.menu_translate)
        }
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_copy -> {
                WebViewUtil.getSelectedText(webView) { selectedText: String? ->
                    clipboard.setText(selectedText!!, null)
                    mode?.finish()
                }
            }
            R.id.menu_share -> {
                WebViewUtil.getSelectedText(webView) { selectedText: String? ->
                    onShareActionModeClicked?.invoke(selectedText)
                    mode?.finish()
                }
            }
            R.id.menu_translate -> {
                WebViewUtil.getSelectedText(webView) { selectedText: String? ->
                    if (!TextUtils.isEmpty(selectedText)) {
                        IntentUtils.googleTranslate(
                            context,
                            selectedText
                        )
                    }
                    mode?.finish()
                }
            }
            R.id.menu_web_search -> {
                WebViewUtil.getSelectedText(webView) { selectedText: String? ->
                    if (selectedText != null) {
                        val i = Intent(Intent.ACTION_WEB_SEARCH)
                            .putExtra(SearchManager.EXTRA_NEW_SEARCH, true)
                            .putExtra(SearchManager.QUERY, selectedText)
                            .putExtra(
                                Browser.EXTRA_APPLICATION_ID,
                                context.packageName
                            )
                        IntentUtils.safeStartActivity(context, i, true)
                    }
                    mode?.finish()
                }
            }
            R.id.menu_select_all -> {
                WebViewUtil.selectAll(webView)
            }
            R.id.menu_highlight -> {
                onHighlightActionModeClicked?.invoke()
                mode?.finish()
            }
            else -> return false
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        // do nothing?
    }

    override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: Rect?) {
        (callback as? ActionMode.Callback2)?.onGetContentRect(mode, view, outRect)
    }
}