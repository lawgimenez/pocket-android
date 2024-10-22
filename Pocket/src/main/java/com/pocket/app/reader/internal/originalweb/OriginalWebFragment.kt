package com.pocket.app.reader.internal.originalweb

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.FragmentOriginalWebBinding
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.OriginalWebViewEvents
import com.pocket.app.CustomTabs
import com.pocket.app.reader.Reader
import com.pocket.app.reader.ReaderFragment
import com.pocket.app.settings.SystemDarkTheme
import com.pocket.app.settings.Theme
import com.pocket.repository.ItemRepository
import com.pocket.sdk.util.AbsPocketFragment
import com.pocket.util.android.PendingIntentUtils
import com.pocket.util.android.drawable.toBitmap
import com.pocket.util.android.navigateSafely
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OriginalWebFragment : AbsPocketFragment(), Reader.NavigationEventHandler {

    @Inject lateinit var theme: Theme
    @Inject lateinit var systemDarkTheme: SystemDarkTheme
    @Inject lateinit var reader: Reader
    @Inject lateinit var tracker: Tracker
    @Inject lateinit var itemRepository: ItemRepository
    @Inject lateinit var customTabs: CustomTabs

    private val viewModel: OriginalWebViewModel by viewModels()

    private val args: OriginalWebFragmentArgs by navArgs()

    private var _binding: FragmentOriginalWebBinding? = null
    private val binding: FragmentOriginalWebBinding
        get() = _binding!!

    private val readerFragment: ReaderFragment?
        get() = parentFragment?.parentFragment as? ReaderFragment

    // The purpose of this variable is to track whether we are just arriving at
    // this screen and need to launch the custom tab, or if we are coming
    // back to this screen from the custom tab
    private var hasLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showsDialog = false
    }

    override fun onCreateViewImpl(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOriginalWebBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Navigation", "OriginalWebFragment")
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        hasLaunched = savedInstanceState?.getBoolean(OPENED) ?: hasLaunched
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(OPENED, hasLaunched)
    }

    override fun onResume() {
        super.onResume()
        if (hasLaunched) {
            handleResumeAction()
        } else {
            launchCustomTab()
        }
    }

    private fun handleResumeAction() {
        when (resumeAction) {
            ResumeAction.CLOSE -> {
                readerFragment?.onBackPressed()
            }
            ResumeAction.NEXT -> {
                readerFragment?.onNextClicked()
            }
            ResumeAction.PREVIOUS -> {
                readerFragment?.onPreviousClicked()
            }
            ResumeAction.SWITCH_TO_ARTICLE_VIEW -> {
                findNavController().navigateSafely(OriginalWebFragmentDirections.switchToArticle(args.url))
            }
        }
        resumeAction = ResumeAction.CLOSE
        urlCustomTabsWasLaunchedWith = ""
        resolvedUrlCustomTabsWasLaunchedWith = ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun launchCustomTab() {
        tracker.track(OriginalWebViewEvents.screenView())
        lifecycleScope.launch {
            val item = try {
                itemRepository.getDomainItem(args.url)
            } catch (e: Exception) {
                null
            }

            urlCustomTabsWasLaunchedWith = args.url
            resolvedUrlCustomTabsWasLaunchedWith = item?.resolvedUrl ?: ""
            if (customTabs.isBrowserSettingEnabled()) {
                val options = customTabs.getBrowserOptions()
                options.resetIfSelectionInvalid()
                if (customTabs.willShowChooser) {
                    options.pickPreferredBrowser()
                    val message = resources.getString(
                        R.string.original_web_changed_browser_setting,
                        options.selectedLabel,
                    )
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
                val preferredBrowser =  customTabs.preferredBrowserPackageName
                hasLaunched = true
                val customTabsIntent = CustomTabsIntent.Builder()
                    .addColors()
                    .addIcon()
                    .setShowTitle(true)
                    .addPreviousAndNext()
                    .setStartAnimations(requireContext(), R.anim.no_animation, R.anim.no_animation )
                    // exit animations don't seem to work
                    .setExitAnimations(requireContext(), R.anim.no_animation, R.anim.no_animation )
                    .build()
                if (preferredBrowser != null) {
                    // Override default system browser with the one configured in-app.
                    customTabsIntent.intent.setPackage(preferredBrowser)
                } else {
                    // Set the default system browser package name
                    // so that apps like youtube don't hijack our custom tab
                    customTabsIntent.intent.setPackage(getDefaultCustomTabsPackageName())
                }
                customTabsIntent.launchUrl(
                    requireContext(),
                    Uri.parse(args.url)
                )
            } else {
                val customTabPackageName = getDefaultCustomTabsPackageName()
                when {
                    customTabPackageName != null -> {
                        hasLaunched = true
                        val customTabsIntent = CustomTabsIntent.Builder()
                            .addColors()
                            .addIcon()
                            .setShowTitle(true)
                            .addPreviousAndNext()
                            .setStartAnimations(requireContext(), R.anim.no_animation, R.anim.no_animation)
                            // exit animations don't seem to work
                            .setExitAnimations(requireContext(), R.anim.no_animation, R.anim.no_animation)
                            .build()
                        // set the package name so that apps like youtube don't hijack our custom tab
                        customTabsIntent.intent.setPackage(customTabPackageName)
                        customTabsIntent.launchUrl(
                            requireContext(),
                            Uri.parse(args.url)
                        )
                    }
                    hasBrowser() -> {
                        // if the user doesn't have a browser that supports custom tabs, try to open in
                        // the browser without custom tabs
                        hasLaunched = true
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(args.url)))
                        return@launch
                    }
                    else -> {
                        // if the user has no browser, show an error toast
                        Toast.makeText(context, R.string.original_web_no_browser, Toast.LENGTH_LONG).show()
                        readerFragment?.onBackPressed()
                        return@launch
                    }
                }
            }
        }
    }

    // firefox does not implement the new methods, so just use the deprecated ones
    // for now
    private fun CustomTabsIntent.Builder.addColors(): CustomTabsIntent.Builder =
        setNavigationBarColor(when {
            theme.isDark(context) -> Theme.getStatusBarColor(Theme.DARK, context)
            else -> Theme.getStatusBarColor(Theme.LIGHT, context)
        })
        .setToolbarColor(when {
            theme.isDark(context) -> Theme.getStatusBarColor(Theme.DARK, context)
            else -> Theme.getStatusBarColor(Theme.LIGHT, context)
        })
        .setNavigationBarDividerColor(when {
            theme.isDark(context) -> Theme.getNavigationBarDividerColor(Theme.DARK, context)
            else -> Theme.getNavigationBarDividerColor(Theme.LIGHT, context)
        })

    private fun CustomTabsIntent.Builder.addIcon(): CustomTabsIntent.Builder =
        setActionButton(
            ContextCompat.getDrawable(requireContext(), com.pocket.ui.R.drawable.ic_pocket_menu)!!.toBitmap(),
            getString(com.pocket.ui.R.string.ic_pocket_menu),
            PendingIntent.getBroadcast(
                context,
                CUSTOM_TAB_REQUEST_CODE,
                Intent(context, CustomTabEventReceiver::class.java).setAction(ACTION_OPEN_MENU),
                PendingIntentUtils.addMutableFlag(0),
            ),
            true,
        )

    private fun CustomTabsIntent.Builder.addPreviousAndNext(): CustomTabsIntent.Builder {
        if ((readerFragment?.hasNext == true || readerFragment?.hasPrevious == true)
            && reader.isPreviousAndNextOn
        ) {
            setSecondaryToolbarViews(
                RemoteViews(
                    requireContext().packageName,
                    when {
                        readerFragment?.hasNext == true
                                && readerFragment?.hasPrevious == true -> if (theme.isDark(context)) {
                            R.layout.view_previous_next_bar_dark
                        } else {
                            R.layout.view_previous_next_bar_light
                        }
                        readerFragment?.hasNext == true -> if (theme.isDark(context)) {
                            R.layout.view_next_bar_dark
                        } else {
                            R.layout.view_next_bar_light
                        }
                        readerFragment?.hasPrevious == true -> if (theme.isDark(context)) {
                            R.layout.view_previous_bar_dark
                        } else {
                            R.layout.view_previous_bar_light
                        }
                        else -> throw Exception()
                    }),
                intArrayOf(R.id.previousItem, R.id.nextItem),
                PendingIntent.getBroadcast(
                    context,
                    CUSTOM_TAB_REQUEST_CODE,
                    Intent(context, CustomTabEventReceiver::class.java).setAction(
                        ACTION_PREVIOUS_NEXT_CLICKED
                    ),
                    PendingIntentUtils.addMutableFlag(0)
                )
            )
        }
        return this
    }

    private fun hasBrowser(): Boolean {
        return context?.packageManager?.queryIntentActivities(
            Intent()
                .setAction(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.fromParts("http", "", null)),
            0
        )?.isNotEmpty() ?: false
    }

    /**
     * Get the package name of the default browser if it supports custom tabs.
     */
    private fun getDefaultCustomTabsPackageName(): String? {
        return CustomTabsClient.getPackageName(requireContext(), null)
    }

    override fun handleNavigationEvent(event: Reader.NavigationEvent) {
        if (event.addToBackstack) {
            when (event) {
                is Reader.NavigationEvent.GoToArticle ->
                    findNavController().navigateSafely(OriginalWebFragmentDirections.enterArticle(event.url))
                is Reader.NavigationEvent.GoToCollection ->
                    findNavController().navigateSafely(OriginalWebFragmentDirections.enterCollection(event.url))
                is Reader.NavigationEvent.GoToOriginalWeb ->
                    findNavController().navigateSafely(OriginalWebFragmentDirections.enterOriginalWeb(event.url))
            }
        } else {
            when (event) {
                is Reader.NavigationEvent.GoToArticle ->
                    findNavController().navigateSafely(OriginalWebFragmentDirections.switchToArticle(event.url))
                is Reader.NavigationEvent.GoToCollection ->
                    findNavController().navigateSafely(OriginalWebFragmentDirections.switchToCollection(event.url))
                is Reader.NavigationEvent.GoToOriginalWeb ->
                    findNavController().navigateSafely(OriginalWebFragmentDirections.switchToOriginalWeb(event.url))
            }
        }
    }

    enum class ResumeAction {
        CLOSE,
        NEXT,
        PREVIOUS,
        SWITCH_TO_ARTICLE_VIEW
    }

    companion object {
        const val OPENED = "opened"
        const val CUSTOM_TAB_REQUEST_CODE = 555

        const val ACTION_OPEN_MENU = "pocket.tabs.open.menu"
        const val ACTION_PREVIOUS_NEXT_CLICKED = "pocket.tabs.previous.next"

        // hacky way to let the fragment know what it should do when the custom tab is closed
        var resumeAction: ResumeAction = ResumeAction.CLOSE
        var urlCustomTabsWasLaunchedWith: String = ""
        var resolvedUrlCustomTabsWasLaunchedWith: String = ""
    }
}