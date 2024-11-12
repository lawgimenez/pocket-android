package com.pocket.app.list.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.ideashower.readitlater.databinding.ViewRecentSearchItemBinding
import com.pocket.app.list.MyListViewModel
import com.pocket.app.list.RecentSearchItemUiState
import com.pocket.util.android.repeatOnCreated

class RecentSearchAdapter(
    private val viewModel: MyListViewModel,
    lifecycleOwner: LifecycleOwner,
) : RecyclerView.Adapter<RecentSearchAdapter.RecentSearchViewHolder>() {

    init {
        lifecycleOwner.repeatOnCreated {
            viewModel.recentSearchState.collect {
                notifyDataSetChanged()
            }
        }
    }

    private val recentSearches: List<RecentSearchItemUiState>
        get() = viewModel.recentSearchState.value

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentSearchViewHolder =
        RecentSearchViewHolder(
            ViewRecentSearchItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        )

    override fun onBindViewHolder(holder: RecentSearchViewHolder, position: Int) {
        holder.bind(recentSearches[position])
    }

    override fun getItemCount(): Int = recentSearches.size

    inner class RecentSearchViewHolder(
        private val binding: ViewRecentSearchItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(state: RecentSearchItemUiState) {
            binding.recentSearchText.text = state.text
            binding.root.setOnClickListener {
                viewModel.onRecentSearchClicked(state.text)
            }
        }
    }
}