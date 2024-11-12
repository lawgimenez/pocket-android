package com.pocket.app.session;

import com.pocket.app.AppMode;
import com.pocket.sdk.offline.cache.AssetUser;
import com.pocket.sdk.offline.cache.Assets;
import com.pocket.sdk.preferences.AppPrefs;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.util.java.Clock;
import com.pocket.util.java.Milliseconds;
import com.pocket.util.prefs.LongPreference;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages/tracks sessions of Pocket use. A session begins when the user starts a user facing screen or some interaction that would be considered "using the app".
 * Such a use is defined by a {@link Session.Segment}, for example: starting an Activity or using TTS for audio playback.
 * <p>
 * A session ends after all {@link Session.Segment}'s have ended and the {@link #SESSION_EXPIRATION} has passed. The next call to {@link #getSid()} will generate a new sid.
 * <p>
 * For each session an id is generated called an SID. This id is simply the unix (seconds) timestamp of when the session began.
 * <p>
 * This class is thread safe.
 * <p>
 * <b>Dev Note:</b> If you add some special component of the app that would be considered in-use by the user, look at invoking {@link #startSegment(Session.Segment)} and {@link #closeSegment(Session.Segment)}.
 * 
 * 
 * Note, that if you are using {@link AbsPocketActivity}, this is already handled for you.
 */
@Singleton
public class AppSession {
    
    /**
     * After the user stops interacting with the app, if they restart the app within this time, it will count
     * as the same session. If they restart after this period, it will be considered a new session.
     */
    private static final long SESSION_EXPIRATION = Milliseconds.MINUTE * 20;
    private static final String ASSET_USER_TYPE = "session";
    private final AppMode mode;
    private final LongPreference id;
    
    private Session session; // Essentially final after onAppInit

    @Inject
    public AppSession(Assets assets, AppMode mode, AppPrefs prefs) {
        this.mode = mode;
        this.id = prefs.SESSION_ID;
        synchronized (this) {
            session = new Session(SESSION_EXPIRATION, id, prefs.WHEN_LAST_SESSION_ENDED, Clock.SYSTEM);
            session.getSid();
        }
        assets.addAssetUserCleaner(a -> {
            List<AssetUser> users = assets.getAssetUsers(ASSET_USER_TYPE);
            String sid = String.valueOf(getSid());
            for (AssetUser user : users) {
                if (!user.user.equals(sid)) {
                    assets.unregisterAssets(user);
                }
            }
        });
    }
    
    /**
     * @return The sid of the current session.
     */
    public synchronized long getSid() {
        return session.getSid();
    }
    
    /**
     * Invoke when from user's perspective, they have started "using" some component of the app. This should only be real interactions from the user's point of view,
     * this should not be background app work that the user wouldn't know about.
     * <p>
     * <b>IMPORTANT</b> You must ensure that {@link #closeSegment(Session.Segment)} is invoked properly or sessions may not expire correctly and sid generation
     * will be wrong.
     *
     * @see #closeSegment(Session.Segment)
     */
    public synchronized void startSegment(Session.Segment segment) {
        session.startSegment(segment);
    }

    /**
     * Invoke when, from the user's perspective, they have ceased interacting with this component of the app.
     *
     * @see #startSegment(Session.Segment)
     */
    public synchronized void closeSegment(Session.Segment segment) {
        session.softCloseSegment(segment);
    }
    
    /**
     * Force this session to expire on the next check.
     * Only intended for dev/debug/testing/simulation purposes.
     * You must kill the app process after calling this, otherwise the session state will be in between things and may not expire properly.
     */
    public synchronized void expire() {
        if (!mode.isForInternalCompanyOnly()) throw new UnsupportedOperationException();
        id.set(0);
    }
    
    /**
     * @return An asset user that will be automatically released at the start of the next session.
     *          Has low priority for keeping when the cache needs to be trimmed.
     */
    public AssetUser assetUser() {
        return new AssetUser(ASSET_USER_TYPE, String.valueOf(getSid()), AssetUser.PRIORITY_LOW);
    }
    
}
