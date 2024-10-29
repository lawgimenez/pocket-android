package com.pocket.app.reader.internal.collection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ideashower.readitlater.databinding.ViewHomeHeroCardBinding
import com.pocket.sdk.api.value.MarkdownString
import com.pocket.sdk.util.MarkdownHandler
import com.pocket.sdk2.view.LazyAssetBitmap
import com.pocket.ui.util.LazyBitmapDrawable
import com.pocket.util.android.repeatOnCreated

class CollectionStoryAdapter(
    viewLifecycleOwner: LifecycleOwner,
    private val viewModel: CollectionViewModel,
    private val markdown: MarkdownHandler,
    private val corpusRecommendationId: String?,
): ListAdapter<CollectionViewModel.StoryUiState,
        CollectionStoryAdapter.ViewHolder>(DIFF_CALLBACK) {

    init {
        viewLifecycleOwner.repeatOnCreated {
            viewModel.storyListUiState.collect {
                submitList(it)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ViewHomeHeroCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class ViewHolder(
        private val binding: ViewHomeHeroCardBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(state: CollectionViewModel.StoryUiState) {
            binding.root.binding.apply {
                title.text = state.title

                excerpt.visibility = View.VISIBLE
                with(markdown) {
                    excerpt.setMarkdownString(MarkdownString(state.excerpt))
                }

                collectionLabel.visibility = if (state.collectionLabelVisible) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                domain.text = state.publisher

                saveLayout.bind().clear()
                    .setSaved(state.isSaved)

                timeToRead.visibility = View.GONE

                image.setImageDrawable(
                    LazyBitmapDrawable(
                        LazyAssetBitmap(
                            state.imageUrl,
                            null
                        )
                    )
                )

                saveLayout.setOnClickListener { viewModel.onSaveClicked(state.url) }
                overflow.setOnClickListener {
                    viewModel.onOverflowClicked(
                        url = state.url,
                        title = state.title,
                        corpusRecommendationId = corpusRecommendationId,
                    )
                }
                excerpt.setOnClickListener { viewModel.onCardClicked(state.url) }
                root.setOnClickListener { viewModel.onCardClicked(state.url) }
            }
        }

    }

    companion object {
        val DIFF_CALLBACK = object: DiffUtil.ItemCallback<CollectionViewModel.StoryUiState>() {
            override fun areItemsTheSame(
                oldItem: CollectionViewModel.StoryUiState,
                newItem: CollectionViewModel.StoryUiState,
            ): Boolean = oldItem.title == newItem.title

            override fun areContentsTheSame(
                oldItem: CollectionViewModel.StoryUiState,
                newItem: CollectionViewModel.StoryUiState,
            ): Boolean = oldItem == newItem
        }
    }
}