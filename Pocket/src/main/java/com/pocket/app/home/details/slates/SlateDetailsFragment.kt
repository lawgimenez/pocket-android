package com.pocket.app.home.details.slates

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
 * A details screen for slates.  Extends [DetailsFragment] which contains most of the logic
 */
@AndroidEntryPoint
class SlateDetailsFragment : DetailsFragment() {

    override val viewModel: SlateDetailsViewModel by viewModels()

    private val args: SlateDetailsFragmentArgs by navArgs()

    override fun getScreenIdentifierString(): String = "slateDetails"

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
        viewModel.onInitialized(args.index)
    }

    override fun goToReader(event: DetailsViewModel.Event.GoToReader) {
        findNavController().navigateSafely(
            SlateDetailsFragmentDirections.slateDetailsToReader(
                event.url,
                InitialQueueType.Empty,
                0
            )
        )
    }
}