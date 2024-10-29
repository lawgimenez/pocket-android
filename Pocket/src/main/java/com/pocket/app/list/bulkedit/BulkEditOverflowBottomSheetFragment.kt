package com.pocket.app.list.bulkedit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.ideashower.readitlater.databinding.FragBulkEditOverflowBottomSheetBinding
import com.pocket.analytics.api.UiEntityable
import com.pocket.analytics.appevents.SavesTab
import com.pocket.app.tags.ItemsTaggingFragment
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.util.AbsPocketBottomSheetDialogFragment
import com.pocket.util.asFragmentActivity
import com.pocket.util.collectWhenResumed
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BulkEditOverflowBottomSheetFragment : AbsPocketBottomSheetDialogFragment() {

    private val viewModel by viewModels<BulkEditOverflowBottomSheetViewModel>()

    private var _binding: FragBulkEditOverflowBottomSheetBinding? = null
    private val binding: FragBulkEditOverflowBottomSheetBinding
        get() = _binding!!

    private lateinit var items: List<Item>
    private lateinit var savesTab: SavesTab
    private lateinit var onDismiss: () -> Unit

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragBulkEditOverflowBottomSheetBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNavigationEventObserver()
        setupAnalytics()
        viewModel.onInitialized(items, savesTab)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupNavigationEventObserver() {
        viewModel.navigationEvents.collectWhenResumed(viewLifecycleOwner) {
            when (it) {
                BulkEditOverflowNavigationEvent.Close -> {
                    onDismiss.invoke()
                    dismiss()
                }
                BulkEditOverflowNavigationEvent.OpenTagScreen -> {
                    ItemsTaggingFragment.show(
                        context?.asFragmentActivity(),
                        mutableListOf<Item>().apply{
                            addAll(items)
                        },
                        false,
                        null
                    )
                    onDismiss.invoke()
                    dismiss()
                }
            }
        }
    }

    private fun setupAnalytics() {
        binding.favorite.setUiEntityType(UiEntityable.Type.BUTTON)
        binding.addTags.setUiEntityType(UiEntityable.Type.BUTTON)
        binding.markAsViewed.setUiEntityType(UiEntityable.Type.BUTTON)
        binding.markAsNotViewed.setUiEntityType(UiEntityable.Type.BUTTON)
    }

    companion object {
        fun newInstance(
            items: List<Item>,
            savesTab: SavesTab,
            onDismiss: () -> Unit,
        ): BulkEditOverflowBottomSheetFragment {
            return BulkEditOverflowBottomSheetFragment().apply {
                this.items = items
                this.savesTab = savesTab
                this.onDismiss = onDismiss
            }
        }
    }
}