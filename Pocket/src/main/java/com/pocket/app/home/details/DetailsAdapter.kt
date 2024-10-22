package com.pocket.app.home.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ideashower.readitlater.databinding.ViewHomeHeroCardBinding
import com.pocket.analytics.ViewableImpressionScrollListener
import com.pocket.sdk2.view.LazyAssetBitmap
import com.pocket.ui.util.LazyBitmapDrawable
import com.pocket.util.android.repeatOnCreated

class DetailsAdapter(
    viewLifecycleOwner: LifecycleOwner,
    private val viewModel: DetailsViewModel,
    private val impressionScrollListener: ViewableImpressionScrollListener,
): ListAdapter<RecommendationUiState,
        DetailsAdapter.ViewHolder>(DIFF_CALLBACK) {

    init {
        viewLifecycleOwner.repeatOnCreated {
            viewModel.uiState.collect {
                submitList(it.recommendations)
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
        holder.bind(getItem(position), position)

    inner class ViewHolder(
        private val binding: ViewHomeHeroCardBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(
            state: RecommendationUiState,
            position: Int,
        ) {
            binding.root.binding.apply {
                title.text = state.title
                domain.text = state.domain
                timeToRead.apply {
                    text = state.timeToRead
                    visibility = if (state.timeToRead.isNullOrBlank()) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
                }
                image.setImageDrawable(
                    LazyBitmapDrawable(
                        LazyAssetBitmap(
                            state.imageUrl,
                            null
                        )
                    )
                )
                collectionLabel.visibility = if (state.isCollection) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                saveLayout.bind().clear()
                    .setSaved(state.isSaved)
                    .setOnSaveButtonClickListener { _, _ ->
                        viewModel.onSaveClicked(
                            url = state.url,
                            isSaved = state.isSaved,
                            corpusRecommendationId = state.corpusRecommendationId
                        )
                        state.isSaved
                    }
                saveLayout.uiEntityComponentDetail = state.title
                root.setOnClickListener {
                    viewModel.onItemClicked(
                        url = state.url,
                        positionInList = state.index,
                        corpusRecommendationId = state.corpusRecommendationId,
                    )
                }
                overflow.setOnClickListener {
                    viewModel.onOverflowClicked(
                        url = state.url,
                        title = state.title,
                        corpusRecommendationId = state.corpusRecommendationId,
                    )
                }
                overflow.engageable.uiEntityComponentDetail = state.title
            }

            impressionScrollListener.track(
                view = binding.root,
                identifier = state.url,
            ) {
                viewModel.onItemViewed(position, state.url, state.corpusRecommendationId)
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = object: DiffUtil.ItemCallback<RecommendationUiState>() {
            override fun areItemsTheSame(
                oldItem: RecommendationUiState,
                newItem: RecommendationUiState,
            ): Boolean = oldItem.itemId == newItem.itemId

            override fun areContentsTheSame(
                oldItem: RecommendationUiState,
                newItem: RecommendationUiState,
            ): Boolean = oldItem == newItem
        }
    }
}