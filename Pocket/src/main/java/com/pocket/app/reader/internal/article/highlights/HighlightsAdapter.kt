package com.pocket.app.reader.internal.article.highlights

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ideashower.readitlater.databinding.ViewHighlightCellBinding
import com.pocket.util.android.repeatOnCreated

class HighlightsAdapter(
    viewLifecycleOwner: LifecycleOwner,
    private val viewModel: HighlightsBottomSheetViewModel,
): ListAdapter<HighlightsBottomSheetViewModel.HighlightUiState, HighlightsAdapter.HighlightViewHolder>(DIFF_CALLBACK) {

    init {
        viewLifecycleOwner.repeatOnCreated {
            viewModel.highlights.collect {
                submitList(it)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HighlightViewHolder =
        HighlightViewHolder(
            ViewHighlightCellBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        )

    override fun onBindViewHolder(holder: HighlightViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HighlightViewHolder(
        private val binding: ViewHighlightCellBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(state: HighlightsBottomSheetViewModel.HighlightUiState) {
            binding.highlightText.text = state.text
            binding.highlightText.setOnClickListener { viewModel.onHighlightClicked(state.id) }
            binding.deleteButton.setOnClickListener { viewModel.onDeleteClicked(state.id) }
            binding.shareButton.setOnClickListener { viewModel.onShareClicked(state.text) }
        }
    }

    companion object {

        val DIFF_CALLBACK = object: DiffUtil.ItemCallback<HighlightsBottomSheetViewModel.HighlightUiState>() {
            override fun areItemsTheSame(
                oldItem: HighlightsBottomSheetViewModel.HighlightUiState,
                newItem: HighlightsBottomSheetViewModel.HighlightUiState,
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: HighlightsBottomSheetViewModel.HighlightUiState,
                newItem: HighlightsBottomSheetViewModel.HighlightUiState,
            ): Boolean = oldItem == newItem
        }
    }
}