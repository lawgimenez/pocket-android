package com.pocket.app.home.slates.overflow.report

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.FragReportItemBottomSheetBinding
import com.pocket.analytics.*
import com.pocket.sdk.util.AbsPocketBottomSheetDialogFragment
import com.pocket.ui.view.notification.PktSnackbar
import com.pocket.util.android.hideKeyboard
import com.pocket.util.collectWhenResumed
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReportItemBottomSheetFragment : AbsPocketBottomSheetDialogFragment() {

    @Inject
    lateinit var tracker: Tracker

    private val viewModel by viewModels<ReportItemBottomSheetViewModel>()

    private var _binding: FragReportItemBottomSheetBinding? = null
    private val binding: FragReportItemBottomSheetBinding
        get() = _binding!!

    private lateinit var url: String
    private var corpusRecommendationId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragReportItemBottomSheetBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEventObserver()
        setupTextWatcher()
        setupEditText()
        viewModel.onInitialized(url, corpusRecommendationId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupEditText() {
        binding.otherEditText.setOnTouchListener { view, event ->
            view.parent.requestDisallowInterceptTouchEvent(true)
            view.onTouchEvent(event)
            true
        }
    }

    private fun setupTextWatcher() {
        binding.otherEditText.doOnTextChanged { text, _, _, _ ->
            viewModel.onOtherTextChanged(text.toString())
        }
    }

    private fun setupEventObserver() {
        viewModel.events.collectWhenResumed(viewLifecycleOwner) { event ->
            when (event) {
                is ReportItemBottomSheetViewModel.Event.ShowToastAndClose -> {
                    PktSnackbar.make(
                        activity,
                        PktSnackbar.Type.DEFAULT_DISMISSABLE,
                        resources.getText(R.string.report_item_submit_text),
                        null
                    ).apply {
                        bind().title(resources.getText(R.string.report_item_submit_title))
                        show()
                    }

                    dismiss()
                }
                is ReportItemBottomSheetViewModel.Event.HideKeyboard -> {
                    hideKeyboard()
                }
            }
        }
    }

    companion object {
        fun newInstance(
            url: String,
            corpusRecommendationId: String?,
        ): ReportItemBottomSheetFragment =
            ReportItemBottomSheetFragment().apply {
                this.url = url
                this.corpusRecommendationId = corpusRecommendationId
            }
    }
}