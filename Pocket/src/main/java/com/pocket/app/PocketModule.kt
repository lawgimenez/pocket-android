package com.pocket.app

import android.content.Context
import android.preference.PreferenceManager
import com.pocket.analytics.BrowserAnalytics
import com.pocket.analytics.PocketTracker
import com.pocket.analytics.SnowplowTracker
import com.pocket.analytics.Tracker
import com.pocket.app.build.Versioning
import com.pocket.repository.ItemRepository
import com.pocket.repository.SyncEngineItemRepository
import com.pocket.repository.SyncEngineUserRepository
import com.pocket.repository.UserRepository
import com.pocket.sdk.Pocket
import com.pocket.sdk.api.*
import com.pocket.sdk.build.AppVersion
import com.pocket.sdk.dev.ErrorHandler
import com.pocket.sdk.http.AndroidNetworkStatus
import com.pocket.sdk.http.NetworkStatus
import com.pocket.sdk.notification.push.PktPush
import com.pocket.sdk.notification.push.Push
import com.pocket.sdk.offline.cache.Assets
import com.pocket.sdk.preferences.AppPrefs
import com.pocket.sdk2.api.legacy.LegacyMigration
import com.pocket.sdk2.api.legacy.PocketCache
import com.pocket.util.DrawableLoader
import com.pocket.util.StringLoader
import com.pocket.util.prefs.AndroidPrefStore
import com.pocket.util.prefs.Preferences
import com.pocket.util.prefs.Prefs
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.threeten.bp.Clock
import javax.inject.Singleton

/**
 * All of the Pocket app's components.
 */
@Module
@InstallIn(SingletonComponent::class)
class PocketModule {

    @Provides @Singleton
    fun providePrefs(@ApplicationContext context: Context): Preferences =
        Prefs(
            AndroidPrefStore(PreferenceManager.getDefaultSharedPreferences(context)),
            AndroidPrefStore(context.getSharedPreferences("pocketAppPrefs", 0))
        )

    @Provides @Singleton
    fun provideAppMode(appVersion: AppVersion): AppMode = appVersion.mode()

    @Provides @Singleton
    fun provideLegacyMigration(
        prefs: Preferences,
        @ApplicationContext context: Context,
        versioning: Versioning,
        assets: Assets,
        errorHandler: ErrorHandler,
        appPrefs: AppPrefs,
        itemCap: ItemCap,
        appThreads: AppThreads,
        dispatcher: AppLifecycleEventDispatcher
    ): LegacyMigration? = LegacyMigration.createIfNeeded(
        prefs,
        context,
        versioning,
        assets,
        errorHandler,
        appPrefs,
        itemCap.cap,
        appThreads,
        dispatcher
    )

    @Provides @Singleton
    fun providePocket(pocketSingleton: PocketSingleton): Pocket = pocketSingleton.instance

    @Provides @Singleton
    fun provideNetworkStatus(@ApplicationContext context: Context): NetworkStatus =
        AndroidNetworkStatus(context)

    @Provides @Singleton
    fun providePocketTracker(
        pocketSingleton: PocketSingleton,
        appOpen: AppOpen,
        browserAnalytics: BrowserAnalytics,
        pocketCache: PocketCache,
        prefs: Preferences,
        @ApplicationContext context: Context,
        appVersion: AppVersion,
        pktServer: PocketServer,
        mode: AppMode,
        clock: Clock,
        adjust: AdjustSdkComponent,
        dispatcher: AppLifecycleEventDispatcher
    ): Tracker = PocketTracker(
        pocketSingleton.instance,
        appOpen,
        browserAnalytics,
        pocketCache,
        SnowplowTracker(
            prefs,
            context,
            clock,
            pktServer.snowplowCollector(),
            pktServer.snowplowPostPath(),
            mode.isForInternalCompanyOnly,
            { adjust.getAdId() },
            appVersion.apiId,
            appVersion.getVersionName(context)
        ),
        dispatcher
    )

    @Provides @Singleton
    fun providePocketPush(
        @ApplicationContext context: Context,
        pocketSingleton: PocketSingleton,
        appSync: AppSync,
        appThreads: AppThreads,
        pocketCache: PocketCache,
        prefs: Preferences,
        appVersion: AppVersion
    ): Push = PktPush(
            context,
            pocketSingleton.instance,
            appSync,
            appThreads,
            pocketCache,
            prefs,
            appVersion.mode()
        )

    @Provides @Singleton
    fun provideClock(errorHandler: ErrorHandler): Clock =
        try {
            Clock.systemDefaultZone()
        } catch (t: Throwable) {
            errorHandler.reportError(t)
            Clock.systemUTC()
        }

    @Provides @Singleton
    fun provideStringLoader(@ApplicationContext context: Context): StringLoader = StringLoader(context)

    @Provides @Singleton
    fun provideDrawableLoader(@ApplicationContext context: Context): DrawableLoader = DrawableLoader(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PocketInterfaces {
    @Binds @Singleton
    abstract fun itemRepository(syncEngineItemRepository: SyncEngineItemRepository): ItemRepository

    @Binds @Singleton
    abstract fun userRepository(syncEngineUserRepository: SyncEngineUserRepository): UserRepository
}
