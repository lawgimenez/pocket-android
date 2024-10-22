package com.pocket.app.reader.internal.article.highlights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.ideashower.readitlater.databinding.FragmentHighlightsBottomSheetBinding
import com.pocket.app.share.ShareDialogFragment
import com.pocket.sdk.util.AbsPocketBottomSheetDialogFragment
import com.pocket.util.collectWhenResumed
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HighlightsBottomSheetFragment : AbsPocketBottomSheetDialogFragment() {

    private val viewModel: HighlightsBottomSheetViewModel by viewModels()

    private var _binding: FragmentHighlightsBottomSheetBinding? = null
    private val binding: FragmentHighlightsBottomSheetBinding
        get() = _binding!!

    lateinit var url: String
    lateinit var onHighlightClicked: (highlightId: String) -> Unit
    lateinit var onHighlightDeleted: () -> Unit

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHighlightsBottomSheetBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEventListener()
        setupRecyclerView()
        viewModel.onInitialized(url)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupEventListener() {
        viewModel.events.collectWhenResumed(viewLifecycleOwner) { event ->
            when (event) {
                is HighlightsBottomSheet.Event.Dismiss -> {
                    event.scrollToId?.let { onHighlightClicked.invoke(it) }
                    dismiss()
                }
                is HighlightsBottomSheet.Event.ShowShare -> {
                    ShareDialogFragment.show(
                        childFragmentManager,
                        url = url,
                        quote = event.text
                    )
                }
                is HighlightsBottomSheet.Event.RemoveHighlightFromWebView -> {
                    onHighlightDeleted.invoke()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.highlightList.addItemDecoration(HighlightSpacingDecorator())
        binding.highlightList.adapter = HighlightsAdapter(viewLifecycleOwner, viewModel)
    }

    companion object {
        fun newInstance(
            url: String,
            onHighlightClicked: (highlightId: String) -> Unit,
            onHighlightDeleted: () -> Unit,
        ): HighlightsBottomSheetFragment =
            HighlightsBottomSheetFragment().apply {
                this.url = url
                this.onHighlightClicked = onHighlightClicked
                this.onHighlightDeleted = onHighlightDeleted
            }
    }
}