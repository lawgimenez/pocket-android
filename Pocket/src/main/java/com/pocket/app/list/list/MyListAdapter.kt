package com.pocket.app.list.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.ViewListItemRowBinding
import com.pocket.analytics.ImpressionComponent
import com.pocket.analytics.ItemContent
import com.pocket.analytics.Tracker
import com.pocket.analytics.ViewableImpressionScrollListener
import com.pocket.analytics.api.UiEntityable
import com.pocket.app.list.BadgeState
import com.pocket.app.list.BadgeType
import com.pocket.app.list.ListItemUiState
import com.pocket.app.list.MyListViewModel
import com.pocket.app.list.bulkedit.BulkEditListItemAnimator
import com.pocket.app.list.list.swipe.SwipeImageAnimator
import com.pocket.app.settings.Theme
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier
import com.pocket.sdk.offline.cache.AssetUser
import com.pocket.sdk2.view.LazyAssetBitmap
import com.pocket.ui.util.LazyBitmapDrawable
import com.pocket.ui.view.badge.BadgeLayout
import com.pocket.ui.view.badge.BadgeView
import com.pocket.ui.view.checkable.CheckableImageView
import com.pocket.ui.view.item.ItemThumbnailView
import com.pocket.ui.view.themed.SwipeListener
import com.pocket.util.android.repeatOnCreated
import com.pocket.util.android.text.toTealHighlightedSpannableString

class MyListAdapter(
    viewLifecycleOwner: LifecycleOwner,
    private val tracker: Tracker,
    private val context: Context,
    private val viewModel: MyListViewModel,
    private val bulkEditListItemAnimator: BulkEditListItemAnimator,
    private val theme: Theme,
    private val recyclerView: RecyclerView,
    private val saveImpressionScrollListener: ViewableImpressionScrollListener,
) : RecyclerView.Adapter<MyListAdapter.ItemRowViewHolder>() {

    /**
     * used to track when the sort / filter / search state has changed so our next list submission
     * can skip async diffing
     */
    private var sortFilterStateHasChanged = false

    private var listDiffer = newListDiffer()

    init {
        viewLifecycleOwner.repeatOnCreated {
            viewModel.listManager.sortFilterState.collect {
                sortFilterStateHasChanged = true
            }
        }
        viewLifecycleOwner.repeatOnCreated {
            viewModel.listState.collect {
                if (sortFilterStateHasChanged) {
                    // Do an instant swap. No async diff.
                    notifyDataSetChanged()
                    recyclerView.scrollToPosition(0)
                    sortFilterStateHasChanged = false
                    listDiffer = newListDiffer() // Reset list differ.
                }
                listDiffer.submitList(it)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyListAdapter.ItemRowViewHolder =
        ItemRowViewHolder(
            ViewListItemRowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: MyListAdapter.ItemRowViewHolder, position: Int) =
        holder.bind(listDiffer.currentList[position], position)

    override fun getItemCount(): Int = listDiffer.currentList.size

    inner class ItemRowViewHolder(
        private val binding: ViewListItemRowBinding
    ): RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.uiEntityIdentifier = UiEntityIdentifier.ITEM.value
        }

        @Suppress("LongMethod")
        fun bind(state: ListItemUiState, position: Int) = with(binding) {
            root.setUiEntityType(UiEntityable.Type.CARD)
            tracker.bindContent(root, ItemContent(state.item.id_url?.url!!))
            tracker.bindUiEntityValue(root, if (state.titleBold) "not_viewed" else "viewed")
            title.text = if (state.showSearchHighlights) {
                state.title.toTealHighlightedSpannableString(theme, context)
            } else {
                state.title.value
            }
            title.setBold(state.titleBold)
            domain.text = if (state.showSearchHighlights) {
                state.domain.toTealHighlightedSpannableString(theme, context)
            } else {
                state.domain.value
            }
            timeEstimate.text = state.timeEstimate
            excerpt.text = state.excerpt.toTealHighlightedSpannableString(theme, context)
            excerpt.visibility = if (state.excerptVisible) {
                View.VISIBLE
            } else {
                View.GONE
            }
            setFavoriteImage(favorite, state.favorite)
            setThumbnail(
                state = state,
                thumbnailView = thumbnail,
            )
            setupBadges(
                state = state,
                badgesLayout = badgesLayout,
            )

            if (state.isInEditMode) {
                swipeLayout.setOnClickListener { viewModel.onItemSelectedForBulkEdit(state.item) }
            } else {
                swipeLayout.setOnClickListener {
                    viewModel.onItemClicked(state.item, state.index)
                }
            }
            favorite.setOnClickListener { viewModel.onFavoriteClicked(state.item) }
            favorite.isClickable = !state.isInEditMode
            share.setOnClickListener { viewModel.onShareItemClicked(state.item) }
            share.isClickable = !state.isInEditMode
            overflow.setOnClickListener { viewModel.onItemOverflowClicked(state.item) }
            overflow.isClickable = !state.isInEditMode

            if (state.isInEditMode) {
                bulkEditListItemAnimator.showBulkEdit(binding)
            } else {
                bulkEditListItemAnimator.hideBulkEdit(binding)
            }
            bulkEditRadioButton.isChecked = state.isSelectedForBulkEdit
            val swipeImage = if (state.isInArchive) {
                com.pocket.ui.R.drawable.ic_pkt_re_add_line
            } else {
                com.pocket.ui.R.drawable.ic_pkt_archive_line
            }
            rightSwipeImage.setImageResource(swipeImage)
            leftSwipeImage.setImageResource(swipeImage)
            swipeLayout.reset()
            swipeLayout.allowSwiping = !state.isInEditMode
            swipeLayout.swipeListener = object : SwipeListener {
                override fun onSwipedRight() {
                    viewModel.onItemSwipedRight(state.item)
                }

                override fun onSwipedLeft() {
                    viewModel.onItemSwipedLeft(state.item)
                }

                override fun onMovement(percentToSwipeThreshold: Float) {
                    SwipeImageAnimator.updateImage(
                        percentToSwipeThreshold,
                        leftSwipeImage,
                        rightSwipeImage
                    )
                }
            }
            saveImpressionScrollListener.track(
                view = root,
                identifier = state.item.id_url!!.url,
            ) {
                viewModel.onSaveViewed(
                    itemUrl = state.item.id_url!!.url,
                    position = position
                )
            }
        }

        private fun setupBadges(
            state: ListItemUiState,
            badgesLayout: BadgeLayout,
        ) {
            badgesLayout.removeAllViews()
            badgesLayout.setBadges(
                state.badges
                    .sortedWith(
                        compareByDescending<BadgeState> {
                            it.type == BadgeType.HIGHLIGHT
                        }.thenByDescending {
                            it.type == BadgeType.SEARCH_MATCHING_TAG
                        }
                    )
                    .map { badgeState ->
                        BadgeView(context).apply {
                            setValues(
                                when (badgeState.type) {
                                    BadgeType.TAG -> BadgeView.Type.TAG
                                    BadgeType.HIGHLIGHT -> BadgeView.Type.HIGHLIGHT
                                    BadgeType.SEARCH_MATCHING_TAG -> BadgeView.Type.EMPHASIZED_TAG
                                },
                                badgeState.text
                            )
                            setOnClickListener {
                                if (badgeState.type == BadgeType.TAG) {
                                    viewModel.onTagBadgeClicked(badgeState.text)
                                }
                            }
                            isClickable = !state.isInEditMode
                        }
                    }
            )
            badgesLayout.visibility = if (state.badges.isEmpty()) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        private fun setThumbnail(
            state: ListItemUiState,
            thumbnailView: ItemThumbnailView,
        ) {
            thumbnailView.visibility = if (state.thumbnailVisible) {
                thumbnailView.setImageDrawable(
                    LazyBitmapDrawable(
                        LazyAssetBitmap(
                            state.imageUrl,
                            AssetUser.forItem(state.item.time_added, state.item.idkey())
                        )
                    )
                )
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        private fun setFavoriteImage(image: CheckableImageView, favorited: Boolean) {
            image.isChecked = favorited
        }
    }

    private fun newListDiffer() = AsyncListDiffer(this, DIFF_CALLBACK)

    companion object {
        val DIFF_CALLBACK = object: DiffUtil.ItemCallback<ListItemUiState>() {
            override fun areItemsTheSame(
                oldItem: ListItemUiState,
                newItem: ListItemUiState,
            ): Boolean = oldItem.item == newItem.item

            override fun areContentsTheSame(
                oldItem: ListItemUiState,
                newItem: ListItemUiState,
            ): Boolean = oldItem == newItem
        }
    }
}