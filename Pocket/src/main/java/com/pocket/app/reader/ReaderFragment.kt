package com.pocket.app.reader

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.FragmentReaderBinding
import com.pocket.app.reader.queue.EmptyQueueManager
import com.pocket.app.reader.queue.QueueManager
import com.pocket.sdk.util.AbsPocketFragment
import com.pocket.sdk.util.view.UpDownAnimator
import com.pocket.util.collectWhenResumed
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReaderFragment : AbsPocketFragment() {

    private val viewModel: ReaderViewModel by viewModels()

    private var _binding: FragmentReaderBinding? = null
    private val binding: FragmentReaderBinding
        get() = _binding!!

    private val args: ReaderFragmentArgs by navArgs()

    private val navHostFragment: NavHostFragment?
        get() = childFragmentManager.findFragmentById(R.id.fragmentContainer) as? NavHostFragment

    private val navController: NavController?
        get() = navHostFragment?.navController

    private val currentFragment: Fragment?
        get() = navHostFragment?.childFragmentManager?.primaryNavigationFragment

    var previousNextAnimator: UpDownAnimator? = null

    val hasNext: Boolean
        get() = viewModel.hasNext

    val hasPrevious: Boolean
        get() = viewModel.hasPrevious

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showsDialog = false
    }

    override fun onCreateViewImpl(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentReaderBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEventObserver()
        if (savedInstanceState == null) {
            // post to make sure navigation component is ready
            Handler(Looper.getMainLooper()).post {
                viewModel.onInitialized(args.url, args.queueType, args.queueStartingIndex)
            }
        } else {
            // Android will restore the correct child fragment for us.
        }
        previousNextAnimator = UpDownAnimator(binding.previousNextLayout, UpDownAnimator.Direction.DOWN)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onBackPressed(): Boolean {
        Log.d("Navigation", "ReaderFragment onBackPressed")
        viewModel.onBackstackPopped()
        // internal nav controller
        if (navController?.popBackStack() != true) {
            // external nav controller
            findNavController().popBackStack()
        }
        return true
    }

    private fun setupEventObserver() {
        viewModel.navigationEvents.collectWhenResumed(viewLifecycleOwner) { event ->
            Log.d(
                "Navigation",
                "Navigation event collected.  Current Fragment:" +
                        " ${currentFragment?.let { it::class.simpleName } ?: "null"}"
            )
            (currentFragment as? Reader.NavigationEventHandler)?.handleNavigationEvent(event)
            previousNextAnimator?.show()
        }
    }

    fun openUrl(
        url: String,
        queueManager: QueueManager? = EmptyQueueManager(),
        forceOpenInWebView: Boolean = false,
    ) {
        viewModel.openUrl(url, queueManager, forceOpenInWebView)
    }

    fun onPreviousClicked() {
        viewModel.onPreviousClicked()
    }

    fun onNextClicked() {
        viewModel.onNextClicked()
    }
}