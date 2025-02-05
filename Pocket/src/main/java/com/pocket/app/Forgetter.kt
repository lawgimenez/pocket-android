@file:Suppress("DEPRECATION")

package com.pocket.app

import com.pocket.sdk.Pocket
import com.pocket.sdk.api.generated.enums.AdzerkPlacementName
import com.pocket.sdk.api.generated.enums.ItemStatus
import com.pocket.sync.space.Holder
import com.pocket.util.prefs.Preferences

/**
 * From time to time we're required to release old holds that previous clients may have created.  Rather than clutter classes with these and possibly lose them or their
 * context over time, these statements are added here.
 */
object Forgetter {
    fun forget(pocket: Pocket, prefs: Preferences) {

        val appSyncHolder = Holder.persistent("AppSync")
        // for some beta users we might need to release this old rememberWhere hold, see MutableSpace.holdersToMatches for details on when/how to clean up
        pocket.forget(appSyncHolder, pocket.spec().things().item().status(ItemStatus.UNREAD).build())
        // for some prod and beta users that were part of the rollout before 7.21.1.1, we need to release the higher item limit that we had
        pocket.forget(appSyncHolder, pocket.spec().things().localItems().max(10000).build())

        // with the release of a revamped "Discover" feed in early 2020, the old feed endpoint was moved to the activity tab and rebranded as "Social Recs", with a
        // feed_class parameter added to separate Discover content from a user's social recommendations.  This identifies the old feed / Discover hold and forgets it if held.
        pocket.forget(Holder.persistent("discover"), pocket.spec().things().feed().version("4").count(30).offset(0).build())

        // We removed this tooltip pointing users to the Activity tab for social recs.
        prefs.remove("show_recs_moved_tooltip")

        // Preferences used by the old v3-based Discover tab.
        prefs.remove("last_discover_feed_refresh")
        prefs.remove("show_survey_spot")
        prefs.remove("show_edu_spot")

        // Legacy feature flags and old tests that were using them.
        pocket.forget(Holder.persistent("flags"), pocket.spec().things().checkFeatures().build())
        prefs.remove("s_feature_flags")

        // Preferences used by a survey prompt we no longer show to new users in My List.
        prefs.remove("noobsrv_dis")
        prefs.remove("dcfig_noobsrv_frc")

        // List counts feature we never released.
        prefs.remove("listCounts")
        prefs.remove("listCounts_tegi")
        prefs.remove("listCounts_in_t")
        prefs.remove("listCounts_in_h")
        prefs.remove("dcfig_list_counts")

        prefs.remove("hasPromptedForName")
        prefs.remove("excludeTagged")

        prefs.remove("fxa")
        prefs.remove("cleargacct")
        prefs.remove("gsovuser")
        prefs.remove("googleaccount")
        prefs.remove("googleconnected")

        // old slates lineup for discover
        prefs.remove("lineup_id")
        pocket.forget(
            Holder.persistent("slates"),
            pocket.spec().things().slateLineup
                .slateLineupId("af3a5196-be16-4bf7-b131-70981e43b132")
                .slateCount(8)
                .recommendationCount(20)
                .build()
        )
        pocket.forget(
            Holder.persistent("slates"),
            pocket.spec().things().slateLineup
                .slateLineupId("507d215c-4776-4f8e-af49-2ed6f3309aff")
                .slateCount(1)
                .recommendationCount(30)
                .build()
        )

        // old adzerk spocs for discover
        prefs.remove("sp_dscvr_cnt")
        pocket.forget(
            Holder.persistent("adzerk.2"),
            pocket.spec().things().adzerkSpocs().name(AdzerkPlacementName.DISCOVER).build()
        )
        pocket.forget(
            Holder.persistent("adzerk.2"),
            pocket.spec().things().adzerkSpocs().name(AdzerkPlacementName.GERMAN_DISCOVER).build()
        )

        // old discover topics list
        pocket.forget(
            Holder.persistent("discover"),
            pocket.spec().things().discoverTopicList().build()
        )

        // old premium upsell pref on my list
        prefs.remove("showMyListUpsell")

        prefs.remove("listDensity")
        prefs.remove("listMode")

        // old activity / notifications screen
        prefs.remove("pktnot_since")
        prefs.remove("pktnot_delv")
        prefs.remove("has_unread_notifications")
        prefs.remove("has_unsynced_notifications")
        pocket.forget(
            Holder.persistent("activity"),
            pocket.spec().things().getNotifications().version("1").build()
        )
        prefs.remove("dcfig_showtakennotis")
        prefs.remove("contacts_imported")
        prefs.remove("readContactsRequested")

        prefs.remove("feed_impr2")
        prefs.remove("feed_impr_sess")
        prefs.remove("dismissed_profile_intro")
        prefs.remove("hasPromptAutoCompl")
        prefs.remove("viewed_feed")
        prefs.remove("photoEditorStatus")
        prefs.remove("pktnot_impr_id")
        prefs.remove("pktnot_impr_sess")
        prefs.remove("show_find_followers_help")
        prefs.remove("pending_gift")

        // Snowplow SDK migration
        prefs.remove("snwplw_1stp")

        // old offline downloading
        prefs.remove("downloadAuto")
        prefs.remove("downloadWeb")

        // old feature flag
        prefs.remove("temp.android.app.reader")

        // twitter
        prefs.remove("hassavedtweetattr")

        // site logins
        prefs.remove("pendingCustomSubscription")

        // removed settings
        prefs.remove("scrollByVolume")
        prefs.remove("pageFlipping")

        // old list state
        prefs.remove("list_impr")
        prefs.remove("list_impr_sess")

        // old reader
        prefs.remove("avatar_file_inc_")
        prefs.remove("gsf_has_visited_reader")
        prefs.remove("webview_disabled_zoom_tip_toast")
        prefs.remove("recit_eoa_enabled")
        prefs.remove("appThemeAutoDarkThreshold")
        prefs.remove("appThemeAutoDark")
        prefs.remove("appThemeAutoDarkThreshManual")

        // Adzerk/Kevel SPOCs/ads
        prefs.remove("adzrk_usr_k")
        prefs.remove("sp_session_id")
        prefs.remove("sp_session_actions")
        prefs.remove("dcfig_addebug")
        prefs.remove("dcfig_adsid")
        prefs.remove("dcfig_adnoage")
        prefs.remove("dcfig_impressshow")
        pocket.forget(Holder.persistent("adzerk.2"))
        pocket.forget(Holder.persistent("adzerk"))

        // Home slates provided by getSlateLineup
        pocket.forget(Holder.persistent("homeSlates"))
        prefs.remove("slate_lineup_refresh_time")
        prefs.remove("topic_refresh_time")

        // Post-signup onboarding.
        prefs.remove("all-aboard")
        prefs.remove("temp.android.app.home.onboarding")

        // "Listen Discoverability"
        prefs.remove("lstn_dscvr_a")
        prefs.remove("lstn_dscvr_b")
        prefs.remove("lstn_dscvr_tmstmp")
        prefs.remove("dcfig_lstn_dscvr_a")
        prefs.remove("dcfig_lstn_dscvr_b")
    }
}
