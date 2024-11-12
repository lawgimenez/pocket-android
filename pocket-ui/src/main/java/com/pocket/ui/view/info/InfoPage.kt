package com.pocket.ui.view.info

import android.view.View
import androidx.annotation.DrawableRes

data class InfoPage @JvmOverloads constructor(
    @DrawableRes
    val imageResId: Int,
    val title: String,
    val text: String,
    val buttonText: String? = null,
    val linkButtonText: String? = null,
    val buttonListener: View.OnClickListener? = null,
    val linkButtonListener: View.OnClickListener? = null,
    val uiEntityIdentifier: String? = null,
)