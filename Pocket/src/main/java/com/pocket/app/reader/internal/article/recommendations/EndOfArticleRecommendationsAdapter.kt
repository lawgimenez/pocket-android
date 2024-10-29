package com.pocket.app.reader.internal.article.recommendations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ideashower.readitlater.databinding.ViewHomeSlateMinorCardBinding
import com.pocket.analytics.ViewableImpressionScrollListener
import com.pocket.sdk2.view.LazyAssetBitmap
import com.pocket.ui.util.LazyBitmapDrawable
import com.pocket.util.android.repeatOnCreated

class EndOfArticleRecommendationsAdapter(
    viewLifecycleOwner: LifecycleOwner,
    private val viewModel: EndOfArticleRecommendationsViewModel,
    private val impressionScrollListener: ViewableImpressionScrollListener,
): ListAdapter<EndOfArticleRecommendationsViewModel.CorpusItemUiState,
        EndOfArticleRecommendationsAdapter.RecommendationViewHolder>(DIFF_CALLBACK) {

    init {
        viewLifecycleOwner.repeatOnCreated {
            viewModel.recommendations.collect {
                submitList(it)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationViewHolder =
        RecommendationViewHolder(
            ViewHomeSlateMinorCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        )

    override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class RecommendationViewHolder(
        private val binding: ViewHomeSlateMinorCardBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(
            state: EndOfArticleRecommendationsViewModel.CorpusItemUiState,
            position: Int,
        ) {
            binding.apply {
                title.text = state.title
                domain.text = state.publisher
                saveLayout.bind().setSaved(state.isSaved)
                image.setImageDrawable(
                    LazyBitmapDrawable(
                        LazyAssetBitmap(
                            state.imageUrl,
                            null
                        )
                    )
                )
                collectionLabel.visibility = View.GONE

                root.setOnClickListener { viewModel.onCardClicked(state.url, state.corpusRecommendationId) }
                saveLayout.bind().setOnSaveButtonClickListener { _, saved ->
                    viewModel.onSaveClicked(state.url, !saved, state.corpusRecommendationId)
                    saved
                }
                overflow.setOnClickListener { viewModel.onOverflowClicked(
                    url = state.url,
                    title = state.title,
                    corpusRecommendationId = state.corpusRecommendationId
                ) }
                impressionScrollListener.track(
                    view = root,
                    identifier = state.url
                ) {
                    viewModel.onArticleViewed(position, state.url, state.corpusRecommendationId)
                }
            }
        }
    }

    companion object {

        val DIFF_CALLBACK = object: DiffUtil.ItemCallback<EndOfArticleRecommendationsViewModel.CorpusItemUiState>() {
            override fun areItemsTheSame(
                oldItem: EndOfArticleRecommendationsViewModel.CorpusItemUiState,
                newItem: EndOfArticleRecommendationsViewModel.CorpusItemUiState,
            ): Boolean = oldItem.url == newItem.url

            override fun areContentsTheSame(
                oldItem: EndOfArticleRecommendationsViewModel.CorpusItemUiState,
                newItem: EndOfArticleRecommendationsViewModel.CorpusItemUiState,
            ): Boolean = oldItem == newItem
        }
    }
}