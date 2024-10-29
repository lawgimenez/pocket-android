package com.pocket.app.settings;

import android.content.Context;
import android.content.res.Configuration;
import android.view.View;

import com.pocket.app.AppLifecycle;
import com.pocket.app.AppLifecycleEventDispatcher;
import com.pocket.app.AppMode;
import com.pocket.app.Feature;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.action.Pv;
import com.pocket.sdk.api.generated.enums.CxtEvent;
import com.pocket.sdk.api.generated.enums.CxtSection;
import com.pocket.sdk2.analytics.context.Interaction;
import com.pocket.util.android.ApiLevel;
import com.pocket.util.prefs.BooleanPreference;
import com.pocket.util.prefs.Preferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * A new app setting to take advantage of a new system setting in Android Q. Users can now toggle between
 * light and dark theme for their whole device and if they like Pocket can always use the same theme as the system.
 */
@Singleton
public final class SystemDarkTheme extends Feature implements AppLifecycle {
	
	private final Context context;
	private final BooleanPreference turnedOnByUser;
	private final Analytics analytics;
	private final Theme theme;

	@Inject
	public SystemDarkTheme(
			Preferences prefs,
			Theme theme,
			AppMode mode,
			Pocket pocket,
			@ApplicationContext Context context,
			AppLifecycleEventDispatcher dispatcher
	) {
		super(mode);
		dispatcher.registerAppLifecycleObserver(this);
		this.theme = theme;
		this.turnedOnByUser = prefs.forUser("appThemeSystem", ApiLevel.hasSystemDarkTheme());
		this.analytics = new Analytics(pocket);
		this.context = context;
		updateTheme(getCurrentConfiguration(context));
	}
	
	@Override protected boolean isEnabled(Audience audience) {
		if (!ApiLevel.hasSystemDarkTheme()) return false; 
		
		return true;
	}
	
	@Override protected boolean isOn(Audience audience) {
		return super.isOn(audience) && turnedOnByUser.get();
	}
	
	public void turnOn(View view) {
		if (!turnedOnByUser.get()) {
			turnedOnByUser.set(true);
			updateTheme(getCurrentConfiguration(view.getContext()));
			analytics.track(view, CxtEvent.ENABLE, null);
		}
	}

	public void turnOn(Context context) {
		if (!turnedOnByUser.get()) {
			turnedOnByUser.set(true);
			updateTheme(getCurrentConfiguration(context));
		}
	}
	
	public void turnOff(View view) {
		if (turnedOnByUser.get()) {
			turnedOnByUser.set(false);
			updateTheme(getCurrentConfiguration(view.getContext()));
			analytics.track(view, CxtEvent.DISABLE, null);
		}
	}

	public void turnOff(Context context) {
		if (turnedOnByUser.get()) {
			turnedOnByUser.set(false);
			updateTheme(getCurrentConfiguration(context));
		}
	}
	
	BooleanPreference getPreference() {
		return turnedOnByUser;
	}
	
	@Override public void onLoggedIn(boolean isNewUser) {
		updateTheme(getCurrentConfiguration(context));
	}
	
	@Override public void onConfigurationChanged(Configuration configuration) {
		updateTheme(configuration);
	}

	@Override
	public LogoutPolicy onLogoutStarted() {
		return new LogoutPolicy() {
			@Override public void stopModifyingUserData() {}
			@Override public void deleteUserData() {}
			@Override public void restart() {}
			@Override public void onLoggedOut() {
				// switch users back to follow system theme on log out
				updateTheme(getCurrentConfiguration(context));
			}
		};
	}
	
	private Configuration getCurrentConfiguration(Context context) {
		return context.getResources().getConfiguration();
	}
	
	private void updateTheme(Configuration configuration) {
		if (!turnedOnByUser.isSet()) {
			// Pick a default for this
			boolean value;
			if (!ApiLevel.hasSystemDarkTheme()) {
				value = false;
			} else {
				final int current = theme.get();
				if (current == Theme.LIGHT) {
					// If they are using the default, let's opt them in,
					// in case they discover the system setting, but not ours.
					value = true;
					
				} else if (current == Theme.DARK && isSystemSetToDark(getCurrentConfiguration(context))) {
					// If they were using dark theme, but also enabled system dark theme
					// then let's opt them in so the themes stay in sync.
					value = true;
				} else {
					// If other
					value = false;
				}
			}
			turnedOnByUser.set(value);
		}
		
		if (isOn()) {
			final int currentTheme = theme.get();
			if (isSystemSetToLight(configuration) && currentTheme != Theme.LIGHT) {
				theme.set(Theme.LIGHT);
				
			} else if (isSystemSetToDark(configuration) && currentTheme != Theme.DARK) {
				theme.set(Theme.DARK);
			}
		}
	}
	
	private static boolean isSystemSetToLight(Configuration configuration) {
		return !isSystemSetToDark(configuration);
	}
	
	private static boolean isSystemSetToDark(Configuration configuration) {
		final int nightMode = configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK;
		return nightMode == Configuration.UI_MODE_NIGHT_YES;
	}
	
	private class Analytics {
		
		private final Pocket pocket;
		
		private Analytics(Pocket pocket) {
			this.pocket = pocket;
		}
		
		public void track(View context, CxtEvent event, String version) {
			
			// don't fire any analytics if this feature isn't enabled
			if (!isEnabled()) {
				return;
			}
			
			Interaction it = Interaction.on(context);
			
			Pv.Builder pv = pocket.spec().actions().pv()
					.section(CxtSection.FOLLOW_SYSTEM_THEME)
					.event(event)
					.event_type(9)
					.time(it.time)
					.context(it.context);
			
			if (version != null) {
				pv.version(version);
			}
			
			pocket.sync(null, pv.build());
		}
	}
}
