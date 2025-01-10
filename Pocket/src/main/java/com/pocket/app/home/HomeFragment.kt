package com.pocket.app.home

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.FragmentHomeBinding
import com.pocket.analytics.ViewableImpressionScrollListener
import com.pocket.analytics.api.UiEntityable
import com.pocket.app.auth.AuthenticationActivity
import com.pocket.app.home.decorators.HorizontalSpacingDecorator
import com.pocket.app.home.saves.RecentSavesAdapter
import com.pocket.app.home.saves.RecentSavesViewModel
import com.pocket.app.home.saves.overflow.RecentSavesOverflowFragment
import com.pocket.app.home.slates.SlatesAdapter
import com.pocket.app.home.slates.overflow.RecommendationOverflowBottomSheetFragment
import com.pocket.app.home.topics.TopicsAdapter
import com.pocket.app.premium.Premium
import com.pocket.app.reader.queue.InitialQueueType
import com.pocket.sdk.util.AbsPocketFragment
import com.pocket.ui.util.DimenUtil
import com.pocket.util.android.FormFactor
import com.pocket.util.android.navigateSafely
import com.pocket.util.android.view.InstantChangeItemAnimator
import com.pocket.util.collectWhenResumed
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class HomeFragment : AbsPocketFragment() {

    @Inject lateinit var premium: Premium

    private val viewModel: HomeViewModel by viewModels()
    private val recentSavesViewModel: RecentSavesViewModel by viewModels()

    private var _binding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding
        get() = _binding!!

    private var savesItemDecorator: HorizontalSpacingDecorator? = null

    override fun getScreenIdentifierString(): String = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showsDialog = false
    }

    override fun onCreateViewImpl(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        binding.recentSavesLayout.recentSavesViewModel = recentSavesViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEventObserver()
        setupRecyclerViews()
        setupBookAnimation()
        setupSwipeRefreshListener()
        setupAnalytics()
        viewModel.onInitialized()
        recentSavesViewModel.onInitialized()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onUserReturned()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @Suppress("MagicNumber")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        addSavesItemDecorator(250)
    }

    private fun setupEventObserver() {
        viewModel.events.collectWhenResumed(viewLifecycleOwner) {
            handleEvent(it)
        }
        recentSavesViewModel.events.collectWhenResumed(viewLifecycleOwner) {
            handleEvent(it)
        }
    }

    private fun handleEvent(event: Home.Event) {
        when (event) {
            is Home.Event.GoToReader -> {
                    findNavController().navigateSafely(
                        HomeFragmentDirections.goToReader(
                            event.url,
                            InitialQueueType.Empty,
                            0
                        )
                    )
            }
            is Home.Event.ShowRecommendationOverflow -> {
                RecommendationOverflowBottomSheetFragment.newInstance(
                    url = event.url,
                    title = event.title,
                    corpusRecommendationId = event.corpusRecommendationId
                )
                    .show(
                        childFragmentManager,
                        RecommendationOverflowBottomSheetFragment::class.simpleName
                    )
            }
            is Home.Event.ShowSaveOverflow -> {
                RecentSavesOverflowFragment.newInstance(event.item, event.itemPosition)
                    .show(
                        childFragmentManager,
                        RecentSavesOverflowFragment::class.simpleName
                    )
            }
            is Home.Event.GoToMyList -> {
                findNavController().navigateSafely(
                    HomeFragmentDirections.goToSaves()
                )
            }
            is Home.Event.GoToSlateDetails -> {
                findNavController().navigateSafely(
                    HomeFragmentDirections.goToSlateDetails(event.index)
                )
            }
            is Home.Event.GoToTopicDetails -> {
                findNavController().navigateSafely(
                    HomeFragmentDirections.goToTopicDetails(event.topicId)
                )
            }
            is Home.Event.GoToPremium -> {
                premium.showPremiumForUserState(requireActivity(), null)
            }
            is Home.Event.GoToSignIn -> {
                AuthenticationActivity.startActivity(requireContext(), true)
            }
        }
    }

    private fun addSavesItemDecorator(delay: Long = 0) {
        // annoying race condition forces us to post delay slightly to give the device
        // time to finish a configuration change and re-measure view widths
        Handler(Looper.getMainLooper()).postDelayed({
            // because we call this in onConfigurationChanged, make sure the binding is not null
            _binding?.let { binding ->
                // this extra padding is mostly noticeable on tablets in landscape.
                // it allows the recent saves to scroll offscreen while making the
                // start and end margins line up with the rest of the views on screen
                val extraPadding = max(
                    (binding.root.width - resources.getDimension(R.dimen.home_max_width)) / 2f,
                    0f
                )
                savesItemDecorator?.let { binding.recentSavesLayout.savesRecyclerView.removeItemDecoration(it) }
                savesItemDecorator = HorizontalSpacingDecorator(
                    extraMarginOnFirstAndLast = DimenUtil.pxToDp(requireContext(), extraPadding)
                )
                savesItemDecorator?.let { binding.recentSavesLayout.savesRecyclerView.addItemDecoration(it) }
            }
        }, delay)
    }

    private fun setupRecyclerViews() {
        addSavesItemDecorator()
        val savesImpressionScrollListener = ViewableImpressionScrollListener(viewLifecycleOwner)
        val savesAdapter = RecentSavesAdapter(
            viewLifecycleOwner = viewLifecycleOwner,
            viewModel = recentSavesViewModel,
            impressionScrollListener = savesImpressionScrollListener,
        )
        binding.recentSavesLayout.savesRecyclerView.addOnScrollListener(savesImpressionScrollListener)
        binding.recentSavesLayout.savesRecyclerView.adapter = savesAdapter
        binding.recentSavesLayout.savesRecyclerView.itemAnimator = InstantChangeItemAnimator()
        savesAdapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                // using _binding because of possible null pointer
                val layoutManager = _binding?.recentSavesLayout?.savesRecyclerView?.layoutManager
                        as? LinearLayoutManager

                if (positionStart == 0 && layoutManager?.findFirstVisibleItemPosition() == 0) {
                    layoutManager.scrollToPosition(0)
                }
            }
        })

        val slateImpressionScrollListener = ViewableImpressionScrollListener(viewLifecycleOwner)
        binding.scrollView.setOnScrollChangeListener(slateImpressionScrollListener)
        binding.slatesRecyclerView.adapter = SlatesAdapter(
            viewLifecycleOwner = viewLifecycleOwner,
            viewModel = viewModel,
            isTablet = FormFactor.isTablet(context),
            impressionScrollListener = slateImpressionScrollListener
        )
        binding.slatesRecyclerView.itemAnimator = null

        binding.topicsRecyclerView.adapter = TopicsAdapter(
            viewLifecycleOwner = viewLifecycleOwner,
            viewModel = viewModel,
        )
    }

    private fun setupBookAnimation() {
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

    private fun setupSwipeRefreshListener() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.onSwipedToRefresh()
        }
    }

    private fun setupAnalytics() {
        binding.recentSavesLayout.recentSavesSeeAllLayout.setUiEntityType(UiEntityable.Type.BUTTON)
        val impressionScrollListener = ViewableImpressionScrollListener(viewLifecycleOwner)
        impressionScrollListener.track(
            binding.signInBanner,
            binding.signInBanner,
            viewModel::onSignInBannerViewed,
        )
        binding.scrollView.setOnScrollChangeListener(impressionScrollListener)
        // Also recheck for impressions any time we update UI state and might show/hide something.
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect {
                yield() // Like Handler.post(), lets the UI finish loading before we check.
                binding.scrollView.let {
                    val x = it.scrollX
                    val y = it.scrollY
                    impressionScrollListener.onScrollChange(it, x, y, x, y)
                }
            }
        }
    }
}
