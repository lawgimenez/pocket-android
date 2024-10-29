package com.pocket.app.auth

import androidx.lifecycle.ViewModel
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.AuthenticationEvents
import com.pocket.app.AdjustSdkComponent
import com.pocket.app.AppMode
import com.pocket.app.UserManager
import com.pocket.sdk.Pocket
import com.pocket.sdk.http.HttpClientDelegate
import com.pocket.sdk.http.NetworkStatus
import com.pocket.util.edit
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URL

@HiltViewModel(assistedFactory = AuthenticationViewModel.Factory::class)
class AuthenticationViewModel @AssistedInject constructor(
    private val httpClientDelegate: HttpClientDelegate,
    private val fxaFeature: FxaFeature,
    private val userManager: UserManager,
    private val adjustSdkComponent: AdjustSdkComponent,
    private val tracker: Tracker,
    private val mode: AppMode,
    @Assisted val skipOnboarding: Boolean,
) : ViewModel() {

    @AssistedFactory interface Factory {
        fun create(skipOnboarding: Boolean): AuthenticationViewModel
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val _events = MutableSharedFlow<Authentication.Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Authentication.Event> = _events

    private var initialEventCollectionStarted = false
    private var networkStatusListener: NetworkStatus.Listener? = null

    suspend fun onEventCollectionStarted(flowCollector: FlowCollector<Authentication.Event>) {
        if (skipOnboarding) {
            if (!initialEventCollectionStarted) {
                // This is first time this screen starts.
                // Skip past onboarding straight to authentication.
                flowCollector.emit(Authentication.Event.Authenticate)
            } else if (_uiState.value.screenState == ScreenState.Loading) {
                // Authentication was successful and we received credentials.
                // Let authentication proceed.
            } else {
                // They must've backed out of authentication.
                // Skip past onboarding to the previous screen
                flowCollector.emit(Authentication.Event.GoBack)
            }
        }
        if (userManager.hasDeletedAccount()) {
            flowCollector.emit(Authentication.Event.ShowDeletedAccountToast)
        }
        initialEventCollectionStarted = true
    }

    fun onShowedDeletedAccountToast() {
        userManager.onShowedDeletedAccountToast()
    }

    fun onAuthenticateClicked() {
        tracker.track(AuthenticationEvents.continueButtonClicked())
        if (!checkForInternet()) return
        _events.tryEmit(Authentication.Event.Authenticate)
    }

    fun onAuthenticateLongClicked(): Boolean {
        if (mode.isForInternalCompanyOnly) {
            _events.tryEmit(Authentication.Event.OpenTeamTools)
            return true
        }

        return false
    }

    fun onContinueSignedOutClicked() {
        userManager.enableSignedOutExperience()
        _events.tryEmit(Authentication.Event.DisableCredentialsCallbackIntentFilter)
        _events.tryEmit(Authentication.Event.GoToDefaultScreen)
    }

    fun onOfflineCloseButtonClicked() {
        hideOfflineView()
    }

    fun onFragmentDestroyed() {
        httpClientDelegate.status().removeListener(networkStatusListener)
    }

    fun onCredentialsReceived(authUri: String) {
        _uiState.edit { copy(
            screenState = ScreenState.Loading
        ) }
        // can't parse the url if it uses the pocket:// scheme.  Just replace with http
        val httpUrl: HttpUrl? = URL(
            authUri.replace("pocket://", "http://")
        ).toHttpUrlOrNull()
        val authKey = httpUrl?.queryParameter("access_token")
        val fxaMigration = httpUrl?.queryParameter("fxa_migration")
        val type = httpUrl?.queryParameter("type")
        fxaFeature.shouldShowMigrationMessage = fxaMigration != null && fxaMigration == "1"
        userManager.authenticate(
            { userApi: Pocket.UserApi, extras: Pocket.AuthenticationExtras? ->
                // Avoiding a race condition where the api doesn't recognize the access token yet
                // and returns a 401.  This method is called from within an async thread, so
                // this shouldn't block the main thread.
                Thread.sleep(1000)
                userApi.loginWithAccessToken(authKey, extras)
            },
            {
                val isSignUp = type != null && type == "signup"
                if (isSignUp) {
                    tracker.track(AuthenticationEvents.signupComplete())
                    adjustSdkComponent.trackSignUp()
                } else {
                    tracker.track(AuthenticationEvents.loginComplete())
                }

                // Make sure that when they log out they skip this screen
                // and see the signed out experience.
                userManager.enableSignedOutExperience()

                _events.tryEmit(Authentication.Event.DisableCredentialsCallbackIntentFilter)
                _events.tryEmit(Authentication.Event.GoToDefaultScreen)
            }
        ) {
            _uiState.edit { copy(
                screenState = ScreenState.Default
            ) }
            _events.tryEmit(Authentication.Event.ShowErrorToast)
        }
    }

    /**
     * return true if user has internet connection
     */
    private fun checkForInternet(): Boolean {
        if (!httpClientDelegate.status().isOnline) {
            _uiState.edit { copy(
                screenState = ScreenState.Offline
            ) }
            networkStatusListener = NetworkStatus.Listener { status: NetworkStatus ->
                if (status.isOnline) {
                    hideOfflineView()
                }
            }
            httpClientDelegate.status().addListener(networkStatusListener)
            return false
        }
        return true
    }

    private fun hideOfflineView() {
        httpClientDelegate.status().removeListener(networkStatusListener)
        _uiState.edit { copy(
            screenState = ScreenState.Default
        ) }
    }

    data class UiState(
        val screenState: ScreenState = ScreenState.Default,
    )

    sealed class ScreenState(
        val loadingVisible: Boolean = false,
        val offlineVisible: Boolean = false,
        val mainLayoutVisible: Boolean = false,
    ) {
        data object Loading : ScreenState(
            loadingVisible = true
        )
        data object Offline : ScreenState(
            offlineVisible = true
        )
        data object Default : ScreenState(
            mainLayoutVisible = true
        )
    }
}
