package com.pocket.sdk.api;

import android.app.Activity;
import android.content.Context;

import androidx.fragment.app.FragmentActivity;

import com.pocket.app.ActivityMonitor;
import com.pocket.app.App;
import com.pocket.app.AppThreads;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.enums.CxtEvent;
import com.pocket.sdk.api.generated.enums.CxtPage;
import com.pocket.sdk.api.generated.enums.CxtSection;
import com.pocket.sdk.api.generated.enums.CxtSource;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.api.generated.enums.UserMessageActionType;
import com.pocket.sdk.api.generated.enums.UserMessageResult;
import com.pocket.sdk.api.generated.enums.UserMessageUi;
import com.pocket.sdk.api.generated.thing.UserMessage;
import com.pocket.sdk.api.generated.thing.UserMessageAction;
import com.pocket.sdk.api.generated.thing.UserMessageButton;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk2.analytics.context.Interaction;
import com.pocket.ui.view.dialog.DialogView;
import com.pocket.util.java.Safe;
import com.pocket.util.prefs.BooleanPreference;
import com.pocket.util.prefs.LongPreference;
import com.pocket.util.prefs.Preferences;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A server provided message to be shown to the user.
 * An example usage is notifying the user their premium subscription is expiring.
 * Original spec is https://docs.google.com/document/d/1gAv_TGHMEtK4E3KB9VtAhaHZqqyyV6-e1EKrq7MXDv8/edit#heading=h.dy9xthri1g9d
 * Also see {@link UserMessage}.
 * (This was previously Pocket/src/main/java/com/pocket/sdk/api/UserMessage.java)
 */
@Singleton
public class UserMessaging {
	
	private final LongPreference since;
	/** A preference group of boolean preferences where the key is the {@link UserMessage#message_id}*/
	private final Preferences seen;
	private final Pocket pocket;

	@Inject
	public UserMessaging(
			AppSync appSync,
			Pocket pocket,
			AppThreads threads,
			Preferences preferences,
			ActivityMonitor activities
	) {
		this.since = preferences.forUser("since_m", 0L);
		this.seen = preferences.group("umsg_");
		this.pocket = pocket;
		appSync.addFlags(get -> get.since_m(Timestamp.fromMillis(since.get())));
		appSync.addWork((__, get, ___) -> {
			if (get != null && get.userMessage != null) {
				threads.runOrPostOnUiThread(() -> {
					UserMessageResult result = show(get.userMessage, activities.getVisible(), true);
					if (result == UserMessageResult.SHOWN || result == UserMessageResult.ALREADY_SHOWN) {
						seenPref(get.userMessage).set(true);
						if (get.since != null) since.set(get.since.value);
					} else {
						// Other statuses we'll leave them as unviewed in case the app later updates to a version that supports them.
					}
				});
			}
			
			
			return null;
		});
	}
	
	private void track(CxtEvent action_identifier, int type_id, UserMessage msg, Context context, UserMessageResult reason_code) {
		Interaction it = Interaction.on(context);
		pocket.sync(null, pocket.spec().actions().pv_wt()
				.time(it.time)
				.context(it.context)
				.action_identifier(action_identifier)
				.type_id(type_id)
				.view(CxtView.MOBILE)
				.page(CxtPage.create(msg.message_id))
				.page_params(msg.message_ui_id.toString())
				.source(CxtSource.create(CxtSource.MESSAGE_.value+msg.message_id))
				.section(CxtSection.MESSAGE)
				.reason_code(reason_code)
				.build());
	}
	
	private UserMessageResult ignored(UserMessage msg, Context context, UserMessageResult reason_code) {
		track(CxtEvent.MSG_NOT_SHOWN, 4, msg, context, reason_code);
		return reason_code;
	}
	
	/**
	 * Attempt to show this message. This must be invoked from the ui thread.
	 * If the activity is unavailable or if the message isn't supported or already has been seen, nothing will be shown.
	 * <p>
	 * Note: This is only public so we can show test messages in review builds, otherwise not really much reason for this to be exposed externally.
	 * When using for fake messages, turn off the tracking parameter.
	 *
	 * @param message The message to show.
	 * @param on The activity to show it on.
	 * @param track True to send analytics for this message's events
	 * @return null if shown, otherwise a reason why it wasn't shown
	 */
	public UserMessageResult show(UserMessage message, Activity on, boolean track) {
		if (track) track(CxtEvent.MSG_DELIVERED, 4, message, on, null);
		
		if (seenPref(message).get()) return ignored(message, on, UserMessageResult.ALREADY_SHOWN);
		if (on == null || on.isFinishing()) return ignored(message, on, UserMessageResult.USER_NOT_PRESENT);
		if (message.message_ui_id == null || message.message_ui_id != UserMessageUi.MESSAGE_UI_CUSTOM_POPUP) return ignored(message, on, UserMessageResult.UNKNOWN_UI_FORMAT);
		
		if (track) track(CxtEvent.VIEW_MESSAGE, 1, message, on, null);
		
		UserMessageButton negative = Safe.get(() -> message.buttons.get(0));
		UserMessageButton positive = Safe.get(() -> message.buttons.get(1));
		
		DialogView.Binder dialog = new DialogView(on).bind()
				.title(message.title)
				.message(message.message);
		if (negative != null) dialog.buttonSecondary(negative.label, v -> {
			if (track) track(CxtEvent.CLICK_BUTTON_0, 2, message, v.getContext(), null);
			act(negative.action, on);
		});
		if (positive != null) dialog.buttonPrimary(positive.label, v -> {
			if (track) track(CxtEvent.CLICK_BUTTON_1, 2, message, v.getContext(), null);
			act(positive.action, on);
		});
		dialog.showAsAlertDialog(null, false);
		return UserMessageResult.SHOWN;
	}
	
	private void act(UserMessageAction action, Activity activity) {
		if (action.id == UserMessageActionType.CLOSE) {
			// Dialog will have already dismissed
			
		} else if (action.id == UserMessageActionType.PREMIUM || action.id == UserMessageActionType.RENEW) {
			App.from(activity).premium().showPremiumForUserState((FragmentActivity) activity, null);
			
		} else if (action.id == UserMessageActionType.BROWSER) {
			App.viewUrl(activity, action.meta.url.url);
			
		} else {
			// Unsupported action.
		}
	}
	
	/** A preference that describes whether this specific message (by its id) has been seen by the user. */
	private BooleanPreference seenPref(UserMessage message) {
		return seen.forUser(message.message_id, false);
	}
}
