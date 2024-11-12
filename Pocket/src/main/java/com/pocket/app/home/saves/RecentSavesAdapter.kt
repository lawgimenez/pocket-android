package com.pocket.app.home.saves

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ideashower.readitlater.databinding.ViewHomeRecentSaveCardBinding
import com.pocket.analytics.ViewableImpressionScrollListener
import com.pocket.sdk2.view.LazyAssetBitmap
import com.pocket.ui.util.LazyBitmapDrawable
import com.pocket.util.android.repeatOnCreated

class RecentSavesAdapter(
    viewLifecycleOwner: LifecycleOwner,
    private val viewModel: RecentSavesViewModel,
    private val impressionScrollListener: ViewableImpressionScrollListener,
): ListAdapter<RecentSavesViewModel.SaveUiState, RecentSavesAdapter.SavesViewHolder>(DIFF_CALLBACK) {

    init {
        viewLifecycleOwner.repeatOnCreated {
            viewModel.recentSavesUiState.collect {
                submitList(it)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavesViewHolder =
        SavesViewHolder(
            ViewHomeRecentSaveCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        )

    override fun onBindViewHolder(holder: SavesViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class SavesViewHolder(
        private val binding: ViewHomeRecentSaveCardBinding,
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(
            state: RecentSavesViewModel.SaveUiState,
            position: Int,
        ) {
            binding.apply {
                title.text = state.title
                title.setBold(state.titleIsBold)
                domain.text = state.domain
                timeToRead.apply {
                    text = state.timeToRead
                    visibility = if (state.timeToRead.isNotBlank()) {
                        View.VISIBLE
                    } else {
                        View.GONE
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
                root.setOnClickListener {
                    viewModel.onItemClicked(state.item, state.index)
                }
                favoriteIcon.isChecked = state.isFavorited
                favoriteIcon.setOnClickListener { viewModel.onFavoriteClicked(state.item, state.index) }
                overflow.setOnClickListener { viewModel.onSaveOverflowClicked(state.item, state.index) }

                impressionScrollListener.track(
                    view = root,
                    identifier = state.item.id_url!!.url
                ) {
                    viewModel.onSaveViewed(position, state.item.id_url!!.url)
                }
            }
        }
    }

    companion object {

        val DIFF_CALLBACK = object: DiffUtil.ItemCallback<RecentSavesViewModel.SaveUiState>() {
            override fun areItemsTheSame(
                oldItem: RecentSavesViewModel.SaveUiState,
                newItem: RecentSavesViewModel.SaveUiState,
            ): Boolean = oldItem.item == newItem.item

            override fun areContentsTheSame(
                oldItem: RecentSavesViewModel.SaveUiState,
                newItem: RecentSavesViewModel.SaveUiState,
            ): Boolean = oldItem == newItem
        }
    }
}