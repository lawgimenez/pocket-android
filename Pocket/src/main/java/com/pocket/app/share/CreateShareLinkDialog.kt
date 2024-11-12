package com.pocket.app.share

import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.ideashower.readitlater.R
import com.pocket.data.models.DomainItem
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Prepares and allows to customize a share link before opening a share sheet. Use this instead of
 * directly opening a share sheet.
 *
 * Currently this is a bare progress dialog that only calls the API to wrap the link in
 * a Pocket Share link.But possible future iterations allow for example adding a note
 * to show when someone opens the link.
 */
@AndroidEntryPoint
class ShareDialogFragment : DialogFragment() {
    private val viewModel by viewModels<CreateShareLinkDialogViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch { setupEventListener() }
                viewModel.onShown(requireArguments().getString(ARG_URL)!!)
            }
        }
        return ProgressDialog(context).apply {
            setTitle(R.string.dg_loading)
        }
    }

    private suspend fun setupEventListener(): Nothing {
        viewModel.events.collect { event ->
            when (event) {
                is CreateShareLinkDialogViewModel.Event.ShowShareSheet -> {
                    ShareSheet.show(
                        context = requireContext(),
                        url = event.originalUrl,
                        shareLink = event.shareLink,
                        quote = arguments?.getString(ARG_QUOTE),
                        title = arguments?.getString(ARG_TITLE),
                    )
                }
                CreateShareLinkDialogViewModel.Event.Dismiss -> dismissAllowingStateLoss()
            }
        }
    }

    companion object {
        private const val ARG_URL = "url"
        private const val ARG_QUOTE = "quote"
        private const val ARG_TITLE = "title"

        fun newInstance(
            url: String,
            quote: String? = null,
            title: String? = null,
        ) = ShareDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_URL, url)
                    putString(ARG_QUOTE, quote)
                    putString(ARG_TITLE, title)
                }
            }

        fun show(
            fragmentManager: FragmentManager,
            url: String,
            quote: String? = null,
            title: String? = null,
        ) {
            newInstance(url, quote, title)
                .show(fragmentManager, ShareDialogFragment::class.simpleName)
        }
    }
}

fun ShareDialogFragment.Companion.show(
    fragmentManager: FragmentManager,
    item: DomainItem,
    quote: String? = null,
) = show(
    fragmentManager,
    url = item.idUrl,
    title = item.displayTitle,
    quote = quote,
)

@HiltViewModel
class CreateShareLinkDialogViewModel
@Inject constructor(
    private val shareRepository: ShareRepository,
) : ViewModel() {

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> get() = _events

    fun onShown(url: String) {
        viewModelScope.launch {
            val shareLink = try {
                shareRepository.createShareLink(url).shareUrl!!.url
            } catch (t: Throwable) {
                // Fall back to the original url if the request fails.
                url
            }
            _events.emit(Event.ShowShareSheet(
                originalUrl = url,
                shareLink = shareLink,
            ))
            _events.emit(Event.Dismiss)
        }
    }

    sealed class Event {
        data class ShowShareSheet(
            val originalUrl: String,
            val shareLink: String,
        ) : Event()
        data object Dismiss : Event()
    }
}
