package com.pocket.app.list;

import com.pocket.app.App;
import com.pocket.app.AppMode;
import com.pocket.app.Feature;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.action.Pv;
import com.pocket.sdk.api.generated.enums.CxtEvent;
import com.pocket.sdk.api.generated.enums.CxtSection;
import com.pocket.sdk.api.generated.enums.ItemSortKey;
import com.pocket.sdk.api.generated.enums.ItemStatusKey;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.preferences.AppPrefs;
import com.pocket.sdk2.analytics.context.Interaction;
import com.pocket.util.java.Safe;
import com.pocket.util.prefs.BooleanPreference;
import com.pocket.util.prefs.StringPreference;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Continue Reading is a feature which displays a Snackbar style view of the most recently read Item
 * when returning to Pocket.
 *
 * The view displays information about the item, such as title, time left, etc., and clicking it will open the Item in the Reader.
 * It is dismissable via swiping to the left or right, or will dismiss automatically after 20 seconds.
 *
 * Original design:
 * https://www.figma.com/file/0xjB5lL323HTcL5abNuiu0iX/Continue-Reading?node-id=0%3A1
 */
@Singleton
public class ContinueReading extends Feature {
    
    public interface ContinueReadingListener {
        void onContinueReadingItemFound(Item item);
    }
    
    private final Pocket pocket;
    private final StringPreference shownId;
    private final BooleanPreference enabled;
    private final BooleanPreference devconfigAlwaysShow;
    private boolean hasCheckedThisAppSession = false;

    @Inject
    public ContinueReading(
            AppMode mode,
            Pocket pocket,
            AppPrefs prefs
    ) {
        super(mode);
        this.pocket = pocket;
        this.shownId = prefs.CONTINUE_READING_SHOWN_ID;
        this.enabled = prefs.CONTINUE_READING_ENABLED;
        this.devconfigAlwaysShow = prefs.DEVCONFIG_SNACKBAR_ALWAYS_SHOW_URL_CR;
        setupAppPresenceChangedListener();
    }

    private void setupAppPresenceChangedListener() {
        App.addOnUserPresenceChangedListener(isInApp -> {
            if (isInApp) {
                hasCheckedThisAppSession = false;
            }
        });
    }

    /**
     * The Continue Reading view appears under the following conditions:
     *
     * 1. The Item is unread.
     * 2. The Item is an article.
     * 3. The user has spent more that 20 seconds on the article.
     * 4. The user has not scrolled more that 95% of the article.
     * 5. The Item is the most recently updated Item the last displayed Continue Reading view was not for this Item.
     * 6. We have already made the check this app session
     *
     * For condition 5, this means that it is possible to see a Continue Reading view for the same item multiple times, provided
     * the user were to see the view for a different item in between.  Reading an item after reading a different one will
     * trigger a new Continue Reading check for that item.
     *
     * @param listener A callback which provides the Item to display, if one is found.  If no Item is found the callback will not occur.
     */
    public void checkForContinueReading(ContinueReadingListener listener) {
        if (!isOn() || hasCheckedThisAppSession) return;
        hasCheckedThisAppSession = true;
        
        pocket.sync(pocket.spec().things().saves()
                .count(1)
                .state(ItemStatusKey.UNREAD)
                .sort(ItemSortKey.POSITION_UPDATED)
                .minTimeSpent(20)
                .maxScrolled(95)
                .is_article(true)
                .build())
                .onSuccess(saves -> {
                    Item item = Safe.get(() -> saves.list.get(0));
                    if (item != null) {
                        String id = item.id_url.url;
                        if (devReview() || !id.equals(shownId.get())) {
                            shownId.set(id);
                            listener.onContinueReadingItemFound(item);
                        }
                    }
                });
    }

    private boolean devReview() {
        return getAudience().isInternalCompany() && devconfigAlwaysShow.get();
    }

    public void trackView(Interaction it) {
        trackEvent(it,false);
    }

    public void trackDismiss(Interaction it) {
        trackEvent(it,true);
    }

    private void trackEvent(Interaction it, boolean dismiss) {
        Pv.Builder builder = pocket.spec().actions().pv()
                .time(it.time)
                .section(CxtSection.CONTINUE_READING)
                .context(it.context);
        if (dismiss) {
            builder.event(CxtEvent.DISMISS);
        }
        pocket.sync(null, builder.build());
    }
    
    @Override
    protected boolean isEnabled(Audience audience) {
        return true;
    }
    
    @Override
    protected boolean isOn(Audience audience) {
        return enabled.get();
    }
}
