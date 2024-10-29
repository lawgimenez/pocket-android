package com.pocket.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.ActivityMainBinding
import com.pocket.app.home.HomeFragment
import com.pocket.app.home.HomeFragmentDirections
import com.pocket.app.list.MyListFragment
import com.pocket.app.list.MyListFragmentDirections
import com.pocket.app.reader.queue.InitialQueueType
import com.pocket.app.settings.PrefsFragment
import com.pocket.app.settings.PrefsFragmentDirections
import com.pocket.app.settings.account.DeletedAccountConfirmationSnackbar
import com.pocket.sdk.api.generated.enums.CxtView
import com.pocket.sdk.tts.Listen
import com.pocket.sdk.tts.toTrack
import com.pocket.sdk.util.AbsPocketActivity
import com.pocket.sdk.util.AbsPocketFragment
import com.pocket.sdk.util.dialog.FetchingDialog
import com.pocket.ui.view.notification.PktSnackbar
import com.pocket.util.BackPressedUtil
import com.pocket.util.android.navigateSafely
import com.pocket.util.prefs.BooleanPreference
import com.pocket.util.prefs.Preferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AbsPocketActivity() {

    @Inject
    lateinit var preferences: Preferences
    @Inject
    lateinit var listen: Listen

    private val viewModel: MainViewModel by viewModels()

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding
        get() = _binding!!

    private val navHostFragment: NavHostFragment?
        get() = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? NavHostFragment

    private val navController: NavController?
        get() = navHostFragment?.navController

    private val currentFragment: Fragment?
        get() = navHostFragment?.childFragmentManager?.primaryNavigationFragment

    override fun getAccessType(): ActivityAccessRestriction = ActivityAccessRestriction.ANY

    override fun getActionViewName(): CxtView = (currentFragment as? AbsPocketFragment)?.actionViewName
        ?: CxtView.POCKET

    private var notificationPermissionDeniedPref: BooleanPreference? = null
    private var resultLauncher: ActivityResultLauncher<String>? = null
    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        setContentView(binding.root)
        setupNotificationPermissionPrompt()
        disableInteractionUntilDataIsFetched()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                setupEventsObserver()
            }
        }
        setupDestinationListener()
        handleDeepLink(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    @Deprecated("Use [OnBackInvokedCallback] or [OnBackPressedCallback] to handle back navigation instead.")
    override fun onBackPressed() {
        if (BackPressedUtil.onBackPressed(supportFragmentManager)) {
            return
        }
        if (navController?.popBackStack() != true) {
            // skip AbsPocketActivity.onBackPressed logic
            superBackPressed()
        }
    }

    // getting a lint warning even though I'm calling super.  Kotlin/java interoperability issue maybe?
    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        Handler(Looper.getMainLooper()).post {
            val destination = intent?.getIntExtra(
                EXTRA_DESTINATION,
                DeepLinkDestination.DEFAULT.ordinal
            ) ?: DeepLinkDestination.DEFAULT.ordinal
            if (destination != DeepLinkDestination.DEFAULT.ordinal) {
                navController?.popBackStack(navController!!.graph.startDestinationId, false)

                // give the navController a chance to finish popping the back stack
                Handler(Looper.getMainLooper()).post {
                    when (destination) {
                        DeepLinkDestination.HOME.ordinal -> viewModel.onHomeClicked()
                        DeepLinkDestination.SAVES.ordinal -> viewModel.onSavesClicked()
                        DeepLinkDestination.SETTINGS.ordinal -> viewModel.onSettingsClicked()
                        DeepLinkDestination.TOPIC_DETAILS.ordinal -> {
                            val topicId = intent?.getStringExtra(EXTRA_TOPIC_ID)
                            if (topicId != null) {
                                navController?.navigateSafely(
                                    HomeFragmentDirections.goToTopicDetails(topicId)
                                )
                                // Make sure they come back to Home after closing the topic page.
                                viewModel.onHomeClicked()
                            }
                        }
                        DeepLinkDestination.READER.ordinal -> {
                            val url = intent?.getStringExtra(EXTRA_URL)
                            val openListen = intent?.getBooleanExtra(EXTRA_OPEN_LISTEN, false)
                            url?.let { viewModel.onReaderDeepLinkReceived(url, openListen ?: false) }
                        }
                    }
                }
            }
        }
    }

    private suspend fun setupEventsObserver() {
        viewModel.events.onSubscription {
            viewModel.onEventCollectionStarted(this)
        }.collect { event ->
            when (event) {
                is MainViewModel.Event.GoToHome -> {
                    val directions = when (currentFragment) {
                        is MyListFragment -> MyListFragmentDirections.goToHome()
                        is PrefsFragment -> PrefsFragmentDirections.goToHome()
                        else -> null
                    }
                    directions?.let { navController?.navigateSafely(it) }
                }
                is MainViewModel.Event.GoToSaves -> {
                    val directions = when (currentFragment) {
                        is HomeFragment -> HomeFragmentDirections.goToSaves()
                        is PrefsFragment -> PrefsFragmentDirections.goToSaves()
                        else -> null
                    }
                    directions?.let { navController?.navigateSafely(it) }
                }
                is MainViewModel.Event.GoToSettings -> {
                    val directions = when (currentFragment) {
                        is HomeFragment -> HomeFragmentDirections.goToSettings()
                        is MyListFragment -> MyListFragmentDirections.goToSettings()
                        else -> null
                    }
                    directions?.let { navController?.navigateSafely(it) }
                }
                is MainViewModel.Event.ShowProgress -> {
                    progressDialog = ProgressDialog(this@MainActivity)
                        .apply {
                            setTitle(R.string.dg_loading)
                        }
                        .also { it.show() }
                }
                is MainViewModel.Event.HideProgress -> progressDialog?.dismiss()
                is MainViewModel.Event.OpenReader -> {
                    val directions = when (currentFragment) {
                        is HomeFragment -> HomeFragmentDirections.goToReader(
                            event.url,
                            InitialQueueType.Empty,
                            0
                        )
                        is MyListFragment -> MyListFragmentDirections.goToReader(
                            event.url,
                            InitialQueueType.Empty,
                            0
                        )
                        is PrefsFragment -> PrefsFragmentDirections.goToReader(
                            event.url,
                            InitialQueueType.Empty,
                            0
                        )
                        else -> null
                    }
                    directions?.let { navController?.navigateSafely(it) }
                    if (event.openListen) {
                        event.item?.let {
                            listen.trackedControls(null, null)
                                .play(it.toTrack())
                        }
                        expandListenUi()
                    }
                }
                MainViewModel.Event.ShowDeletedAccountToast -> {
                    DeletedAccountConfirmationSnackbar.make(
                            this@MainActivity,
                            viewModel::onDeletedAccountExitSurveyClicked,
                        )
                            .show()
                    viewModel.onShowedDeletedAccountToast()
                }
                MainViewModel.Event.ShowBadCredentialsToast -> {
                    val snackbar = PktSnackbar.make(
                        this,
                        PktSnackbar.Type.DEFAULT_DISMISSABLE,
                        getString(R.string.dg_forced_logout_m),
                        null,
                    )
                    snackbar.show()
                    viewModel.onShowedBadCredentialsToast()
                }
            }
        }
    }

    private fun setupDestinationListener() {
        navController?.addOnDestinationChangedListener { _, navDestination, _ ->
            when (navDestination.id) {
                R.id.home -> {
                    viewModel.onNavigationDestinationChanged(MainViewModel.Destination.HOME)
                }
                R.id.saves -> {
                    viewModel.onNavigationDestinationChanged(MainViewModel.Destination.SAVES)
                }
                R.id.settings -> {
                    viewModel.onNavigationDestinationChanged(MainViewModel.Destination.SETTINGS)
                }
                else -> {
                    viewModel.onNavigationDestinationChanged(MainViewModel.Destination.OTHER)
                }
            }
        }
    }

    // Our screens don't have good states while initially fetching data. This could be
    // refactored in the future to the point that we don't need this blocking dialog.
    private fun disableInteractionUntilDataIsFetched() {
        if (!FetchingDialog.blockInteractionUntilFetched(this) {
                // When fetch is successful, hide the fetching background to show the nav host.
                onFetchComplete()
            }
        ) {
            // If data is already fetched, hide the fetching background to show the nav host.
            onFetchComplete()
        }
    }

    private fun onFetchComplete() {
        binding.fetchingBackground.visibility = View.GONE
        showNotificationPermissionsRequest()
    }

    private fun setupNotificationPermissionPrompt() {
        notificationPermissionDeniedPref = preferences.forApp(
            "notification_permission_requested",
            false
        )
        resultLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) {
                notificationPermissionDeniedPref?.set(true)
            }
        }
    }

    private fun showNotificationPermissionsRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
            && notificationPermissionDeniedPref?.get() == false
        ) {
            val dialog: AlertDialog = AlertDialog.Builder(this)
                .setTitle(R.string.notification_permission_primer_title)
                .setMessage(R.string.notification_permissions_primer_message)
                .setPositiveButton(R.string.notification_permissions_primer_positive_button) { _, _ ->
                    resultLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                .setNegativeButton(R.string.notification_permissions_primer_negative_button) { _, _ ->
                    notificationPermissionDeniedPref?.set(true)
                }
                .show()
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(getColor(com.pocket.ui.R.color.pkt_themed_grey_4))
        }
    }

    enum class DeepLinkDestination {
        DEFAULT,
        HOME,
        SAVES,
        SETTINGS,
        TOPIC_DETAILS,
        READER,
    }

    companion object {
        const val EXTRA_DESTINATION = "destination"
        // if we are opening an item there will be a url
        const val EXTRA_URL = "extraUrl"
        const val EXTRA_OPEN_LISTEN = "openListen"
        const val EXTRA_TOPIC_ID = "topicId"
    }
}
