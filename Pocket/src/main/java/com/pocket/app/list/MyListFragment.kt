package com.pocket.app.list

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.FragMyListBinding
import com.pocket.analytics.Tracker
import com.pocket.analytics.ViewableImpressionScrollListener
import com.pocket.app.auth.AuthenticationActivity
import com.pocket.app.auth.FxaFeature
import com.pocket.app.help.Help
import com.pocket.app.list.add.AddUrlBottomSheetFragment
import com.pocket.app.list.bulkedit.BulkEditListItemAnimator
import com.pocket.app.list.bulkedit.BulkEditOverflowBottomSheetFragment
import com.pocket.app.list.filter.FilterBottomSheetFragment
import com.pocket.app.list.list.MyListAdapter
import com.pocket.app.list.list.MyListPagingScrollListener
import com.pocket.app.list.list.loading.SkeletonListFadeAnimator
import com.pocket.app.list.list.overflow.ItemOverflowBottomSheetFragment
import com.pocket.app.list.notes.Notes
import com.pocket.app.list.search.RecentSearchAdapter
import com.pocket.app.list.tags.TagBottomSheetFragment
import com.pocket.app.premium.Premium
import com.pocket.app.reader.queue.InitialQueueType
import com.pocket.app.settings.Theme
import com.pocket.app.share.ShareDialogFragment
import com.pocket.app.share.show
import com.pocket.sdk.Pocket
import com.pocket.sdk.api.generated.enums.*
import com.pocket.sdk.tts.Listen
import com.pocket.sdk.util.AbsPocketFragment
import com.pocket.sdk.util.dialog.AlertMessaging
import com.pocket.sdk2.analytics.context.Interaction
import com.pocket.util.android.hideKeyboard
import com.pocket.util.android.navigateSafely
import com.pocket.util.android.repeatOnResumed
import com.pocket.util.android.showKeyboard
import com.pocket.util.android.view.ContinueReadingView
import com.pocket.util.collectWhenResumed
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class MyListFragment : AbsPocketFragment() {

    @Inject lateinit var listen: Listen
    @Inject lateinit var tracker: Tracker
    @Inject lateinit var fxaFeature: FxaFeature
    @Inject lateinit var pocket: Pocket
    @Inject lateinit var bulkEditListItemAnimator: BulkEditListItemAnimator
    @Inject lateinit var premium: Premium
    @Inject lateinit var theme: Theme
    @Inject lateinit var continueReading: ContinueReading

    private val viewModel: MyListViewModel by viewModels()

    private var _binding: FragMyListBinding? = null
    private val binding: FragMyListBinding
        get() = _binding!!

    // used to remember the scroll position when coming back to this screen
    private var recyclerViewState: Parcelable? = null

    override fun getScreenIdentifierString(): String = "saves"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showsDialog = false
    }

    override fun onCreateViewImpl(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragMyListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListView()
        setupSearchList()
        setupNotesList()
        fxaFeature.showMigrationMessage(activity)
        setupUiListeners()
        SkeletonListFadeAnimator(binding.skeletonList, viewLifecycleOwner, viewModel)
        setupNavigationEventListener()
        binding.addButton.setSideMarginEnd()
        Handler(Looper.getMainLooper()).post {
            (_binding?.listRecyclerView?.layoutManager as? LinearLayoutManager)?.onRestoreInstanceState(
                recyclerViewState
            )
        }
    }

    override fun onResume() {
        super.onResume()
        setupContinueReading()
    }

    override fun onPause() {
        super.onPause()
        binding.continueReadingSnackBarLayout.removeAllViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerViewState = (binding.listRecyclerView.layoutManager as? LinearLayoutManager)?.onSaveInstanceState()
        _binding = null
        bulkEditListItemAnimator.reset()
    }

    override fun onBackPressed(): Boolean {
        val backClickHandled = viewModel.onBackButtonClicked()
        if (!backClickHandled) activity?.finish()
        return true
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun setupNavigationEventListener() {
        viewModel.navigationEvents.collectWhenResumed(viewLifecycleOwner) { event ->
            when (event) {
                is MyListNavigationEvent.ShowAddUrlBottomSheet -> {
                    AddUrlBottomSheetFragment.newInstance().show(
                        childFragmentManager,
                        AddUrlBottomSheetFragment::class.simpleName
                    )
                }
                is MyListNavigationEvent.ShowTagBottomSheet -> {
                    TagBottomSheetFragment.newInstance(viewModel.savesTab)
                        .show(
                            childFragmentManager,
                            TagBottomSheetFragment::class.simpleName
                        )
                }
                is MyListNavigationEvent.ShowFilterBottomSheet -> {
                    FilterBottomSheetFragment.newInstance(viewModel.savesTab)
                        .show(
                            childFragmentManager,
                            FilterBottomSheetFragment::class.simpleName
                        )
                }
                is MyListNavigationEvent.ShowBulkEditOverflowBottomSheet -> {
                    BulkEditOverflowBottomSheetFragment.newInstance(event.items, viewModel.savesTab) {
                        viewModel.onBulkEditFinished()
                    }.show(
                        childFragmentManager,
                        BulkEditOverflowBottomSheetFragment::class.simpleName
                    )
                }
                is MyListNavigationEvent.GoToListen -> {
                    listen.trackedControls(binding.listenChip, CxtUi.TOOLBAR).on()
                    absPocketActivity.expandListenUi()
                }
                is MyListNavigationEvent.GoToSignIn -> {
                    context?.let { context -> AuthenticationActivity.startActivity(context, true) }
                }
                is MyListNavigationEvent.ShowSyncError -> {
                    AlertMessaging.showConnectionDependantError(
                        absPocketActivity,
                        event.error,
                        Help.Type.SYNC,
                        false,
                        null,
                        R.string.dg_sync_error_t,
                        0
                    )
                }
                is MyListNavigationEvent.GoToReader -> {
                    findNavController().navigateSafely(
                        MyListFragmentDirections.goToReader(
                            event.item.id_url?.url!!,
                            InitialQueueType.SavesList,
                            event.startingIndex
                        )
                    )
                }
                is MyListNavigationEvent.ShowShare -> {
                    ShareDialogFragment.show(childFragmentManager, event.item)
                }
                is MyListNavigationEvent.ShowItemOverflow -> {
                    ItemOverflowBottomSheetFragment.newInstance(event.item, viewModel.savesTab)
                        .show(childFragmentManager, ItemOverflowBottomSheetFragment::class.simpleName)
                }
                is MyListNavigationEvent.SetSearchFocus -> {
                    if (binding.searchEditText.requestFocus()) {
                        showKeyboard(binding.searchEditText)
                    }
                }
                is MyListNavigationEvent.CloseKeyboard -> {
                    binding.dummyFocus.requestFocus()
                    hideKeyboard()
                }
                is MyListNavigationEvent.UpdateSearch -> {
                    binding.searchEditText.setText(event.searchText)
                    binding.searchEditText.setSelection(event.searchText.length)
                }
                is MyListNavigationEvent.TrackSearchAnalytics -> {
                    tracker.trackEngagement(
                        binding.searchEditText,
                        value = event.searchText,
                    )
                }
            }
        }
    }

    private fun setupUiListeners() {
        binding.bulkEditSnackBar.setOnReAddClickedListener {
            viewModel.onBulkReAddClicked()
        }
        binding.bulkEditSnackBar.setOnArchiveClickedListener {
            viewModel.onBulkArchiveClicked()
        }
        binding.bulkEditSnackBar.setOnDeleteClickedListener {
            viewModel.onBulkDeleteClicked()
        }
        binding.bulkEditSnackBar.setOnOverflowClickedListener {
            viewModel.onBulkEditOverflowClicked()
        }
        binding.bulkEditSnackBar.setOnTextClickListener { viewModel.onBulkEditSelectAllClicked() }
        binding.refreshLayout.setOnRefreshListener { viewModel.onPulledToRefresh() }
        binding.searchEditText.addTextChangedListener {
            viewModel.onSearchTextChanged(it.toString())
        }
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.onSearchDoneClicked()
                binding.dummyFocus.requestFocus()
                hideKeyboard()
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun setupListView() {
        val saveImpressionScrollListener = ViewableImpressionScrollListener(viewLifecycleOwner)
        val myListAdapter = MyListAdapter(
            viewLifecycleOwner = viewLifecycleOwner,
            tracker = tracker,
            context = requireContext(),
            viewModel = viewModel,
            bulkEditListItemAnimator = bulkEditListItemAnimator,
            theme = theme,
            recyclerView = binding.listRecyclerView,
            saveImpressionScrollListener = saveImpressionScrollListener,
        )
        binding.listRecyclerView.apply {
            addOnScrollListener(MyListPagingScrollListener(
                myListAdapter,
                viewModel
            ))
            addOnScrollListener(saveImpressionScrollListener)
            adapter = myListAdapter
        }
    }

    private fun setupSearchList() {
        binding.recentSearchesRecyclerView.adapter = RecentSearchAdapter(
            viewModel,
            viewLifecycleOwner,
        )
    }

    private fun setupNotesList() {
        binding.notes.setContent {
            Notes(this::findNavController)
        }
    }

    // this could be refactored to be in the view model
    @Suppress("MagicNumber")
    private fun setupContinueReading() {
        continueReading.checkForContinueReading { item ->
            val continueReadingView = ContinueReadingView.getInstance(
                context,
                item,
                continueReading
            ) {
                findNavController().navigateSafely(
                    MyListFragmentDirections.goToReader(
                        item.given_url?.url!!,
                        InitialQueueType.Empty,
                        0
                    )
                )
            }
            // binding could be null since this is a callback
            _binding?.let { binding ->
                binding.continueReadingSnackBarLayout.addView(continueReadingView)
                continueReading.trackView(Interaction.on(continueReadingView))
                viewLifecycleOwner.repeatOnResumed {
                    // hide after 15 seconds
                    delay(15_000)
                    binding.continueReadingSnackBarLayout.removeAllViews()
                }
            }
        }
    }

    companion object {
        @JvmStatic fun newInstance(): MyListFragment {
            return MyListFragment()
        }
    }
}