package com.pocket.app.reader.internal.collection

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.FragmentCollectionBinding
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.CollectionEvents
import com.pocket.app.auth.AuthenticationActivity
import com.pocket.app.home.slates.overflow.RecommendationOverflowBottomSheetFragment
import com.pocket.app.reader.Reader
import com.pocket.app.reader.ReaderFragment
import com.pocket.sdk.tts.Listen
import com.pocket.sdk.util.AbsPocketFragment
import com.pocket.sdk.util.MarkdownFormatter
import com.pocket.util.android.FormFactor
import com.pocket.util.android.navigateSafely
import com.pocket.util.android.view.InstantChangeItemAnimator
import com.pocket.util.collectWhenResumed
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CollectionFragment : AbsPocketFragment(), Reader.NavigationEventHandler {

    @Inject lateinit var listen: Listen
    @Inject lateinit var tracker: Tracker

    private val navController: NavController?
        get() = (parentFragment as? NavHostFragment)?.navController

    private val readerFragment: ReaderFragment?
        get() = parentFragment?.parentFragment as? ReaderFragment

    private val viewModel: CollectionViewModel by viewModels()

    private val args: CollectionFragmentArgs by navArgs()

    private var _binding: FragmentCollectionBinding? = null
    private val binding: FragmentCollectionBinding
        get() = _binding!!

    private lateinit var markdown: MarkdownFormatter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showsDialog = false
    }

    override fun onCreateViewImpl(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCollectionBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Navigation", "CollectionFragment")
        tracker.track(CollectionEvents.screenView())
        markdown = MarkdownFormatter(requireContext()) { _, url -> readerFragment?.openUrl(url) }
        setupUiStateListener()
        setupToolbar()
        setupRecyclerView()
        setupEventListener()
        viewModel.onInitialized(args.url)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun handleNavigationEvent(event: Reader.NavigationEvent) {
        if (event.addToBackstack) {
            when (event) {
                is Reader.NavigationEvent.GoToArticle ->
                    navController?.navigateSafely(CollectionFragmentDirections.enterArticle(event.url))
                is Reader.NavigationEvent.GoToCollection ->
                    navController?.navigateSafely(CollectionFragmentDirections.enterCollection(event.url))
                is Reader.NavigationEvent.GoToOriginalWeb ->
                    navController?.navigateSafely(CollectionFragmentDirections.enterOriginalWeb(event.url))
            }
        } else {
            when (event) {
                is Reader.NavigationEvent.GoToArticle ->
                    navController?.navigateSafely(CollectionFragmentDirections.switchToArticle(event.url))
                is Reader.NavigationEvent.GoToCollection ->
                    navController?.navigateSafely(CollectionFragmentDirections.switchToCollection(event.url))
                is Reader.NavigationEvent.GoToOriginalWeb ->
                    navController?.navigateSafely(CollectionFragmentDirections.switchToOriginalWeb(event.url))
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setupToolbar(
            toolbarEvents = viewModel.toolbar.toolbarEvents,
            lifecycleOwner = viewLifecycleOwner,
            readerFragment = readerFragment,
            listen = listen,
            url = args.url,
            toolbarInteractions = viewModel.toolbar,
            toolbarOverflowInteractions = viewModel.toolbar,
            toolbarUiStateHolder = viewModel.toolbar,
        )
    }

    private fun setupUiStateListener() {
        viewModel.uiState.collectWhenResumed(viewLifecycleOwner) { uiState ->
            uiState.intro?.let {
                binding.intro.setMovementMethodForLinks(true)
                binding.intro.text = markdown.format(it)
            }
        }
    }

    private fun setupRecyclerView() {
        binding.storyList.adapter = CollectionStoryAdapter(
            viewLifecycleOwner = viewLifecycleOwner,
            viewModel = viewModel,
            markdown = markdown,
            corpusRecommendationId = args.url,
        )
        binding.storyList.itemAnimator = InstantChangeItemAnimator()

        if (FormFactor.isTablet(context)) {
            binding.storyList.layoutManager = GridLayoutManager(context, 2)
            binding.storyList.addItemDecoration(CollectionGridSpacingDecorator())
        } else {
            binding.storyList.addItemDecoration(CollectionSpacingDecorator())
        }
    }

    private fun setupEventListener() {
        viewModel.events.collectWhenResumed(viewLifecycleOwner) { event ->
            when (event) {
                is CollectionScreen.Event.ShowOverflowBottomSheet -> {
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
                is CollectionScreen.Event.OpenUrl -> {
                    readerFragment?.openUrl(event.url, event.queueManager)
                }
                is CollectionScreen.Event.ShowSavedToast -> {
                    Toast.makeText(context, getString(R.string.ts_add_added), Toast.LENGTH_SHORT).show()
                }
                is CollectionScreen.Event.ShowArchivedToast -> {
                    Toast.makeText(context, getString(R.string.ts_item_archived), Toast.LENGTH_SHORT).show()
                }
                is CollectionScreen.Event.ShowReAddedToast -> {
                    Toast.makeText(context, getString(R.string.ts_item_readded), Toast.LENGTH_SHORT).show()
                }
                is CollectionScreen.Event.GoToSignIn -> {
                    AuthenticationActivity.startActivity(requireContext(), true)
                }
            }
        }
    }
}