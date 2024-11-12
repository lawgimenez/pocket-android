package com.pocket.repository

import com.pocket.sdk.Pocket
import com.pocket.sdk.api.generated.thing.DiscoverTopicFeed
import com.pocket.sdk.api.generated.thing.DiscoverTopicList
import com.pocket.sdk.get
import com.pocket.sync.await
import com.pocket.sync.source.bindLocalAsFlow
import com.pocket.sync.space.Holder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TopicsRepository @Inject constructor(
    private val pocket: Pocket,
) {

    private val holder = Holder.persistent("topics")
    private val topics = pocket.spec().things().discoverTopicList().build()
    private val topicSessionHolder = Holder.session("topicSession")

    init {
        pocket.setup {
            pocket.remember(holder, topics)
        }
    }

    suspend fun refreshTopics(): DiscoverTopicList =
        pocket.syncRemote(topics).await()

    fun getTopicsAsFlow(): Flow<DiscoverTopicList> =
        pocket.bindLocalAsFlow(topics)

    suspend fun getTopicsLocal(): DiscoverTopicList =
        pocket.syncLocal(topics).await()

    suspend fun hasCachedTopics(): Boolean = !getTopicsLocal().topics.isNullOrEmpty()

    suspend fun refreshTopic(topicId: String): DiscoverTopicFeed {
        val topic = pocket.spec().things().discoverTopicFeed()
            .topics(topicId)
            .curated_count(CURATED_COUNT)
            .algorithmic_count(ALGORITHMIC_COUNT)
            .build()
        pocket.setup {
            pocket.remember(topicSessionHolder, topic)
        }
        return pocket.get(topic)
    }

    fun getTopicAsFlow(topicId: String): Flow<DiscoverTopicFeed> =
        pocket.bindLocalAsFlow(
            pocket.spec().things().discoverTopicFeed()
                .topics(topicId)
                .curated_count(CURATED_COUNT)
                .algorithmic_count(ALGORITHMIC_COUNT)
                .build()
        )

    companion object {
        private const val CURATED_COUNT = 5
        private const val ALGORITHMIC_COUNT = 20
    }
}