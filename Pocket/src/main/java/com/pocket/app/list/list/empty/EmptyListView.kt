package com.pocket.app.list.list.empty

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.ideashower.readitlater.databinding.ViewEmptyListBinding
import com.pocket.ui.view.themed.ThemedConstraintLayout2

class EmptyListView(
    context: Context,
    attrs: AttributeSet? = null,
) : ThemedConstraintLayout2(context, attrs) {

    private val binding: ViewEmptyListBinding = ViewEmptyListBinding.inflate(
        LayoutInflater.from(context),
        this,
        true,
    )

    init {
        binding.all.button.setOnClickListener {
            CustomTabsIntent.Builder()
                .build()
                .launchUrl(
                    context,
                    "https://help.getpocket.com/article/885-saving-to-pocket-on-android".toUri()
                )
        }
        binding.tagged.button.setOnClickListener {
            CustomTabsIntent.Builder()
                .build()
                .launchUrl(
                    context,
                    "https://help.getpocket.com/article/926-tagging-in-pocket-for-android".toUri()
                )
        }
        binding.specificTag.button.setOnClickListener {
            CustomTabsIntent.Builder()
                .build()
                .launchUrl(
                    context,
                    "https://help.getpocket.com/article/926-tagging-in-pocket-for-android".toUri()
                )
        }
        binding.archive.button.setOnClickListener {
            CustomTabsIntent.Builder()
                .build()
                .launchUrl(
                    context,
                    "https://help.getpocket.com/article/1150-what-is-the-archive".toUri()
                )
        }
    }

    fun setOnSignedOutButtonClick(listener: OnClickListener?) {
        binding.signedOut.button.setOnClickListener(listener)
    }

    fun setSignedOutEmptyVisible(visible: Boolean) {
        binding.signedOut.root.visibility = if (visible) VISIBLE else GONE
    }

    fun setAllEmptyVisible(visible: Boolean) {
        binding.all.root.visibility = if (visible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    fun setFavoriteEmptyVisible(visible: Boolean) {
        binding.favorite.root.visibility = if (visible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    fun setTaggedEmptyVisible(visible: Boolean) {
        binding.tagged.root.visibility = if (visible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    fun setSpecificTagEmptyVisible(visible: Boolean) {
        binding.specificTag.root.visibility = if (visible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    fun setHighlightedEmptyVisible(visible: Boolean) {
        binding.highlights.root.visibility = if (visible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    fun setArchiveEmptyVisible(visible: Boolean) {
        binding.archive.root.visibility = if (visible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    fun setSearchEmptyVisible(visible: Boolean) {
        binding.search.root.visibility = if (visible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}
