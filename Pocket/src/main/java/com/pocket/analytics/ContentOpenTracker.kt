package com.pocket.analytics

import com.pocket.analytics.events.ContentOpen
import com.pocket.app.AppScope
import com.pocket.app.reader.DestinationHelper
import com.pocket.app.reader.toContentOpenDestination
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentOpenTracker @Inject constructor(
    private val destinationHelper: DestinationHelper,
    private val appScope: AppScope,
    private val tracker: Tracker,
) {

    fun track(contentOpen: ContentOpen) {
        appScope.launch {
            destinationHelper
                .getDestination(contentOpen.contentEntity.url)
                ?.toContentOpenDestination()
                ?.let { destination ->
                    tracker.track(
                        contentOpen.copy(
                            destination = destination
                        )
                    )
                }
        }
    }
}