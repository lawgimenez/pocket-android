package com.pocket.app.add;

import android.view.View;

import com.pocket.analytics.EngagementType;
import com.pocket.analytics.Tracker;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.action.Pv;
import com.pocket.sdk.api.generated.enums.CxtEvent;
import com.pocket.sdk.api.generated.enums.CxtSection;
import com.pocket.sdk.api.generated.thing.ActionContext;
import com.pocket.sdk.api.value.Timestamp;

/**
 * Add overlay analytics, originally defined here:
 * https://docs.google.com/spreadsheets/d/1bnorbJobsR0QzQK7uv7wGVqA5810_78am7tjYZfwRzU/edit#gid=791640838
 *
 * Note: the "premium" Add Overlay project was never released and related code has been removed, so those events do not appear here.
 */
class SaveExtensionAnalytics {

    private final Pocket pocket;
    private final ActionContext actionContext;
    private final Tracker tracker;
    private final boolean premium;

    SaveExtensionAnalytics(Pocket pocket,
            ActionContext actionContext,
            Tracker tracker,
            boolean premium) {
        this.pocket = pocket;
        this.actionContext = actionContext;
        this.tracker = tracker;
        this.premium = premium;
    }
    
    /**
     * tap "Add to Pocket" from another app, either via dedicated share button or a system/custom
     * share sheet
     * @param view the extension if it was shown, or just activity root if overlay was disabled
     */
    void onCommitSave(View view) {
        tracker.trackEngagement(view, EngagementType.SAVE, null, null, null);
    } 

    /**
     * tap 'Saved' confirmation toast to open app and go to List
     */
    void pageSavedClick() {
        track(CxtSection.ITEM_SAVE, CxtEvent.CLICK_PAGE_SAVED);
    }

    /**
     * tap outside of extension to dismiss
     */
    void clickOutsideDismiss() {
        track(CxtSection.ADD_TAGS, CxtEvent.CLICK_DISMISS);
    }

    /**
     * tap Add Tag icon to open in app tag editor/view (free)
     */
    void addTags() {
        track(CxtSection.ITEM_SAVE, CxtEvent.CLICK_ADD_TAG);
    }

    private void track(CxtSection section, CxtEvent event) {
        track(section, event, null);
    }

    private void track(CxtSection section, CxtEvent event, String version) {
        Pv.Builder pv = pocket.spec().actions().pv()
                .time(Timestamp.now())
                .context(actionContext)
                .view(actionContext.cxt_view)
                .section(section)
                .event_type(premium ? 1 : 0)
                .event(event);
        if (version != null) {
            pv.version(version);
        }
        pocket.sync(null, pv.build());
    }
}
