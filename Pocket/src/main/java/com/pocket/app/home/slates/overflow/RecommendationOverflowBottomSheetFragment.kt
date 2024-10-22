package com.pocket.app.home.slates.overflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.ideashower.readitlater.databinding.FragRecommendationOverflowBottomSheetBinding
import com.pocket.app.home.slates.overflow.report.ReportItemBottomSheetFragment
import com.pocket.app.share.ShareDialogFragment
import com.pocket.sdk.util.AbsPocketBottomSheetDialogFragment
import com.pocket.util.collectWhenResumed
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecommendationOverflowBottomSheetFragment : AbsPocketBottomSheetDialogFragment() {

    private val viewModel by viewModels<RecommendationOverflowBottomSheetViewModel>()

    private var _binding: FragRecommendationOverflowBottomSheetBinding? = null
    private val binding: FragRecommendationOverflowBottomSheetBinding
        get() = _binding!!

    private lateinit var url: String
    private lateinit var title: String
    private var corpusRecommendationId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragRecommendationOverflowBottomSheetBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEventObserver()
        viewModel.onInitialized(
            url = url,
            title = title,
            corpusRecommendationId = corpusRecommendationId,
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupEventObserver() {
        viewModel.events.collectWhenResumed(viewLifecycleOwner) { event ->
            when (event) {
                RecommendationOverflowBottomSheetViewModel.Event.ShowShare -> {
                    ShareDialogFragment.show(parentFragmentManager, url, title = title)
                    dismiss()
                }
                RecommendationOverflowBottomSheetViewModel.Event.ShowReport -> {
                    ReportItemBottomSheetFragment.newInstance(url, corpusRecommendationId)
                        .show(parentFragmentManager, ReportItemBottomSheetFragment::class.simpleName)
                    dismiss()
                }
            }
        }
    }

    companion object {
        fun newInstance(
            url: String?,
            title: String?,
            corpusRecommendationId: String?,
        ): RecommendationOverflowBottomSheetFragment =
            RecommendationOverflowBottomSheetFragment().apply {
                this.url = url!!
                this.title = title ?: ""
                this.corpusRecommendationId = corpusRecommendationId
            }
    }
}