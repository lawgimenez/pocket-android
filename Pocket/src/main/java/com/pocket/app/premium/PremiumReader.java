package com.pocket.app.premium;

import com.pocket.app.AppMode;
import com.pocket.app.Feature;
import com.pocket.sdk2.api.legacy.PocketCache;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PremiumReader extends Feature {

    private final PocketCache pktcache;

    @Inject
    public PremiumReader(
            AppMode mode,
            PocketCache pktcache
    ) {
        super(mode);
        this.pktcache = pktcache;
    }

    @Override
    protected boolean isEnabled(Audience audience) {
        return pktcache.hasPremium();//  TODO check PREMIUM_READER once https://getpocket.atlassian.net/browse/P19-760 is completed
    }

}
