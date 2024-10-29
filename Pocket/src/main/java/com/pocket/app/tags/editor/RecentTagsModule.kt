package com.pocket.app.tags.editor

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.ideashower.readitlater.databinding.ViewRecentTagsModuleBinding
import com.pocket.sdk.Pocket
import com.pocket.sdk.api.generated.thing.Tags
import com.pocket.util.android.setTextOrHide

class RecentTagsModule(
    private val pocket: Pocket,
    manager: TagModuleManager?,
    visibilityListener: VisibilityListener?,
    context: Context?,
) : TagModule(manager, visibilityListener, context) {

    private val views = ViewRecentTagsModuleBinding.inflate(LayoutInflater.from(context)).apply {
        root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        tag1.setTagAddingClickListener()
        tag2.setTagAddingClickListener()
        tag3.setTagAddingClickListener()
    }
    private fun TextView.setTagAddingClickListener() {
        setOnClickListener { 
            val tag = text.toString()
            manager.addTag(this@RecentTagsModule, tag)
            onTagAdded(tag)
        }
    }
    override fun getView() = views.root

    private val recentTags = mutableListOf<String>()

    override fun load(callback: TagModuleLoadedListener?) {
        pocket.sync(pocket.spec().things().tags().build())
            .onSuccess { tags: Tags? ->
                recentTags.clear()
                tags?.recentlyUsed
                    ?.mapNotNull { it.tag }
                    ?.let { recentTags.addAll(it) }
                views.update()

                callback?.onTagModuleLoaded()
            }
            .onFailure { throw RuntimeException(it) }
    }

    override fun onTagInputTextChanged(text: CharSequence?) {
        // Hide if the user is adding a tag manually.
        setPreferredVisibility(text.isNullOrEmpty() && recentTags.isNotEmpty())
    }

    override fun onTagAdded(tag: String?) {
        recentTags.remove(tag)
        views.update()
    }

    override fun onTagRemoved(tag: String?) {
        // If they picked a tag, and then removed it, we probably don't need to suggest it again.
    }

    private fun ViewRecentTagsModuleBinding.update() {
        setPreferredVisibility(recentTags.isNotEmpty())
        
        root.visibility = View.VISIBLE
        views.tag1.setTextOrHide(recentTags.getOrNull(0))
        views.tag2.setTextOrHide(recentTags.getOrNull(1))
        views.tag3.setTextOrHide(recentTags.getOrNull(2))

        views.divider1.visibility = views.tag2.visibility
        views.divider2.visibility = views.tag3.visibility
    }
}
