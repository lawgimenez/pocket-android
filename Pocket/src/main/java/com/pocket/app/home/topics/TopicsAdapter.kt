package com.pocket.app.home.topics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ideashower.readitlater.databinding.ViewHomeTopicItemBinding
import com.pocket.analytics.api.UiEntityable
import com.pocket.app.home.HomeViewModel
import com.pocket.util.android.repeatOnCreated

class TopicsAdapter(
    viewLifecycleOwner: LifecycleOwner,
    private val viewModel: HomeViewModel,
): ListAdapter<HomeViewModel.TopicUiState, TopicsAdapter.TopicViewHolder>(DIFF_CALLBACK) {

    init {
        viewLifecycleOwner.repeatOnCreated {
            viewModel.topicsUiState.collect {
                submitList(it)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder =
        TopicViewHolder(
            ViewHomeTopicItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        )

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TopicViewHolder(
        val binding: ViewHomeTopicItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(state: HomeViewModel.TopicUiState) {
            binding.apply {
                topicTitle.text = state.title
                root.setOnClickListener { viewModel.onTopicClicked(state.topicId, state.title) }
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = object: DiffUtil.ItemCallback<HomeViewModel.TopicUiState>() {
            override fun areItemsTheSame(
                oldItem: HomeViewModel.TopicUiState,
                newItem: HomeViewModel.TopicUiState,
            ): Boolean = oldItem == newItem

            override fun areContentsTheSame(
                oldItem: HomeViewModel.TopicUiState,
                newItem: HomeViewModel.TopicUiState,
            ): Boolean = oldItem == newItem
        }
    }
}

