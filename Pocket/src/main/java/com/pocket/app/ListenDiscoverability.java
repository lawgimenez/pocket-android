package com.pocket.app;

import android.view.View;

import com.ideashower.readitlater.R;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.action.Pv;
import com.pocket.sdk.api.generated.enums.CxtEvent;
import com.pocket.sdk.api.generated.enums.CxtSection;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.preferences.AppPrefs;
import com.pocket.sdk.tts.Listen;
import com.pocket.sdk.tts.TrackKt;
import com.pocket.sdk.util.AbsPocketFragment;
import com.pocket.sdk.util.view.tooltip.Tooltip;
import com.pocket.sdk.util.view.tooltip.theme.FocusTheme;
import com.pocket.sdk.util.view.tooltip.view.TooltipFocusBackground;
import com.pocket.sdk2.analytics.context.Interaction;
import com.pocket.sdk2.api.legacy.PocketCache;
import com.pocket.util.java.Milliseconds;
import com.pocket.util.java.Safe;
import com.pocket.util.prefs.BooleanPreference;
import com.pocket.util.prefs.LongPreference;

import org.threeten.bp.Clock;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Shows Listen Discoverability "notifications"—a series of prompts and tooltips designed to 
 * inform more users about Listen, a great feature relatively few know about.
 * <p>
 * Here's
 * <a href="https://docs.google.com/document/d/19NDNSUeoz9cZNjjFDf8U-bWqlBOdD3PfdAuvYh97Iww/edit#">the full spec</a>.
 */
@Singleton
public final class ListenDiscoverability {
	
	private static final long MINIMUM_MILLISECONDS_BETWEEN_MESSAGES = Milliseconds.DAY * 5;
	private static final long MAXIMUM_ACCOUNT_AGE_FOR_READER_TOOLTIP = Milliseconds.DAY * 30;
	private static final int MINIMUM_UNREAD_COUNT_FOR_MY_LIST_TOAST = 10;
	private static final int MAXIMUM_EXACT_UNREAD_COUNT_SHOWN_IN_MESSAGE = 4999;
	private final AppMode mode;
	private final Pocket pocket;
	private final PocketCache pktcache;
	private final FeatureStats featureStats;
	private final Listen listen;
	private final Clock clock;
	private final BooleanPreference hasShownReaderTooltip;
	private final BooleanPreference hasShownMyListToast;
	private final BooleanPreference forceA;
	private final BooleanPreference forceB;
	private final LongPreference lastShownTime;
	private final AppThreads threads;

	@Inject
	public ListenDiscoverability(
			AppThreads threads,
			AppMode mode,
			Pocket pocket,
			PocketCache pktcache,
			FeatureStats featureStats,
			Listen listen,
			AppPrefs prefs,
			Clock clock
	) {
		this(mode, threads, pocket, pktcache, featureStats, listen, clock,
				prefs.LISTEN_HAS_SHOWN_INTRO_A,
				prefs.LISTEN_HAS_SHOWN_INTRO_B,
				prefs.DEVCONFIG_LISTEN_DISCOVERABILITY_FORCE_A,
				prefs.DEVCONFIG_LISTEN_DISCOVERABILITY_FORCE_B,
				prefs.LISTEN_LAST_SHOWN_INTRO_TIME);
	}
	
	public ListenDiscoverability(AppMode mode,
			AppThreads threads,
			Pocket pocket,
			PocketCache pktcache,
			FeatureStats featureStats,
			Listen listen,
			Clock clock,
			BooleanPreference hasShownReaderTooltip,
			BooleanPreference hasShownMyListToast,
			BooleanPreference forceA,
			BooleanPreference forceB,
			LongPreference lastShownTime) {
		this.mode = mode;
		this.threads = threads;
		this.pktcache = pktcache;
		this.pocket = pocket;
		this.featureStats = featureStats;
		this.listen = listen;
		this.clock = clock;
		this.hasShownReaderTooltip = hasShownReaderTooltip;
		this.hasShownMyListToast = hasShownMyListToast;
		this.forceA = forceA;
		this.forceB = forceB;
		this.lastShownTime = lastShownTime;
	}
	
	// Notification A
	public void showReaderTooltipIfCriteriaMet(View anchor, Item item) {
		if (mode.isForInternalCompanyOnly() && forceA.get()) {
			// Skip checking most of the conditions.
		} else {
			if (hasShownReaderTooltip.get()) return;
			if (hasShownAnotherMessageRecently()) return;
			if (Safe.getLong(() -> pktcache.account().birth.millis()) == 0) {
				// If birth isn't known, don't show the tooltip -- can't assume it's a new user.
				return;
			}
			if (getAccountAge() > MAXIMUM_ACCOUNT_AGE_FOR_READER_TOOLTIP) return;
			if (featureStats.getUseCount(FeatureStats.Feature.READER) < 3) return;
		}
		if (!listen.isListenable(TrackKt.toTrack(item))) return;
		
		Tooltip.DefaultTheme.showButton(anchor,
				R.string.listen_reader_tooltip,
				new Tooltip.TooltipListener() {
					@Override public void onTooltipShown() {
						trackReaderTooltipShown(anchor);
					}
					
					@Override public void onTooltipFailed() {}
					
					@Override public void onTooltipDismissed(Tooltip.DismissReason reason) {
						if (reason == Tooltip.DismissReason.ANCHOR_CLICKED) {
							pocket.sync(null, createReaderTooltipBasePv(anchor).event(CxtEvent.CLICK_LISTEN_ICON).build());
						}
					}
				});
	}
	
	private long getAccountAge() {
		return clock.millis() - Safe.getLong(() -> pktcache.account().birth.millis());
	}
	
	private void trackReaderTooltipShown(View on) {
		hasShownReaderTooltip.set(true);
		trackShownTime();
		
		pocket.sync(null, createReaderTooltipBasePv(on).build());
	}
	
	private Pv.Builder createReaderTooltipBasePv(View on) {
		final Interaction it = Interaction.on(on);
		return pocket.spec().actions().pv()
				.view(CxtView.READER)
				.section(CxtSection.LISTEN_TOOLTIP)
				.context(it.context)
				.time(Timestamp.fromMillis(clock.millis()));
	}
	
	// Notification B
	// previously called on my list.  removed for now, while users are switching to FXA accounts
	public void showMyListToastIfCriteriaMet(AbsPocketFragment fragment, View anchor) {
		boolean skipConditions;
		if (mode.isForInternalCompanyOnly() && forceB.get()) {
			skipConditions = true;
			
		} else {
			skipConditions = false;
			if (notAllMyListToastCriteriaMet()) return;
		}
		
		pocket.sync(pocket.spec().things().listCounts().local(true).build())
			.onSuccess(counts -> threads.runOrPostOnUiThread( () -> {
				int unreadCount = counts.unread;
				if (fragment.isDetachedOrFinishing()) return;
				if (skipConditions) {
					// Don't check.
				} else {
					if (notAllMyListToastCriteriaMet()) return;
					if (unreadCount < MINIMUM_UNREAD_COUNT_FOR_MY_LIST_TOAST) return;
				}
				
				final String title;
				if (unreadCount <= MAXIMUM_EXACT_UNREAD_COUNT_SHOWN_IN_MESSAGE) {
					title = anchor.getResources()
							.getQuantityString(R.plurals.listen_my_list_toast_title, unreadCount, unreadCount, "⚡");
					
				} else {
					title = anchor.getResources().getString(R.string.listen_my_list_toast_title_5000_plus, "⚡");
				}
				final int buttonPositive = R.string.listen_my_list_toast_button;
				final int buttonNeutral = R.string.ac_no_thanks;
				new FocusTheme(TooltipFocusBackground.Shape.CIRCLE, title, buttonPositive, buttonNeutral)
						.showButton(anchor,
								R.string.listen_my_list_toast_message,
								new Tooltip.TooltipListener() {
									@Override public void onTooltipShown() {
										trackMyListToastShown(anchor);
									}
									
									@Override public void onTooltipFailed() {}
									
									@Override public void onTooltipDismissed(Tooltip.DismissReason reason) {
										switch (reason) {
											case DISMISS_REQUESTED:
												pocket.sync(null,
														createListenToastBasePv(anchor).event(CxtEvent.CLICK_DISMISS).build());
												break;
											case ANCHOR_CLICKED:
												pocket.sync(null,
														createListenToastBasePv(anchor).event(CxtEvent.CLICK_LISTEN_ICON).build());
												break;
											case BUTTON_CLICKED:
												pocket.sync(null,
														createListenToastBasePv(anchor).event(CxtEvent.CLICK_TRY_LISTEN).build());
												break;
										}
									}
								});
			}));
	}
	
	private boolean notAllMyListToastCriteriaMet() {
		if (hasShownAnotherMessageRecently()) return true;
		if (hasShownMyListToast.get()) return true;
		if (featureStats.getUseCount(FeatureStats.Feature.LISTEN) > 0) return true;
		return false;
	}
	
	private void trackMyListToastShown(View on) {
		hasShownMyListToast.set(true);
		trackShownTime();
		
		pocket.sync(null, createListenToastBasePv(on).build());
	}
	
	private Pv.Builder createListenToastBasePv(View on) {
		final Interaction it = Interaction.on(on);
		return pocket.spec().actions().pv()
				.view(CxtView.LIST)
				.section(CxtSection.LISTEN_TOAST)
				.context(it.context)
				.time(Timestamp.fromMillis(clock.millis()));
	}
	
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //
	private boolean hasShownAnotherMessageRecently() {
		return clock.millis() - lastShownTime.get() < MINIMUM_MILLISECONDS_BETWEEN_MESSAGES;
	}
	
	private void trackShownTime() {
		lastShownTime.set(clock.millis());
	}
}
