package com.pocket.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;

import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.util.android.ApiLevel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Tracks the current {@link com.pocket.sdk.util.AbsPocketActivity}'s that are in play. These are activities that are
 * either starting, resumed or in the process of stopping. This can be used to know where the user might be coming from or
 * going to as Pocket's internal task stack changes.
 */
@Singleton
public class ActivityMonitor {

    public enum State {
        CREATED,
        RESTARTED,
        STARTED,
        RESUMED,
        PAUSED,
        STOPPED
    }

    private final HashMap<State, Activity> mActivities = new HashMap<>();
    private final Set<Listener> mListeners = new HashSet<>();

    @Inject
    ActivityMonitor() {}

    public void onActivityCreate(AbsPocketActivity activity) {
        set(activity, State.CREATED);

        activity.addOnLifeCycleChangedListener(new AbsPocketActivity.SimpleOnLifeCycleChangedListener() {
            @Override
            public void onActivityCreate(Bundle savedInstanceState, AbsPocketActivity activity) {
                // Not expected to happen since onCreate is what invokes the parent addListener
                set(activity, State.CREATED);
            }

            @Override
            public void onActivityRestart(AbsPocketActivity activity) {
                set(activity, State.RESTARTED);
            }

            @Override
            public void onActivityStart(AbsPocketActivity activity) {
                set(activity, State.STARTED);
            }

            @Override
            public void onActivityResume(AbsPocketActivity activity) {
                set(activity, State.RESUMED);
            }

            @Override
            public void onActivityPause(AbsPocketActivity activity) {
                set(activity, State.PAUSED);
            }

            @Override
            public void onActivityStop(AbsPocketActivity activity) {
                set(activity, State.STOPPED);
            }

            @Override
            public void onActivityDestroy(AbsPocketActivity activity) {
                set(activity, null);
            }
        });
    }

    /**
     *
     * @param activity
     * @param state The updated state or null to remove the activity from the monitor.
     */
    private void set(Activity activity, State state) {
        // Remove all references to this activity from any previous state
        Iterator<Map.Entry<State, Activity>> it = mActivities.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() == activity) {
                it.remove();
            }
        }

        // Attach it to the new state
        if (state != null) {
            mActivities.put(state, activity);
        }

        // Invoke listeners
        for (Listener listener : mListeners) {
            if (state == null) {
                continue;
            }
            switch (state) {
                case STARTED:
                    listener.onActivityStarted(activity);
                    break;
                case RESUMED:
                    listener.onActivityResumed(activity);
                    break;
                case PAUSED:
                    listener.onActivityPaused(activity);
                    break;
            }
        }
    }

    /**
     * @return An app activity that is visible to the user. In most cases this just means in the resumed or started state,
	 * 			but in a multi window mode, it could include paused.
	 * 			null if nothing is visible.
     */
    @SuppressLint("NewApi")
	public Activity getVisible() {
        if (mActivities.containsKey(State.RESUMED)) {
            return mActivities.get(State.RESUMED);

        } else if (mActivities.containsKey(State.STARTED)) {
			return mActivities.get(State.STARTED);
	
		} else if (mActivities.containsKey(State.PAUSED)) {
			Activity paused = mActivities.get(State.PAUSED);
			if (ApiLevel.isNougatOrGreater() && paused.isInMultiWindowMode()) {
				return paused;
			} else {
				return null;
			}

        } else {
            return null;
        }
    }
    
    /**
     * @return Returns the most relevant or recent Activity if available or null.
     */
    public Activity getAvailableContext() {
        if (mActivities.containsKey(State.RESUMED)) {
            return mActivities.get(State.RESUMED);

        } else if (mActivities.containsKey(State.STARTED)) {
            return mActivities.get(State.STARTED);

        } else if (mActivities.containsKey(State.CREATED)) {
            return mActivities.get(State.CREATED);

        } else if (mActivities.containsKey(State.RESTARTED)) {
            return mActivities.get(State.RESTARTED);

        } else if (mActivities.containsKey(State.PAUSED)) {
            return mActivities.get(State.PAUSED);

        } else if (mActivities.containsKey(State.STOPPED)) {
            return mActivities.get(State.STOPPED);

        } else {
            return null;
        }
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    public interface Listener {

        public void onActivityStarted(Activity activity);
        public void onActivityResumed(Activity activity);
        public void onActivityPaused(Activity activity);

    }

    public static class SimpleListener implements Listener {

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }
    }

}
