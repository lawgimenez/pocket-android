package com.pocket.analytics.appevents

import com.pocket.analytics.entities.ContentEntity
import com.pocket.analytics.entities.UiEntity
import com.pocket.analytics.events.ContentOpen
import com.pocket.analytics.events.Engagement
import com.pocket.analytics.events.Impression

object HomeEvents {

    /**
     * Fired when a user clicks a card in the `Recent Saves` section
     */
    fun recentSavesCardContentOpen(
        itemUrl: String,
        positionInList: Int,
    ) = ContentOpen(
        contentEntity = ContentEntity(
            url = itemUrl
        ),
        uiEntity = UiEntity(
            type = UiEntity.Type.CARD,
            identifier = "home.recent.open",
            index = positionInList,
        )
    )

    /**
     * Fired when a card in the recent saves section is viewed
     */
    fun recentSavesImpression(
        positionInList: Int,
        itemUrl: String,
    ) = Impression(
        component = Impression.Component.Content(
            contentEntity = ContentEntity(
                url = itemUrl
            )
        ),
        requirement = Impression.Requirement.VIEWABLE,
        uiEntity = UiEntity(
            type = UiEntity.Type.CARD,
            identifier = "home.recent.impression",
            index = positionInList,
        )
    )

    /**
     * Fired when opening an article on android home with from the discover api
     */
    fun slateArticleContentOpen(
        slateTitle: String,
        positionInSlate: Int,
        itemUrl: String,
        corpusRecommendationId: String?,
    ) = ContentOpen(
        contentEntity = ContentEntity(
            url = itemUrl
        ),
        uiEntity = UiEntity(
            identifier = "home.slate.article.open",
            type = UiEntity.Type.CARD,
            componentDetail = slateTitle,
            index = positionInSlate
        ),
        extraEntities = buildList { withCorpusRecommendationEntity(corpusRecommendationId) }
    )

    /**
     * Fired when viewing recs on home
     */
    fun slateArticleImpression(
        slateTitle: String,
        positionInSlate: Int,
        itemUrl: String,
        corpusRecommendationId: String?,
    ) = Impression(
        component = Impression.Component.Content(
            contentEntity = ContentEntity(
                url = itemUrl
            )
        ),
        requirement = Impression.Requirement.VIEWABLE,
        uiEntity = UiEntity(
            type = UiEntity.Type.CARD,
            identifier = "home.slate.article.impression",
            componentDetail = slateTitle,
            index = positionInSlate,
        ),
        extraEntities = buildList { withCorpusRecommendationEntity(corpusRecommendationId = corpusRecommendationId) }
    )

    /**
     * Fired when opening an article on android home in an expanded slate
     */
    fun slateDetailsArticleContentOpen(
        slateTitle: String,
        positionInSlate: Int,
        itemUrl: String,
        corpusRecommendationId: String?,
        ) = ContentOpen(
        contentEntity = ContentEntity(
            url = itemUrl
        ),
        uiEntity = UiEntity(
            identifier = "home.expandedSlate.article.open",
            type = UiEntity.Type.CARD,
            componentDetail = slateTitle,
            index = positionInSlate
        ),
        extraEntities = buildList { withCorpusRecommendationEntity(corpusRecommendationId) }
    )

    /**
     * Fired when viewing recs on home in an expanded slate
     */
    fun slateDetailsArticleImpression(
        slateTitle: String,
        positionInSlate: Int,
        itemUrl: String,
        corpusRecommendationId: String?,
    ) = Impression(
        component = Impression.Component.Content(
            contentEntity = ContentEntity(
                url = itemUrl
            )
        ),
        requirement = Impression.Requirement.VIEWABLE,
        uiEntity = UiEntity(
            type = UiEntity.Type.CARD,
            identifier = "home.expandedSlate.article.impression",
            componentDetail = slateTitle,
            index = positionInSlate,
        ),
        extraEntities = buildList { withCorpusRecommendationEntity(corpusRecommendationId = corpusRecommendationId) }
    )

    /**
     * Fired when opening content from a topic list
     */
    fun topicArticleContentOpen(
        topicTitle: String,
        positionInTopic: Int,
        itemUrl: String,
    ) = ContentOpen(
        contentEntity = ContentEntity(
            url = itemUrl
        ),
        uiEntity = UiEntity(
            identifier = "home.topic.article.open",
            type = UiEntity.Type.CARD,
            componentDetail = topicTitle,
            index = positionInTopic
        ),
    )

    /**
     * Fired when viewing recs from a topic list
     */
    fun topicArticleImpression(
        topicTitle: String,
        positionInTopic: Int,
        itemUrl: String,
    ) = Impression(
        component = Impression.Component.Content(
            contentEntity = ContentEntity(
                url = itemUrl
            )
        ),
        requirement = Impression.Requirement.VIEWABLE,
        uiEntity = UiEntity(
            type = UiEntity.Type.CARD,
            identifier = "home.topic.article.impression",
            componentDetail = topicTitle,
            index = positionInTopic,
        )
    )

    /**
     * Fired when a user clicks the overflow button on an article from within home
     */
    fun recommendationOverflowClicked(corpusRecommendationId: String?, url: String) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "home.corpus.overflow",
        ),
        extraEntities = buildList {
            add(ContentEntity(url))
            withCorpusRecommendationEntity(corpusRecommendationId)
        }
    )

    /**
     * Click “Save” on a card in baseline home
     */
    fun recommendationSaveClicked(
        url: String,
        corpusRecommendationId: String?,
    ) = Engagement(
        type = Engagement.Type.Save(
            contentEntity = ContentEntity(
                url = url
            )
        ),
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "home.corpus.save",
        ),
        extraEntities = buildList { withCorpusRecommendationEntity(corpusRecommendationId) },
    )

    /**
     * Fired when a user clicks See All saves in the recent saves section
     */
    fun recentSavesSeeAllClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "home.recent.view-saves"
        )
    )

    /**
     * Fired when a user clicks the favorite button on a recent save
     */
    fun recentSavesFavorite(
        positionInList: Int,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "home.recent.favorite",
            index = positionInList,
        )
    )

    /**
     * Fired when a user clicks the overflow button on a recent save
     */
    fun recentSavesOverflow(
        positionInList: Int,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "home.recent.overflow",
            index = positionInList,
        )
    )

    /**
     * Fired when a user clicks the mark as viewed button in the recent saves overflow menu
     */
    fun recentSavesOverflowMarkAsViewed(
        positionInList: Int,
        url: String,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "home.recent.markAsViewed",
            index = positionInList,
        ),
        extraEntities = listOf(
            ContentEntity(
                url = url
            )
        ),
    )

    /**
     * Fired when a user clicks the share button in the recent saves overflow menu
     */
    fun recentSavesOverflowShare(
        positionInList: Int,
        url: String,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "home.recent.share",
            index = positionInList,
        ),
        extraEntities = listOf(
            ContentEntity(
                url = url
            )
        ),
    )

    /**
     * Fired when a user clicks the archive button in the recent saves overflow menu
     */
    fun recentSavesOverflowArchive(
        positionInList: Int,
        url: String,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "home.recent.archive",
            index = positionInList,
        ),
        extraEntities = listOf(
            ContentEntity(
                url = url
            )
        ),
    )

    /**
     * Fired when a user clicks the delete button in the recent saves overflow menu
     */
    fun recentSavesOverflowDelete(
        positionInList: Int,
        url: String,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "home.recent.delete",
            index = positionInList,
        ),
        extraEntities = listOf(
            ContentEntity(
                url = url
            )
        ),
    )

    fun slateSeeAllClicked(
        slateTitle: String,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "home.slate.seeAll",
            componentDetail = slateTitle,
        ),
    )

    fun topicClicked(
        topicTitle: String,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "home.topic.click",
            componentDetail = topicTitle,
        ),
    )

    /** "Sign in to get the best of Pocket" banner viewed. */
    fun signInBannerImpression() = Impression(
        component = Impression.Component.Card,
        requirement = Impression.Requirement.VIEWABLE,
        uiEntity = UiEntity(
            type = UiEntity.Type.CARD,
            identifier = "home.signin.banner",
        ),
    )

    /** "Continue" on the "Sign in to get the best of Pocket" banner clicked. */
    fun signInBannerButtonClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "home.signin.banner.button",
        ),
    )
}
