package com.pocket.analytics.appevents

import com.pocket.analytics.entities.ContentEntity
import com.pocket.analytics.entities.CorpusRecommendationEntity
import com.pocket.analytics.entities.UiEntity
import com.pocket.analytics.events.ContentOpen
import com.pocket.analytics.events.Engagement
import com.pocket.analytics.events.Impression

object ArticleViewEvents {

    /**
     * Fired when a user clicks the overflow button on an end of article recommendation
     */
    fun recommendationOverflowClicked() = Engagement(
        type = Engagement.Type.General,
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "article.corpus.overflow",
        )
    )

    /**
     * Fired when a user clicks the save button on an end of article recommendation
     */
    fun recommendationSaveClicked(
        url: String,
        corpusRecommendationId: String,
    ) = Engagement(
        type = Engagement.Type.Save(
            contentEntity = ContentEntity(
                url = url
            )
        ),
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "article.corpus.save",
        ),
        extraEntities = listOf(
            CorpusRecommendationEntity(
                corpusRecommendationId = corpusRecommendationId,
            ),
        )
    )

    /**
     * Fired when a user enters article view in the reader
     */
    fun screenView() = Impression(
        component = Impression.Component.Screen,
        requirement = Impression.Requirement.INSTANT,
        uiEntity = UiEntity(
            type = UiEntity.Type.SCREEN,
            identifier = "article.screen"
        )
    )

    /**
     * Fired when a user clicks on an end of article recommendation to open it
     */
    fun endOfArticleContentOpen(
        url: String,
        corpusRecommendationId: String,
    ) = ContentOpen(
        trigger = ContentOpen.Trigger.CLICK,
        contentEntity = ContentEntity(
            url = url,
        ),
        uiEntity = UiEntity(
            type = UiEntity.Type.CARD,
            identifier = "article.corpus.open",
        ),
        extraEntities = listOf(
            CorpusRecommendationEntity(
                corpusRecommendationId = corpusRecommendationId,
            ),
        )
    )

    /**
     * Fired when viewing end of article recommendations
     */
    fun endOfArticleImpression(
        positionInList: Int,
        itemUrl: String,
        corpusRecommendationId: String,
    ) = Impression(
        component = Impression.Component.Content(
            contentEntity = ContentEntity(
                url = itemUrl
            )
        ),
        requirement = Impression.Requirement.VIEWABLE,
        uiEntity = UiEntity(
            type = UiEntity.Type.CARD,
            identifier = "article.corpus.impression",
            index = positionInList,
        ),
        extraEntities = listOf(
            CorpusRecommendationEntity(
                corpusRecommendationId = corpusRecommendationId,
            ),
        )
    )

    /**
     * Fired when a user clicks a link in and article within article view
     */
    fun articleLinkContentOpen(
        url: String,
    ) = ContentOpen(
        trigger = ContentOpen.Trigger.CLICK,
        contentEntity = ContentEntity(
            url = url
        ),
        uiEntity = UiEntity(
            type = UiEntity.Type.LINK,
            identifier = "article.link.open"
        )
    )

    /* User taps the share icon in the highlights list. */
    fun highlightShareClicked(
        url: String,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.share-highlight",
        ),
        extraEntities = listOf(
            ContentEntity(url),
        ),
    )
}
