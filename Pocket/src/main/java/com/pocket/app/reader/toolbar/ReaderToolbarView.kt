package com.pocket.app.reader.toolbar

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.ViewReaderToolbarBinding
import com.pocket.app.auth.AuthenticationActivity
import com.pocket.app.reader.ReaderFragment
import com.pocket.app.share.ShareDialogFragment
import com.pocket.app.tags.ItemsTaggingFragment
import com.pocket.sdk.tts.Listen
import com.pocket.sdk.util.AbsPocketActivity
import com.pocket.ui.view.themed.ThemedConstraintLayout2
import com.pocket.util.collectWhenResumed
import kotlinx.coroutines.flow.SharedFlow

class ReaderToolbarView(
    context: Context,
    attrs: AttributeSet?,
) : ThemedConstraintLayout2(
    context,
    attrs,
) {

    val binding = ViewReaderToolbarBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    fun setupToolbar(
        toolbarEvents: SharedFlow<ReaderToolbar.ToolbarEvent>,
        lifecycleOwner: LifecycleOwner,
        readerFragment: ReaderFragment?,
        listen: Listen,
        url: String,
        toolbarOverflowInteractions: ReaderToolbar.ToolbarOverflowInteractions,
        toolbarInteractions: ReaderToolbar.ToolbarInteractions,
        toolbarUiStateHolder: ReaderToolbar.ToolbarUiStateHolder,
    ) {
        binding.lifecycleOwner = lifecycleOwner
        binding.toolbarInteractions = toolbarInteractions
        binding.toolbarUiStateHolder = toolbarUiStateHolder

        toolbarEvents.collectWhenResumed(lifecycleOwner) { event ->
            when (event) {
                is ReaderToolbar.ToolbarEvent.GoBack -> readerFragment?.onBackPressed()
                is ReaderToolbar.ToolbarEvent.GoToSignIn -> {
                    readerFragment ?: return@collectWhenResumed
                    AuthenticationActivity.startActivity(readerFragment.requireContext(), true)
                }
                is ReaderToolbar.ToolbarEvent.OpenListen -> {
                    event.track?.let { listen.trackedControls(null, null).play(it) }
                    (readerFragment?.activity as? AbsPocketActivity)?.expandListenUi()
                }
                is ReaderToolbar.ToolbarEvent.Share -> {
                    readerFragment ?: return@collectWhenResumed
                    ShareDialogFragment.show(
                        readerFragment.childFragmentManager,
                        url,
                        title = event.title
                    )
                }
                is ReaderToolbar.ToolbarEvent.ShowOverflow -> OverflowBuilder.showOverflow(
                    binding.overflowButton,
                    event.overflowUiState,
                    toolbarOverflowInteractions
                )
                is ReaderToolbar.ToolbarEvent.ShowTagScreen ->
                    ItemsTaggingFragment.show(
                        readerFragment?.activity as? AbsPocketActivity,
                        event.item,
                        null
                    )
                is ReaderToolbar.ToolbarEvent.ShowArticleReportedToast ->
                    Toast.makeText(
                        context,
                        R.string.ts_article_reported,
                        Toast.LENGTH_SHORT
                    ).show()
            }
        }
    }
}