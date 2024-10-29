package com.pocket.app.reader.internal.article.textsettings.fontSettings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ideashower.readitlater.databinding.ViewFontChoiceCellBinding
import com.pocket.app.reader.internal.article.DisplaySettingsManager.FontOption
import com.pocket.util.android.repeatOnCreated

class FontSettingsAdapter(
    viewLifecycleOwner: LifecycleOwner,
    private val viewModel: FontSettingsBottomSheetViewModel,
): ListAdapter<FontSettingsBottomSheetViewModel.FontChoiceUiState, FontSettingsAdapter.FontChoiceViewHolder>(DIFF_CALLBACK) {

    init {
        viewLifecycleOwner.repeatOnCreated {
            viewModel.fontChoiceUiState.collect {
                submitList(it)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontChoiceViewHolder =
        FontChoiceViewHolder(
            ViewFontChoiceCellBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        )

    override fun onBindViewHolder(holder: FontChoiceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FontChoiceViewHolder(
        private val binding: ViewFontChoiceCellBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(state: FontSettingsBottomSheetViewModel.FontChoiceUiState) {
            binding.fontName.text = state.fontName
            binding.fontName.typeface =
                FontOption.values().find { it.id == state.fontId }?.getTypeface(binding.root.context)
            binding.premiumIcon.visibility = if (state.premiumIconVisible) {
                View.VISIBLE
            } else {
                View.GONE
            }
            binding.selectedCheck.visibility = if (state.isSelected) {
                View.VISIBLE
            } else {
                View.GONE
            }
            binding.upgrade.visibility = if (state.upgradeVisible) {
                View.VISIBLE
            } else {
                View.GONE
            }

            binding.root.setOnClickListener {
                viewModel.onFontSelected(state.fontId)
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = object: DiffUtil.ItemCallback<FontSettingsBottomSheetViewModel.FontChoiceUiState>() {
            override fun areItemsTheSame(
                oldItem: FontSettingsBottomSheetViewModel.FontChoiceUiState,
                newItem: FontSettingsBottomSheetViewModel.FontChoiceUiState,
            ): Boolean = oldItem.fontName == newItem.fontName

            override fun areContentsTheSame(
                oldItem: FontSettingsBottomSheetViewModel.FontChoiceUiState,
                newItem: FontSettingsBottomSheetViewModel.FontChoiceUiState,
            ): Boolean = oldItem == newItem
        }
    }
}