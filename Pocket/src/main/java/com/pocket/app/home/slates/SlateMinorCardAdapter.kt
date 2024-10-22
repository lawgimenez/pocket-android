package com.pocket.app.home.slates

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ideashower.readitlater.databinding.ViewHomeSlateMinorCardBinding
import com.pocket.analytics.ViewableImpressionScrollListener
import com.pocket.app.home.HomeViewModel
import com.pocket.app.home.details.RecommendationUiState

class SlateMinorCardAdapter(
    private val viewModel: HomeViewModel,
    private val itemWidth: Int? = null,
    private val impressionScrollListener: ViewableImpressionScrollListener,
): RecyclerView.Adapter<SlateMinorCardAdapter.MinorCardViewHolder>() {
    private var recommendations = emptyList<RecommendationUiState>()
    private lateinit var slateTitle: String

    fun setData(recommendations: List<RecommendationUiState>, slateTitle: String) {
        this.slateTitle = slateTitle
        this.recommendations = recommendations
        notifyDataSetChanged()
    }

    override fun getItemCount() = recommendations.count()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): MinorCardViewHolder =
        MinorCardViewHolder(
            ViewHomeSlateMinorCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: MinorCardViewHolder, position: Int) =
        holder.bind(recommendations[position])

    inner class MinorCardViewHolder(
        private val binding: ViewHomeSlateMinorCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemWidth?.let {
                binding.root.layoutParams.width = it
            }
        }

        fun bind(state: RecommendationUiState) {
            binding.apply {
                DefaultSlateViewHolderHelper.bind(
                    slateTitle = slateTitle,
                    impressionScrollListener = impressionScrollListener,
                    viewModel = viewModel,
                    state = state,
                    title = title,
                    domain = domain,
                    timeToRead = timeToRead,
                    image = image,
                    collectionLabel = collectionLabel,
                    saveLayout = saveLayout,
                    rootView = root,
                    overflow = overflow,
                )
            }
        }
    }
}
