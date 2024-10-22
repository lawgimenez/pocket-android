package com.pocket.app.reader.internal.article.textsettings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.viewModels
import com.ideashower.readitlater.databinding.FragmentTextSettingsBottomSheetBinding
import com.pocket.app.premium.Premium
import com.pocket.app.reader.internal.article.DisplaySettingsManager
import com.pocket.app.reader.internal.article.textsettings.fontSettings.FontSettingsBottomSheetFragment
import com.pocket.app.settings.Brightness
import com.pocket.app.settings.SystemDarkTheme
import com.pocket.app.settings.Theme
import com.pocket.sdk.util.AbsPocketActivity
import com.pocket.sdk.util.AbsPocketBottomSheetDialogFragment
import com.pocket.ui.view.menu.ThemeToggle.ThemeChoice
import com.pocket.util.android.ViewUtil
import com.pocket.util.collectWhenResumed
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TextSettingsBottomSheetFragment : AbsPocketBottomSheetDialogFragment() {

    @Inject lateinit var displaySettingsManager: DisplaySettingsManager
    @Inject lateinit var systemDarkTheme: SystemDarkTheme
    @Inject lateinit var theme: Theme
    @Inject lateinit var premium: Premium

    private val viewModel: TextSettingsBottomSheetViewModel by viewModels()

    private var _binding: FragmentTextSettingsBottomSheetBinding? = null
    private val binding: FragmentTextSettingsBottomSheetBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTextSettingsBottomSheetBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEventListener()
        setupView()
        viewModel.onInitialized()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupEventListener() {
        viewModel.events.collectWhenResumed(viewLifecycleOwner) { event ->
            when (event) {
                is TextSettingsBottomSheet.Event.ShowFontChangeBottomSheet -> {
                    FontSettingsBottomSheetFragment.newInstance().show(
                        parentFragmentManager,
                        FontSettingsBottomSheetFragment::class.simpleName
                    )
                    dismiss()
                }
                is TextSettingsBottomSheet.Event.ShowPremiumScreen -> {
                    premium.showPremiumForUserState(
                        activity,
                        null
                    )
                    dismiss()
                }
            }
        }
    }

    private fun setupView() {
        binding.settingsView.bind().clear()
            .brightness(0.5f)
            .brightnessListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    viewModel.onBrightnessChanged(progress)
                    (activity as? AbsPocketActivity)?.let { Brightness.applyBrightnessIfSet(it) }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
            .fontSizeUpClick { viewModel.onFontSizeUpClicked() }
            .fontSizeDownClick { viewModel.onFontSizeDownClicked() }
            .lineHeightUpClick { viewModel.onLineHeightUpClicked() }
            .lineHeightDownClick { viewModel.onLineHeightDownClicked() }
            .marginUpClick { viewModel.onMarginUpClicked() }
            .marginDownClick { viewModel.onMarginDownClicked() }
            .premiumUpgradeClick { viewModel.onPremiumUpgradeClicked() }
            .fontChangeTypeface(displaySettingsManager.currentFont.getTypeface(context))
            .fontChangeClick { viewModel.onFontChangeClicked() }
            .theme().apply {
                listener { _: View?, value: ThemeChoice? ->
                    when (value) {
                        ThemeChoice.LIGHT -> viewModel.onLightThemeClicked()
                        ThemeChoice.DARK -> viewModel.onDarkThemeClicked()
                        else -> viewModel.onSystemThemeClicked()
                    }
                    // manually refresh the theme for this bottom sheet
                    ViewUtil.refreshDrawableStateDeep(binding.root)
                }
                theme(
                    when {
                        systemDarkTheme.isOn -> ThemeChoice.AUTO
                        theme.get() == Theme.LIGHT -> ThemeChoice.LIGHT
                        else -> ThemeChoice.DARK
                    }
                )
                availableThemes(
                    ThemeChoice.LIGHT,
                    ThemeChoice.DARK,
                    ThemeChoice.AUTO
                )
            }
    }

    companion object {
        fun newInstance(): TextSettingsBottomSheetFragment = TextSettingsBottomSheetFragment()
    }
}