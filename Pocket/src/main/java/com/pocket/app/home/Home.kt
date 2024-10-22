package com.pocket.app.home

import com.pocket.sdk.api.generated.thing.Item

class Home {

    interface Interactions {
        fun onInitialized()
        fun onUserReturned()
        fun onTopicClicked(topicId: String, topicTitle: String)
        fun onSwipedToRefresh()
        fun onPremiumUpgradeClicked()
    }

    interface RecommendationInteractions {
        fun onRecommendationOverflowClicked(
            url: String,
            title: String,
            corpusRecommendationId: String?,
        )
        fun onSeeAllRecommendationsClicked(
            slateId: String,
            slateTitle: String
        )
        fun onSaveClicked(url: String, isSaved: Boolean, corpusRecommendationId: String?)
        fun onItemClicked(
            url: String,
            slateTitle: String,
            positionInSlate: Int,
            corpusRecommendationId: String?,
        )
        fun onRecommendationViewed(
            slateTitle: String,
            positionInSlate: Int,
            itemUrl: String,
            corpusRecommendationId: String?
        )
    }

    interface ErrorSnackBarInteractions {
        fun onErrorRetryClicked()
        fun onErrorSnackBarDismissed()
    }

    interface SavesInteractions {
        fun onInitialized()
        fun onItemClicked(item: Item, positionInList: Int)
        fun onFavoriteClicked(item: Item, positionInList: Int)
        fun onSaveOverflowClicked(item: Item, itemPosition: Int)
        fun onSeeAllSavesClicked()
        fun onSaveViewed(positionInList: Int, url: String)
    }

    sealed class Event {
        data class GoToReader(
            val url: String,
        ) : Event()

        data class ShowRecommendationOverflow(
            val url: String,
            val title: String,
            val corpusRecommendationId: String?,
        ) : Event()

        data class ShowSaveOverflow(
            val item: Item,
            val itemPosition: Int
        ) : Event()

        data object GoToMyList : Event()

        data class GoToSlateDetails(
            val slateId: String,
        ) : Event()

        data class GoToTopicDetails(
            val topicId: String,
        ) : Event()

        data object GoToPremium : Event()
        data object GoToSignIn : Event()
    }
}