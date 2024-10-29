package com.pocket.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocket.analytics.ContentOpenTracker
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.MainEvents
import com.pocket.analytics.appevents.ReaderEvents
import com.pocket.repository.ItemRepository
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.util.edit
import com.pocket.util.prefs.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: Preferences,
    private val itemRepository: ItemRepository,
    private val contentOpenTracker: ContentOpenTracker,
    private val userManager: UserManager,
    private val tracker: Tracker,
): ViewModel(), MainActivityInteractions {

    private val lastTabOpenedPreference = preferences.forUser(LAST_TAB_OPENED, HOME)
    // Fixes two edge cases
    // 1. Skip updating the lastTabOpenedPreference for the initial destination navigation
    // 2. Only navigate to the last opened tab one time
    private var initialEventCollectionStarted = false

    private val _uiState = MutableStateFlow(
        UiState(
            navigationButtonState = NavigationButtonState.SavesChecked
        )
    )
    val uiState: StateFlow<UiState> = _uiState

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> = _events

    override fun onHomeClicked() {
        _events.tryEmit(Event.GoToHome)
    }

    override fun onSavesClicked() {
        _events.tryEmit(Event.GoToSaves)
    }

    override fun onSettingsClicked() {
        _events.tryEmit(Event.GoToSettings)
    }

    override fun onNavigationDestinationChanged(destination: Destination) {
        when (destination) {
            Destination.HOME -> _uiState.edit { copy(
                navigationButtonState = NavigationButtonState.HomeChecked,
                bottomNavigationVisible = true,
            ) }
            Destination.SAVES -> _uiState.edit { copy(
                navigationButtonState = NavigationButtonState.SavesChecked,
                bottomNavigationVisible = true,
            ) }
            Destination.SETTINGS -> _uiState.edit { copy(
                navigationButtonState = NavigationButtonState.SettingsChecked,
                bottomNavigationVisible = true,
            ) }
            else -> _uiState.edit { copy(
                bottomNavigationVisible = false,
            ) }
        }
        if (initialEventCollectionStarted) {
            when (destination) {
                Destination.HOME -> lastTabOpenedPreference.set(HOME)
                Destination.SAVES -> lastTabOpenedPreference.set(SAVES)
                else -> {}
            }
        }
    }

    suspend fun onEventCollectionStarted(flowCollector: FlowCollector<Event>) {
        if (!initialEventCollectionStarted) {
            initialEventCollectionStarted = true
            when (lastTabOpenedPreference.get()) {
                HOME -> onHomeClicked()
                SAVES -> onSavesClicked()
            }
        }

        if (userManager.hasDeletedAccount()) {
            flowCollector.emit(Event.ShowDeletedAccountToast)
        }
        if (userManager.hadBadCredentials()) {
            flowCollector.emit(Event.ShowBadCredentialsToast)
        }
    }

    fun onShowedDeletedAccountToast() {
        tracker.track(MainEvents.accountDeleteBannerImpression())
        userManager.onShowedDeletedAccountToast()
    }

    fun onDeletedAccountExitSurveyClicked(showed: Boolean) {
        tracker.track(MainEvents.accountDeleteExitSurveyClicked())
        if (showed) {
            tracker.track(MainEvents.accountDeleteExitSurveyImpression())
        }
    }

    fun onShowedBadCredentialsToast() {
        userManager.onShowedBadCredentialsMessage()
    }

    override fun onReaderDeepLinkReceived(url: String, openListen: Boolean) {
        viewModelScope.launch {
            val httpUrl = url.toHttpUrlOrNull()

            val item: Item? = when {
                httpUrl == null -> null
                httpUrl.isShareLink() -> {
                    // If it's a Share Link then resolve it via the API.
                    try {
                        _events.emit(Event.ShowProgress)
                        itemRepository.getItemByShareSlug(httpUrl.pathSegments[1])
                    } catch (ignored: Throwable) {
                        // Fall back to let the browser open the original URL.
                        null
                    } finally {
                        _events.emit(Event.HideProgress)
                    }
                }
                httpUrl.isShortLink() -> {
                    // If it's a pocket.co link then un-shorten it via the API.
                    try {
                        _events.emit(Event.ShowProgress)
                        itemRepository.getItem(url, ItemRepository.LookupStrategy.RemoteIfNotCached)
                    } catch (ignored: Throwable) {
                        // Fall back to opening by URL and let the browser redirect the short link.
                        null
                    } finally {
                        _events.emit(Event.HideProgress)
                    }
                }
                openListen -> {
                    // We need the item object if we are planning to open listen.
                    try {
                        _events.emit(Event.ShowProgress)
                        itemRepository.getItem(url, ItemRepository.LookupStrategy.RemoteIfNotCached)
                    } catch (ignored: Throwable) {
                        null
                    } finally {
                        _events.emit(Event.HideProgress)
                    }
                }
                else -> {
                    null
                }
            }

            val realUrl = item?.id_url?.url ?: url
            if (httpUrl?.isShortLink() == true) {
                contentOpenTracker.track(ReaderEvents.pocketCoContentOpen(realUrl))
            } else {
                contentOpenTracker.track(ReaderEvents.deeplinkContentOpen(realUrl))
            }
            _events.emit(
                Event.OpenReader(
                realUrl,
                openListen,
                item
            ))
        }
    }

    /**
     * @return true if it's a Pocket Share Link, e.g. https://pocket.co/share/3a3c1727-caae-4adc-8062-e31dd0ebf7a8
     */
    private fun HttpUrl.isShareLink(): Boolean {
        return host == "pocket.co" && pathSize == 2 && pathSegments[0] == "share"
    }

    /**
     * @return true if it's a `pocket.co` short link, e.g. https://pocket.co/x1Lbih
     */
    private fun HttpUrl.isShortLink(): Boolean {
        return  host == "pocket.co" && pathSize == 1
    }

    data class UiState(
        val navigationButtonState: NavigationButtonState = NavigationButtonState.HomeChecked,
        val bottomNavigationVisible: Boolean = true,
    )

    sealed class NavigationButtonState(
        val homeNavigationButtonChecked: Boolean = false,
        val savesNavigationButtonChecked: Boolean = false,
        val settingsNavigationButtonChecked: Boolean = false,
    ) {
        data object HomeChecked: NavigationButtonState(
            homeNavigationButtonChecked = true
        )
        data object SavesChecked: NavigationButtonState(
            savesNavigationButtonChecked = true
        )
        data object SettingsChecked: NavigationButtonState(
            settingsNavigationButtonChecked = true
        )
    }

    enum class Destination {
        HOME,
        SAVES,
        SETTINGS,
        OTHER
    }

    sealed class Event {
        data object GoToHome : Event()
        data object GoToSaves : Event()
        data object GoToSettings : Event()
        data object ShowProgress : Event()
        data object HideProgress : Event()
        data class OpenReader(
            val url: String,
            val openListen: Boolean,
            val item: Item?,
        ) : Event()
        data object ShowDeletedAccountToast : Event()
        data object ShowBadCredentialsToast : Event()
    }

    companion object {
        private const val LAST_TAB_OPENED = "last_tab_opened"
        private const val HOME = "home"
        private const val SAVES = "my_list"
    }
}

interface MainActivityInteractions {
    fun onHomeClicked()
    fun onSavesClicked()
    fun onSettingsClicked()
    fun onNavigationDestinationChanged(destination: MainViewModel.Destination)
    fun onReaderDeepLinkReceived(url: String, openListen: Boolean)
}