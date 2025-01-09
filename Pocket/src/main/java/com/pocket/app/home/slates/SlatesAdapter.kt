package com.pocket.app.home.slates

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.GONE
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.ViewHomeSlateDefaultBinding
import com.ideashower.readitlater.databinding.ViewHomeSlateWideBinding
import com.pocket.analytics.ViewableImpressionScrollListener
import com.pocket.app.home.HomeViewModel
import com.pocket.app.home.decorators.GridSpacingDecorator
import com.pocket.app.home.decorators.HorizontalSpacingDecorator
import com.pocket.util.android.repeatOnCreated

class SlatesAdapter(
    viewLifecycleOwner: LifecycleOwner,
    private val viewModel: HomeViewModel,
    private val isTablet: Boolean,
    private val impressionScrollListener: ViewableImpressionScrollListener
): ListAdapter<HomeViewModel.RecommendationSlateUiState, ViewHolder>(DIFF_CALLBACK) {

    init {
        viewLifecycleOwner.repeatOnCreated {
            viewModel.slatesUiState.collect {
                submitList(it)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        when (viewType) {
            TABLET -> SlatesTabletViewHolder(
                ViewHomeSlateWideBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
            else -> SlateViewHolder(
                ViewHomeSlateDefaultBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
        }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        when (holder) {
            is SlateViewHolder -> holder.bind(getItem(position), position)
            is SlatesTabletViewHolder -> holder.bind(getItem(position), position)
            else -> {}
        }


    override fun getItemViewType(position: Int): Int =
        if (isTablet) {
            TABLET
        } else {
            NORMAL
        }

    inner class SlateViewHolder(
        private val binding: ViewHomeSlateDefaultBinding
    ): ViewHolder(binding.root) {

        private val horizontallyScrollingCardWidth =
            binding.root.context.resources.getDimensionPixelSize(R.dimen.home_minor_card_width)
        private val minorCardAdapter = SlateMinorCardAdapter(
            viewModel,
            horizontallyScrollingCardWidth,
            impressionScrollListener,
        )

        init {
            binding.minorCardRecyclerView.addOnScrollListener(impressionScrollListener)
            binding.minorCardRecyclerView.addItemDecoration(HorizontalSpacingDecorator())
            binding.minorCardRecyclerView.adapter = minorCardAdapter
            binding.minorCardRecyclerView.itemAnimator = null
        }

        fun bind(state: HomeViewModel.RecommendationSlateUiState, position: Int) {
            binding.title.text = state.title
            if (state.subheadline != null) {
                binding.subtitle.text = state.subheadline
            } else {
                binding.subtitle.visibility = GONE
            }
            binding.slateSeeAllLayout.apply {
                setOnClickListener {
                    viewModel.onSeeAllRecommendationsClicked(position, state.title.orEmpty())
                }
                engageable.uiEntityComponentDetail = state.title
            }

            // first item goes to the hero card
            binding.heroCard.binding.apply {
                DefaultSlateViewHolderHelper.bind(
                    slateTitle = state.title!!,
                    impressionScrollListener = impressionScrollListener,
                    viewModel = viewModel,
                    state = state.recommendations.first(),
                    title = title,
                    domain = domain,
                    timeToRead = timeToRead,
                    image = image,
                    collectionLabel = collectionLabel,
                    saveLayout = saveLayout,
                    rootView = binding.heroCard,
                    overflow = overflow,
                )
            }

            // the rest of the items go to the nested horizontally scrolling recycler view
            minorCardAdapter.setData(state.recommendations.drop(1), state.title!!)
        }
    }

    inner class SlatesTabletViewHolder(
        private val binding: ViewHomeSlateWideBinding
    ): ViewHolder(binding.root) {

        private val minorCardAdapter = SlateMinorCardAdapter(
            viewModel = viewModel,
            impressionScrollListener = impressionScrollListener,
        )

        init {
            binding.minorCardRecyclerView.addOnScrollListener(impressionScrollListener)
            binding.minorCardRecyclerView.addItemDecoration(GridSpacingDecorator())
            binding.minorCardRecyclerView.adapter = minorCardAdapter
            binding.minorCardRecyclerView.itemAnimator = null
        }

        fun bind(state: HomeViewModel.RecommendationSlateUiState, position: Int) {
            binding.title.text = state.title
            if (state.subheadline != null) {
                binding.subtitle.text = state.subheadline
            } else {
                binding.subtitle.visibility = GONE
            }
            binding.seeAllLayout.setOnClickListener {
                viewModel.onSeeAllRecommendationsClicked(position, state.title.orEmpty())
            }

            // first item goes to the hero card
            binding.heroCard.binding.apply {
                DefaultSlateViewHolderHelper.bind(
                    slateTitle = state.title ?: "",
                    impressionScrollListener = impressionScrollListener,
                    viewModel = viewModel,
                    state = state.recommendations.first(),
                    title = title,
                    domain = domain,
                    timeToRead = timeToRead,
                    image = image,
                    collectionLabel = collectionLabel,
                    saveLayout = saveLayout,
                    rootView = binding.heroCard,
                    overflow = overflow,
                    excerpt = excerpt,
                )
            }

            // the rest of the items go to the nested grid recycler view
            minorCardAdapter.setData(state.recommendations.drop(1), state.title ?: "")
        }
    }

    companion object {

        private const val NORMAL = 0
        private const val TABLET = 1

        val DIFF_CALLBACK = object: DiffUtil.ItemCallback<HomeViewModel.RecommendationSlateUiState>() {
            override fun areItemsTheSame(
                oldItem: HomeViewModel.RecommendationSlateUiState,
                newItem: HomeViewModel.RecommendationSlateUiState,
            ): Boolean {
                // Slate ids are not stable, so we can't use them for comparing identity.
                // Instead we'll use a heuristic, to see if at least some of the data didn't change.
                // This way we'll account for small changes, like:
                // * we updated the title, but not the headline;
                // * we updated the headline, but not the title;
                // * we updated both the title and the headline, but not any of the stories;
                // * we updated just the stories.
                // If at some point we update all three things at once, then Home might animate
                // as if a slate was removed and a new one appeared in its place 🤷
                // As long as we don't have multiple slates with the same title or headline
                // this should work nicely for reordering or adding an extra thematic slate.
                return oldItem.title == newItem.title ||
                        oldItem.subheadline == newItem.subheadline ||
                        oldItem.recommendations == newItem.recommendations
            }

            override fun areContentsTheSame(
                oldItem: HomeViewModel.RecommendationSlateUiState,
                newItem: HomeViewModel.RecommendationSlateUiState,
            ): Boolean = oldItem == newItem
        }
    }
}