package com.pocket.repository

import com.pocket.sdk.api.generated.thing.LoginInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeUserRepository : UserRepository {
    var loggedIn = true
    var premium = false
    var hasPremiumDisplaySettings = false

    override fun isLoggedIn() = flow { emit(loggedIn) }

    override fun isPremiumUpgradeAvailable() = flow { emit(!premium) }

    override fun hasPremiumDisplaySettings() = flow { emit(hasPremiumDisplaySettings) }

    override fun getLoginInfoAsFlow(): Flow<LoginInfo> {
        return flow {
            emit(LoginInfo.Builder().build())
        }
    }
}
