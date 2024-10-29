package com.pocket.sync.value

import android.text.Spanned
import androidx.core.text.HtmlCompat
import com.pocket.sdk.api.value.HtmlString

object AndroidParser : HtmlString.Parser<Spanned?> {
    override fun parse(htmlString: HtmlString): Spanned {
        return HtmlCompat.fromHtml(htmlString.value, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}
