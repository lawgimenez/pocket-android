package com.pocket.app.home.details

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.FragmentHomeDetailsBinding
import com.pocket.analytics.ViewableImpressionScrollListener
import com.pocket.app.MainActivity
import com.pocket.app.auth.AuthenticationActivity
import com.pocket.app.home.decorators.VerticalSpacingDecorator
import com.pocket.app.home.slates.overflow.RecommendationOverflowBottomSheetFragment
import com.pocket.sdk.util.AbsPocketFragment
import com.pocket.util.android.FormFactor
import com.pocket.util.android.view.InstantChangeItemAnimator
import com.pocket.util.collectWhenResumed
import dagger.hilt.android.AndroidEntryPoint

/**
 * abstract fragment with common code shared between
 * [com.pocket.app.home.details.topics.TopicDetailsFragment] and
 * [com.pocket.app.home.details.slates.SlateDetailsFragment]
 */
@AndroidEntryPoint
abstract class DetailsFragment : AbsPocketFragment() {

    protected abstract val viewModel: DetailsViewModel

    protected var _binding: FragmentHomeDetailsBinding? = null
    protected val binding: FragmentHomeDetailsBinding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showsDialog = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupEventObserver()
        setupAppBar()
        setupBookAnimation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        val impressionScrollListener = ViewableImpressionScrollListener(viewLifecycleOwner)
        binding.scrollView.setOnScrollChangeListener(impressionScrollListener)
        binding.recyclerView.addItemDecoration(VerticalSpacingDecorator())
        binding.recyclerView.adapter = DetailsAdapter(
            viewLifecycleOwner = viewLifecycleOwner,
            viewModel = viewModel,
            impressionScrollListener = impressionScrollListener
        )
        if (FormFactor.isTablet(context)) binding.recyclerView.layoutManager =
            GridLayoutManager(context, 2)
        binding.recyclerView.itemAnimator = InstantChangeItemAnimator()
    }

    private fun setupAppBar() {
        binding.appBar.bind().onLeftIconClick {
            (activity as? MainActivity)?.onBackPressed()
        }
    }

    private fun setupEventObserver() {
        viewModel.events.collectWhenResumed(viewLifecycleOwner) { event ->
            when (event) {
                is DetailsViewModel.Event.GoToReader -> {
                    goToReader(event)
                }
                is DetailsViewModel.Event.GoToSignIn -> {
                    AuthenticationActivity.startActivity(requireContext(), true)
                }
                is DetailsViewModel.Event.ShowRecommendationOverflow -> {
                    RecommendationOverflowBottomSheetFragment.newInstance(
                        url = event.url,
                        title = event.title,
                        corpusRecommendationId = event.corpusRecommendationId,
                    )
                        .show(
                            childFragmentManager,
                            RecommendationOverflowBottomSheetFragment::class.simpleName
                        )
                }
            }
        }
    }

    private fun setupBookAnimation() {
        // scroll change listener is only available on api 23+.  If the user is on a
        // lower version, just hide the animation view.  We have almost no users
        // on versions that low.
        binding.bookAnimationView.binder
            .clear()
            .text(R.string.home_footer_message)
        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val bookBottom = binding.bookAnimationView.bottom
            val bottomOfScreen = binding.root.height + scrollY
            // if we scrolled to the bottom of book animation view
            if (bookBottom - bottomOfScreen <= 0) {
                binding.bookAnimationView.playAnimation()
            }
        }
    }

    abstract fun goToReader(event: DetailsViewModel.Event.GoToReader)
}
