package com.pocket.app.reader.internal.article.textsettings.fontSettings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.ideashower.readitlater.databinding.FragmentFontSettingsBottomSheetBinding
import com.pocket.app.premium.Premium
import com.pocket.app.reader.internal.article.textsettings.TextSettingsBottomSheetFragment
import com.pocket.sdk.util.AbsPocketBottomSheetDialogFragment
import com.pocket.util.collectWhenResumed
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FontSettingsBottomSheetFragment : AbsPocketBottomSheetDialogFragment() {

    @Inject lateinit var premium: Premium

    private val viewModel: FontSettingsBottomSheetViewModel by viewModels()

    private var _binding: FragmentFontSettingsBottomSheetBinding? = null
    private val binding: FragmentFontSettingsBottomSheetBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFontSettingsBottomSheetBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupEventListener()
        viewModel.onInitialized()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        binding.list.adapter = FontSettingsAdapter(
            viewLifecycleOwner = viewLifecycleOwner,
            viewModel = viewModel,
        )
    }

    private fun setupEventListener() {
        viewModel.events.collectWhenResumed(viewLifecycleOwner) { event ->
            when (event) {
                is FontSettings.Event.ReturnToTextSettings -> {
                    TextSettingsBottomSheetFragment.newInstance().show(
                        parentFragmentManager,
                        TextSettingsBottomSheetFragment::class.simpleName
                    )
                    dismiss()
                }
                is FontSettings.Event.GoToPremium -> {
                    premium.showPremiumForUserState(
                        activity,
                        null
                    )
                    dismiss()
                }
            }
        }
    }

    companion object {
        fun newInstance(): FontSettingsBottomSheetFragment = FontSettingsBottomSheetFragment()
    }
}