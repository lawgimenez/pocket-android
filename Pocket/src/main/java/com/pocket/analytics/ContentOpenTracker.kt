package com.pocket.analytics

import com.pocket.analytics.events.ContentOpen
import com.pocket.app.AppScope
import com.pocket.app.reader.Destination
import com.pocket.app.reader.DestinationHelper
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
            val destination =
                when (destinationHelper.getDestination(contentOpen.contentEntity.url)) {
                    Destination.COLLECTION, Destination.ARTICLE -> ContentOpen.Destination.INTERNAL
                    Destination.ORIGINAL_WEB -> ContentOpen.Destination.EXTERNAL
                }
            tracker.track(contentOpen.copy(destination = destination))
        }
    }
}
