package com.pocket.app.auth

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.FragmentAuthenticationBinding
import com.pocket.analytics.ImpressionComponent
import com.pocket.analytics.ImpressionRequirement
import com.pocket.analytics.ImpressionableInfoPageAdapter
import com.pocket.analytics.Tracker
import com.pocket.analytics.UiEntityType
import com.pocket.app.settings.account.DeletedAccountConfirmationSnackbar
import com.pocket.sdk.Pocket
import com.pocket.sdk.api.PocketServer
import com.pocket.sdk.api.generated.enums.CxtView
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier
import com.pocket.sdk.build.AppVersion
import com.pocket.sdk.dev.TeamTools
import com.pocket.sdk.util.AbsPocketFragment
import com.pocket.ui.view.info.InfoPage
import com.pocket.util.android.FormFactor
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import javax.inject.Inject

@AndroidEntryPoint
class AuthenticationFragment : AbsPocketFragment() {

    @Inject lateinit var tracker: Tracker
    @Inject lateinit var pocketServer: PocketServer
    @Inject lateinit var appVersion: AppVersion
    @Inject lateinit var pocket: Pocket

    private val viewModel: AuthenticationViewModel by viewModels(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<AuthenticationViewModel.Factory> {
                it.create(requireArguments().getBoolean(ARG_SKIP_ONBOARDING))
            }
        }
    )

    private var _binding: FragmentAuthenticationBinding? = null
    private val binding: FragmentAuthenticationBinding
        get() = _binding!!

    override fun getActionViewName(): CxtView {
        return CxtView.MOBILE
    }

    override fun onCreateViewImpl(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthenticationBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreatedImpl(view: View, savedInstanceState: Bundle?) {
        super.onViewCreatedImpl(view, savedInstanceState)
        setupIntroPager()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                setupEventListener()
            }
        }
        setCredentialsReceiverIntentFilterEnabled(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.onFragmentDestroyed()
        _binding = null
    }

    private suspend fun setupEventListener() {
        viewModel.events
            .onSubscription { viewModel.onEventCollectionStarted(this) }
            .collect { event ->
                when (event) {
                    Authentication.Event.Authenticate -> authenticate()
                    Authentication.Event.GoToDefaultScreen -> {
                        absPocketActivity.startDefaultActivity()
                        finish()
                    }
                    Authentication.Event.GoBack -> finish()
                    Authentication.Event.ShowErrorToast -> {
                        Toast.makeText(
                            activity,
                            R.string.dg_unexpected_m,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    Authentication.Event.DisableCredentialsCallbackIntentFilter -> {
                        setCredentialsReceiverIntentFilterEnabled(false)
                    }
                    Authentication.Event.OpenTeamTools -> {
                        TeamTools(absPocketActivity).show()
                    }
                    Authentication.Event.ShowDeletedAccountToast -> {
                        activity?.let { activity ->
                            DeletedAccountConfirmationSnackbar.make(activity).show()
                            viewModel.onShowedDeletedAccountToast()
                        }
                    }
                }
            }
    }

    private fun setupIntroPager() {
        binding.intro.bind().clear()
            .adapter(
                ImpressionableInfoPageAdapter(
                    requireContext(),
                    FormFactor.getWindowWidthPx(activity),
                    listOf(
                        InfoPage(
                            R.drawable.pkt_onboarding_pocket,
                            getString(R.string.onboarding_learn_more_1_title),
                            getString(R.string.onboarding_learn_more_1_text)
                        ),
                        InfoPage(
                            R.drawable.pkt_onboarding_treasure,
                            getString(R.string.onboarding_learn_more_2_title),
                            getString(R.string.onboarding_learn_more_2_text)
                        ),
                        InfoPage(
                            R.drawable.pkt_onboarding_quiet,
                            getString(R.string.onboarding_learn_more_3_title),
                            getString(R.string.onboarding_learn_more_3_text)
                        )
                    )
                )
            )
            .header(R.drawable.pkt_onboarding_logo)
        tracker.bindUiEntityType(binding.intro, UiEntityType.SCREEN)
        tracker.bindUiEntityIdentifier(binding.intro, UiEntityIdentifier.LOGGED_OUT_HOME.value)
        trackScreenImpression(binding.intro)
    }

    private fun authenticate() {
        CustomTabsIntent.Builder()
            .build()
            .launchUrl(
                requireContext(),
                Uri.parse(
                    HttpUrl.Builder()
                        .scheme("https")
                        .host(
                            pocketServer.api()
                                .replace("https://", "")
                                .replace("api.", "")
                        )
                        .addPathSegment("login")
                        .addQueryParameter("redirect_uri", POCKET_AUTH_SCHEME)
                        .addQueryParameter("consumer_key", appVersion.consumerKey)
                        .addQueryParameter("force_logout", "1")
                        .addQueryParameter("utm_source", "android")
                        .build()
                        .toString()
                )
            )
    }

    private fun setCredentialsReceiverIntentFilterEnabled(enabled: Boolean) {
        val context = context ?: return
        context.packageManager
            ?.setComponentEnabledSetting(
                ComponentName(context, "com.pocket.app.auth.AuthCallbackReceiverActivity"),
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
    }

    fun onNewIntent(intent: Intent?) {
        if (intent?.data.toString().startsWith(POCKET_AUTH_SCHEME)) {
            viewModel.onCredentialsReceived(intent?.data.toString())
        }
    }

    private fun trackScreenImpression(view: View) {
        tracker.trackImpression(view,
            ImpressionComponent.SCREEN,
            ImpressionRequirement.INSTANT)
    }

    companion object {
        const val ARG_SKIP_ONBOARDING = "skipOnboarding"
        private const val POCKET_AUTH_SCHEME = "pocket://auth"
        @JvmStatic fun newInstance(skipOnboarding: Boolean): AuthenticationFragment {
            return AuthenticationFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_SKIP_ONBOARDING, skipOnboarding)
                }
            }
        }
    }
}
