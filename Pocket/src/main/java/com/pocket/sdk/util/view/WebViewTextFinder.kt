package com.pocket.sdk.util.view

import android.content.Context
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.widget.EditText
import android.widget.TextView
import com.ideashower.readitlater.R
import com.pocket.ui.view.edittext.TextFinderLayout
import com.pocket.util.android.SimpleTextWatcher
import com.pocket.util.android.ViewUtil

class WebViewTextFinder(
    private val webView: WebView,
    textFinderLayout: TextFinderLayout,
) {

    private val context: Context = webView.context
    private val root: View = textFinderLayout.root()
    private val cancel: View = textFinderLayout.cancel()
    private val input: EditText = textFinderLayout.input()
    private val count: TextView = textFinderLayout.count()
    private val back: View = textFinderLayout.back()
    private val forward: View = textFinderLayout.forward()

    init {
        webView.setFindListener { active, matches, _ ->
            val realActive = if (matches == 0) 0 else active + 1 // for some reason active is 1 when matches is 0
            count.text = context.getString(com.pocket.ui.R.string.quantity_count, realActive, matches)
        }
        back.setOnClickListener { back() }
        forward.setOnClickListener { forward() }
        cancel.setOnClickListener { close() }
        input.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                webView.findAllAsync(s.toString())
            }
        })
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.maxLines = 1
        input.imeOptions = EditorInfo.IME_ACTION_DONE
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                ViewUtil.forceSoftKeyboard(false, input)
            }
            true
        }
        input.setHint(R.string.lb_search_hint)
        root.visibility = View.GONE
    }

    fun isOpen(): Boolean {
        return root.visibility == View.VISIBLE
    }

    fun open() {
        input.text = null
        root.visibility = View.VISIBLE
        root.postDelayed({ViewUtil.forceFocus(true, input)}, 200)
    }

    fun close() {
        webView.clearMatches()
        root.visibility = View.GONE
        ViewUtil.forceSoftKeyboard(false, input)
    }

    private fun back() {
        ViewUtil.forceSoftKeyboard(false, input)
        webView.findNext(false)
    }

    private fun forward() {
        ViewUtil.forceSoftKeyboard(false, input)
        webView.findNext(true)
    }
}