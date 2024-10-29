package com.pocket.app.home.details.topics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ideashower.readitlater.databinding.FragmentHomeDetailsBinding
import com.pocket.app.home.details.DetailsFragment
import com.pocket.app.home.details.DetailsViewModel
import com.pocket.app.reader.queue.InitialQueueType
import com.pocket.util.android.navigateSafely
import dagger.hilt.android.AndroidEntryPoint

/**
 * A details screen for topics.  Extends [DetailsFragment] which contains most of the logic
 */
@AndroidEntryPoint
class TopicDetailsFragment : DetailsFragment() {

    override val viewModel: TopicDetailsViewModel by viewModels()

    private val args: TopicDetailsFragmentArgs by navArgs()

    override fun getScreenIdentifierString(): String = "topicDetails"

    override fun onCreateViewImpl(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeDetailsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.onInitialized(args.topicId)
    }

    override fun goToReader(event: DetailsViewModel.Event.GoToReader) {
        findNavController().navigateSafely(
            TopicDetailsFragmentDirections.topicDetailsToReader(
                event.url,
                InitialQueueType.Empty,
                0
            )
        )
    }
}