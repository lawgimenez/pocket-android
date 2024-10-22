package com.pocket.app.home.slates

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.pocket.analytics.*
import com.pocket.app.home.HomeViewModel
import com.pocket.app.home.details.RecommendationUiState
import com.pocket.sdk2.view.LazyAssetBitmap
import com.pocket.ui.util.LazyBitmapDrawable
import com.pocket.ui.view.item.SaveButton
import com.pocket.ui.view.themed.ThemedCardView
import com.pocket.ui.view.themed.ThemedImageView

/**
 * Home slate hero cards and minor cards are very similar, so this object helps
 * to reduce duplicated code when binding views
 */
object DefaultSlateViewHolderHelper {

    @Suppress("LongMethod")
    fun bind(
        slateTitle: String,
        impressionScrollListener: ViewableImpressionScrollListener,
        viewModel: HomeViewModel,
        state: RecommendationUiState,
        title: TextView,
        domain: TextView,
        timeToRead: TextView,
        image: ImageView,
        collectionLabel: TextView,
        saveLayout: SaveButton,
        rootView: ThemedCardView,
        overflow: ThemedImageView,
        excerpt: TextView? = null,
    ) {
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
                    corpusRecommendationId = state.corpusRecommendationId,
                )
                state.isSaved
            }
        rootView.apply {
            setOnClickListener {
                viewModel.onItemClicked(
                    url = state.url,
                    slateTitle = slateTitle,
                    positionInSlate = state.index,
                    corpusRecommendationId = state.corpusRecommendationId,
                )
            }
            impressionScrollListener.track(
                view = this,
                identifier = ItemContent(state.url)
            ) {
                viewModel.onRecommendationViewed(
                    slateTitle = slateTitle,
                    positionInSlate = state.index,
                    itemUrl = state.url,
                    corpusRecommendationId = state.corpusRecommendationId,
                )
            }
        }
        overflow.setOnClickListener {
            viewModel.onRecommendationOverflowClicked(
                corpusRecommendationId = state.corpusRecommendationId,
                url = state.url,
                title = state.title,
            )
        }

        excerpt?.let { it.text = state.excerpt }
    }
}