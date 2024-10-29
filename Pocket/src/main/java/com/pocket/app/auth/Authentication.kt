package com.pocket.app.auth

class Authentication {
    sealed class Event {
        data object Authenticate : Event()
        data object GoToDefaultScreen : Event()
        data object GoBack : Event()
        data object ShowErrorToast : Event()
        data object ShowDeletedAccountToast : Event()
        data object DisableCredentialsCallbackIntentFilter : Event()
        data object OpenTeamTools : Event()
    }
}