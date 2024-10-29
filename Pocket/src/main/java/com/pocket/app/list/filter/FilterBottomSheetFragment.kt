package com.pocket.app.list.filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.ideashower.readitlater.databinding.FragSortFilterBottomSheetBinding
import com.pocket.analytics.appevents.SavesTab
import com.pocket.sdk.util.AbsPocketBottomSheetDialogFragment
import com.pocket.util.android.enumArg
import com.pocket.util.android.putEnum
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FilterBottomSheetFragment : AbsPocketBottomSheetDialogFragment() {

    private val viewModel by viewModels<FilterBottomSheetViewModel>()

    private var _binding: FragSortFilterBottomSheetBinding? = null
    private val binding: FragSortFilterBottomSheetBinding
        get() = _binding!!

    private val savesTab by enumArg<SavesTab>(ARG_SAVES_TAB)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragSortFilterBottomSheetBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.onInitialized(savesTab)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SAVES_TAB = "savesTab"

        fun newInstance(savesTab: SavesTab) = FilterBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putEnum(ARG_SAVES_TAB, savesTab)
            }
        }
    }
}
