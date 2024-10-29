package com.pocket.app;

import com.pocket.util.prefs.LongPreference;
import com.pocket.util.prefs.Preferences;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * An app component for tracking feature usage stats. For now enables simply keep track of use count. If needed we
 * could also save time of last use or what was the app version, etc.
 * <p>
 * If we like this approach and agree that the previous one (of scattered one-shot prefs) is not enough for the scale
 * of our needs there are a couple things we should consider:
 * <ol>
 * <li>Migrating the old prefs to use this component instead.</li>
 * <li>Starting more proactively tracking usage of new features as we add them, so when later we want to add some logic
 * based on usage we have the data for upgrading users.</li>
 * </ol>
 */
@Singleton
public final class FeatureStats {
	
	public enum Feature {
		LISTEN("listen"),
		READER("reader");
		
		final String key;
		
		Feature(String key) {
			this.key = key;
		}
	}
	
	private final Preferences counts;
	private final LongPreference firstAppLaunchTime;

	@Inject
	public FeatureStats(Preferences prefs) {
		counts = prefs.group("fcnt");

		firstAppLaunchTime = prefs.forApp("firstAppTime", 0L);
		if (!firstAppLaunchTime.isSet()) {
			firstAppLaunchTime.set(System.currentTimeMillis());
		}
	}
	
	private LongPreference pref(Feature feature) {
		return counts.forUser(feature.key, 0L);
	}
	
	public long getUseCount(Feature feature) {
		return pref(feature).get();
	}
	
	public void trackUse(Feature feature) {
		LongPreference pref = pref(feature);
		pref.set(pref.get()+1);
	}

	public long getFirstAppLaunchTime() {
		return firstAppLaunchTime.get();
	}
}