package com.pocket.app.home.saves.overflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.ideashower.readitlater.databinding.FragRecentSavesOverflowBottomSheetBinding
import com.pocket.app.share.ShareDialogFragment
import com.pocket.app.share.show
import com.pocket.data.models.toDomainItem
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.util.AbsPocketBottomSheetDialogFragment
import com.pocket.util.collectWhenResumed
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecentSavesOverflowFragment : AbsPocketBottomSheetDialogFragment() {

    private val viewModel by viewModels<RecentSaveOverflowViewModel>()

    private var _binding: FragRecentSavesOverflowBottomSheetBinding? = null
    private val binding: FragRecentSavesOverflowBottomSheetBinding
        get() = _binding!!

    private lateinit var item: Item
    private var itemPosition: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragRecentSavesOverflowBottomSheetBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.onInitialized(item, itemPosition)
        setupMarkAsViewed()
        setupEventsObserver()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupEventsObserver() {
        viewModel.events.collectWhenResumed(viewLifecycleOwner) { event ->
            when (event) {
                is RecentSaveOverflowViewModel.Event.ShowShare -> {
                    ShareDialogFragment.show(parentFragmentManager, item.toDomainItem())
                    dismiss()
                }
                is RecentSaveOverflowViewModel.Event.Dismiss -> {
                    dismiss()
                }
            }
        }
    }

    private fun setupMarkAsViewed() {
        if (item.viewed == true) {
            binding.markAsViewedText.text = getString(com.pocket.ui.R.string.ic_mark_as_not_viewed)
            binding.markAsViewedIcon.setImageDrawable(ContextCompat.getDrawable(
                requireContext(),
                com.pocket.ui.R.drawable.ic_viewed_not
            ))
        } else {
            binding.markAsViewedText.text = getString(com.pocket.ui.R.string.ic_mark_as_viewed)
            binding.markAsViewedIcon.setImageDrawable(ContextCompat.getDrawable(
                requireContext(),
                com.pocket.ui.R.drawable.ic_viewed
            ))
        }
    }

    companion object {
        fun newInstance(item: Item, itemPosition: Int): RecentSavesOverflowFragment =
            RecentSavesOverflowFragment().apply {
                this.item = item
                this.itemPosition = itemPosition
            }
    }
}