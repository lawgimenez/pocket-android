package com.pocket.app.list.list.overflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.ideashower.readitlater.databinding.FragItemOverflowBottomSheetBinding
import com.pocket.analytics.api.UiEntityable
import com.pocket.analytics.appevents.SavesTab
import com.pocket.app.PocketSingleton
import com.pocket.app.tags.ItemsTaggingFragment
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.offline.cache.AssetUser
import com.pocket.sdk.util.AbsPocketBottomSheetDialogFragment
import com.pocket.sdk2.view.LazyAssetBitmap
import com.pocket.sync.value.putThing
import com.pocket.sync.value.thingArg
import com.pocket.ui.util.LazyBitmapDrawable
import com.pocket.util.android.enumArg
import com.pocket.util.android.putEnum
import com.pocket.util.asFragmentActivity
import com.pocket.util.collectWhenResumed
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ItemOverflowBottomSheetFragment : AbsPocketBottomSheetDialogFragment() {

    @Inject
    lateinit var pocketSingleton: PocketSingleton

    private val viewModel by viewModels<ItemOverflowBottomSheetViewModel>()

    private var _binding: FragItemOverflowBottomSheetBinding? = null
    private val binding: FragItemOverflowBottomSheetBinding
        get() = _binding!!

    private val savesTab by enumArg<SavesTab>(ARG_SAVES_TAB)
    private val item by thingArg(ARG_ITEM, Item.JSON_CREATOR)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragItemOverflowBottomSheetBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupScreenStateObserver()
        setupImageUrlObserver()
        setupAnalytics()
        viewModel.onInitialized(item, savesTab)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupScreenStateObserver() {
        viewModel.uiState.collectWhenResumed(viewLifecycleOwner) {
            when(it.screenState) {
                ItemOverflowBottomSheetScreenState.CLOSING -> {
                    dismiss()
                }
                ItemOverflowBottomSheetScreenState.OPEN_TAG_SCREEN -> {
                    ItemsTaggingFragment.show(
                        context?.asFragmentActivity(),
                        item,
                        null
                    )
                    dismiss()
                }
                else -> {}
            }
        }
    }

    private fun setupImageUrlObserver() {
        viewModel.uiState.collectWhenResumed(viewLifecycleOwner) {
            binding.image.setImageDrawable(
                LazyBitmapDrawable(
                    LazyAssetBitmap(
                        it.imageUrl,
                        AssetUser.forItem(item.time_added, item.idkey())
                    )
                )
            )
        }
    }

    private fun setupAnalytics() {
        binding.addTags.setUiEntityType(UiEntityable.Type.BUTTON)
        binding.markAsViewed.setUiEntityType(UiEntityable.Type.BUTTON)
        binding.archive.setUiEntityType(UiEntityable.Type.BUTTON)
        binding.delete.setUiEntityType(UiEntityable.Type.BUTTON)
    }

    companion object {
        private const val ARG_ITEM = "item"
        private const val ARG_SAVES_TAB = "savesTab"

        fun newInstance(
            item: Item,
            savesTab: SavesTab,
        ) = ItemOverflowBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putThing(ARG_ITEM, item)
                putEnum(ARG_SAVES_TAB, savesTab)
            }
        }
    }
}
