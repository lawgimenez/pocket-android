package com.pocket.app.list.tags

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.FragTagBottomSheetBinding
import com.pocket.analytics.appevents.SavesTab
import com.pocket.sdk.util.AbsPocketBottomSheetDialogFragment
import com.pocket.sdk.util.dialog.AlertMessaging
import com.pocket.ui.view.menu.MenuItem
import com.pocket.ui.view.menu.ThemedPopupMenu
import com.pocket.util.android.enumArg
import com.pocket.util.android.putEnum
import com.pocket.util.collectWhenResumed
import com.pocket.util.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TagBottomSheetFragment : AbsPocketBottomSheetDialogFragment() {

    private val viewModel by viewModels<TagBottomSheetViewModel>()

    private var _binding: FragTagBottomSheetBinding? = null
    private val binding: FragTagBottomSheetBinding
        get() = _binding!!

    private val savesTab by enumArg<SavesTab>(ARG_SAVES_TAB)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragTagBottomSheetBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.onInitialized(savesTab)
        setupRecyclerView()
        setupClickListeners()
        setupScreenStateListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        binding.recyclerView.adapter = TagsAdapter(
            viewModel = viewModel,
            lifecycleOwner = viewLifecycleOwner
        )
    }

    private fun setupClickListeners() {
        binding.overflowButton.setOnClickListener {
            ThemedPopupMenu(
                requireContext(),
                ThemedPopupMenu.Section.actions(
                    null,
                    listOf(
                        MenuItem(
                            com.pocket.ui.R.string.ic_edit,
                            com.pocket.ui.R.drawable.ic_pkt_pencil_line
                        ) {
                            viewModel.onEditClicked()
                        }
                    )
                )
            ).show(binding.overflowButton)
        }

        binding.cancelButton.setOnClickListener {
            binding.root.hideKeyboard()
            viewModel.onCancelClicked()
        }

        binding.saveButton.setOnClickListener {
            binding.root.hideKeyboard()
            viewModel.onSaveClicked()
        }
    }

    private fun setupScreenStateListener() {
        viewModel.navigationEvent.collectWhenResumed(viewLifecycleOwner) { event ->
            when (event) {
                TagBottomSheetNavigationEvent.Close -> dismiss()
                TagBottomSheetNavigationEvent.ShowConfirmDelete -> {
                    AlertMessaging.show(
                        requireContext(),
                        getString(R.string.delete_tag_confirmation_message, viewModel.tagToDelete),
                        null,
                        getString(com.pocket.ui.R.string.ic_cancel),
                        { _, _ ->
                            viewModel.onDeleteCanceled()
                        },
                        getString(com.pocket.ui.R.string.ic_delete),
                        { _, _ ->
                            viewModel.onDeleteTagConfirmed()
                        },
                        false
                    )
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.onDismissed()
    }

    companion object {
        private const val ARG_SAVES_TAB = "savesTab"

        fun newInstance(savesTab: SavesTab) = TagBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putEnum(ARG_SAVES_TAB, savesTab)
            }
        }
    }
}
