package com.pocket.app.settings.beta;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.ideashower.readitlater.BuildConfig;
import com.ideashower.readitlater.R;
import com.pocket.app.PocketSingleton;
import com.pocket.app.PocketUiPlaygroundActivity;
import com.pocket.app.settings.AbsPrefsFragment;
import com.pocket.app.settings.view.preferences.ActionPreference;
import com.pocket.app.settings.view.preferences.MultipleChoicePreference;
import com.pocket.app.settings.view.preferences.Preference;
import com.pocket.app.settings.view.preferences.PreferenceViews;
import com.pocket.sdk.api.PocketServer;
import com.pocket.sdk.api.generated.enums.PremiumAllTimeStatus;
import com.pocket.sdk.api.generated.enums.PremiumFeature;
import com.pocket.sdk.api.generated.enums.UserMessageActionType;
import com.pocket.sdk.api.generated.enums.UserMessageUi;
import com.pocket.sdk.api.generated.thing.UserMessage;
import com.pocket.sdk.api.generated.thing.UserMessageAction;
import com.pocket.sdk.api.generated.thing.UserMessageButton;
import com.pocket.sdk.api.thing.AccountUtil;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.dev.AppTransplant;
import com.pocket.sdk.network.eclectic.EclecticHttp;
import com.pocket.sdk.preferences.AppPrefs;
import com.pocket.ui.view.edittext.LabeledEditText;
import com.pocket.util.android.ContextUtil;
import com.pocket.util.android.Email;
import com.pocket.util.android.PPActivity;
import com.pocket.util.android.fragment.FragmentUtil.FragmentLaunchMode;
import com.pocket.util.java.function.Take;
import com.pocket.util.prefs.EnumPreference;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class BetaConfigFragment extends AbsPrefsFragment {
	
	public static final int DEVCONFIG_PREMIUM_ACTUAL = 0;

	public static FragmentLaunchMode getLaunchMode() {
		return FragmentLaunchMode.ACTIVITY_DIALOG;
	}
	
	public static BetaConfigFragment newInstance() {
		return new BetaConfigFragment();
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (!(ContextUtil.getActivity(context) instanceof TCActivity)) {
			throw new RuntimeException("unsupported activity " + context);
		}
		if (!app().mode().isForInternalCompanyOnly()) {
			throw new RuntimeException("unsupported activity mode");
		}
	}
	
	@Override
	protected void createPrefs(ArrayList<Preference> prefs) {
		if (!app().mode().isForInternalCompanyOnly()) {
			return;
		}
		
		AppPrefs appPrefs = app().prefs();
		
		prefs.add(PreferenceViews.newHeader(this, "Tools and Settings", !prefs.isEmpty()));

		prefs.add(PreferenceViews.newActionBuilder(this, "Unleash Feature Toggles")
				.setOnClickListener(() -> startActivity(new Intent(getContext(), UnleashDebugActivity.class)))
				.build());

		prefs.add(PreferenceViews.newActionBuilder(this, "LeakCanary")
				.setOnClickListener(() -> startActivity(new Intent().setClassName(requireContext(), "leakcanary.internal.activity.LeakActivity")))
				.setEnabledWhen(() -> app().mode().isDevBuild())
				.build());

		// Api
		final PocketServer.DevConfig apiConfig = app().pktserver().devConfig();
		prefs.add(PreferenceViews.newMultipleChoiceBuilder(this, apiConfig.api, "Api Server")
				.addChoice(PocketServer.API_PRODUCTION)
				.addChoice("https://*" + BuildConfig.API_DEV_SUFFIX)
				.setOnItemSelectedListener(new MultipleChoicePreference.OnSelectedItemChangedListener() {
					   @Override
					   public boolean onItemSelected(View view, int newValue, DialogInterface dialog) {
							if (newValue == 1) {
								final EditText field = new EditText(getContext());
								field.setText(apiConfig.devServerPrefix.get());
								field.setHint("[blank]");
							 
								new AlertDialog.Builder(getContext())
										.setTitle("https://[blank]" + BuildConfig.API_DEV_SUFFIX)
										.setView(field)
										.setNegativeButton(R.string.ac_cancel, null)
										.setPositiveButton(R.string.ac_ok, (__, ___) -> {
											String host = field.getText().toString();
											if (!StringUtils.isBlank(host)) {
												apiConfig.api.set(1);
												apiConfig.devServerPrefix.set(host);
												dialog.dismiss();
												requireRestart();
											}
										})
										.show();
								return false;
							} else {
								apiConfig.devServerPrefix.set(null);
								dialog.dismiss();
								return true;
							}
					   }

					   @Override
					   public void onItemSelectionChanged(int newValue) {
						   requireRestart();
					   }})
				.build()
				.updateSummary(1, "https://" + StringUtils.defaultIfBlank(apiConfig.devServerPrefix.get(), "*") + BuildConfig.API_DEV_SUFFIX));
		
		prefs.add(PreferenceViews.newMultipleChoiceBuilder(this, apiConfig.parserApi, "Parser Server")
				.addChoice(PocketServer.ARTICLE_VIEW)
				.addChoice("Custom")
				.setOnItemSelectedListener(new MultipleChoicePreference.OnSelectedItemChangedListener() {
					@Override public boolean onItemSelected(View view, int newValue, DialogInterface dialog) {
						if (newValue == 1) {
							final EditText field = new EditText(getContext());
							field.setText(apiConfig.customParserApi.get());
							
							new AlertDialog.Builder(getContext())
									.setView(field)
									.setNegativeButton(R.string.ac_cancel, null)
									.setPositiveButton(R.string.ac_ok, (__, ___) -> {
										String host = field.getText().toString();
										if (!StringUtils.isBlank(host)) {
											apiConfig.parserApi.set(1);
											apiConfig.customParserApi.set(host);
											dialog.dismiss();
											requireRestart();
										}
									})
									.show();
							return false;
						} else {
							dialog.dismiss();
							return true;
						}
					}
					
					public void onItemSelectionChanged(int newValue) {
						requireRestart();
					}})
				.build()
				.updateSummary(1, StringUtils.defaultString(apiConfig.customParserApi.get())));


		String pushToken = app().push().getToken();
		String pushTokenSummary;
		if (pushToken != null) {
			pushTokenSummary = pushToken.substring(0, min(pushToken.indexOf(":"), 16)) + "…" +
					pushToken.substring(max(pushToken.lastIndexOf("-"), pushToken.length() - 16));
		} else {
			pushTokenSummary = "Token unavailable";
		}
		prefs.add(PreferenceViews.newActionBuilder(this, "FCM push notifications token (click to copy)")
				.setSummaryDefaultUnchecked(pushTokenSummary)
				.setOnClickListener(() -> app().clipboard().setText(pushToken, "Push token"))
				.build());

		prefs.add(PreferenceViews.newActionBuilder(this, "Pocket UID (click to copy)")
				.setSummaryDefaultUnchecked(app().pktcache().getUID())
				.setOnClickListener(() -> app().clipboard().setText(app().pktcache().getUID(), "UID"))
				.build());

		prefs.add(PreferenceViews.newMultipleChoiceBuilder(this, apiConfig.snowplow, "Snowplow collector")
				.addChoice("Production")
				.addChoice("Dev/Mini")
				.addChoice("Micro")
				.setOnItemSelectedListener(new MultipleChoicePreference.OnSelectedItemChangedListener() {
					@Override public boolean onItemSelected(View view, int newValue, DialogInterface dialog) {
						if (newValue == 2) {
							final EditText field = new EditText(getContext());
							field.setText(apiConfig.snowplowMicro.get());
							
							new AlertDialog.Builder(getContext())
									.setView(field)
									.setNegativeButton(R.string.ac_cancel, null)
									.setPositiveButton(R.string.ac_ok, (__, ___) -> {
										String host = field.getText().toString();
										if (!StringUtils.isBlank(host)) {
											apiConfig.snowplow.set(2);
											apiConfig.snowplowMicro.set(host);
											dialog.dismiss();
											requireRestart();
										}
									})
									.show();
							return false;
						} else {
							dialog.dismiss();
							return true;
						}
					}
					
					public void onItemSelectionChanged(int __) {
						requireRestart();
					}})
				.build()
				.updateSummary(2, StringUtils.defaultString(apiConfig.snowplowMicro.get())));
				
		
		if (app().pktcache().isLoggedIn()) {
			// Free / Premium
			String actualPrem = app().pktcache().hasPremium() ? "(Premium)" : "(Free)";
			prefs.add(PreferenceViews.newMultipleChoiceBuilder(this, appPrefs.DEVCONFIG_PREMIUM, "Premium Status")
					.addChoice("Actual " + actualPrem)
					.addChoice("Fake - Premium")
					.addChoice("Fake - Free")
					.setOnItemSelectedListener(newValue -> {
						switch (newValue) {
							case 0:
								toast("Retrieving your latest account info from server...");
								pocket().syncRemote(AccountUtil.getuser(pocket().spec()))
										.onSuccess(r -> toast("Premium Status reset back to Actual"))
										.onFailure(e -> toast("Couldn't load actual status, make sure you are online and try again"));
								break;
							case 1:
								pocket().sync(null, pocket().spec().actions().fake_premium_status()
										.premium_status(true)
										.premium_features(new ArrayList<>(PremiumFeature.values()))
										.premium_alltime_status(PremiumAllTimeStatus.ACTIVE)
										.time(Timestamp.now())
										.build());
								toast("Note: This does not effect your actual status. If the app syncs the latest account info, this fake status will be overridden back to the real one.");
								break;
							case 2:
								pocket().sync(null, pocket().spec().actions().fake_premium_status()
										.premium_status(false)
										.premium_features(Collections.emptyList())
										.premium_alltime_status(PremiumAllTimeStatus.NEVER)
										.time(Timestamp.now())
										.build());
								toast("Note: This does not effect your actual status. If the app syncs the latest account info, this fake status will be overridden back to the real one.");
								break;
						}
					})
					.build());
		}

		prefs.add(PreferenceViews.newActionBuilder(this, "Reset Session Id")
				.setOnClickListener(() -> {
					app().session().expire();
					requireRestart();
				})
				.build());

		prefs.add(PreferenceViews.newToggleSwitchBuilder(this, appPrefs.DEVCONFIG_SNACKBAR_ALWAYS_SHOW_URL_CR, "Always show Continue Reading and URL Save")
				.setSummaryDefaultUnchecked("Always show the Continue Reading and URL Clipboard save snackbars on app start")
				.setOnChangeListener(n -> toast("Exit and restart the app to view."))
				.build());

		prefs.add(PreferenceViews.newActionBuilder(this, "Edit Fake Device Info for Login")
				.setOnClickListener(
						new ActionPreference.OnClickAction() {
							
							private View newField(String label, String value, Take<String> set) {
								LabeledEditText editText = new LabeledEditText(getContext());
								editText.bind().clear()
										.label(label)
										.text(value);
								editText.setTag(set);
								return editText;
							}
							
							@SuppressLint("HardwareIds") @Override
							public void onClick() {
								final LinearLayout layout = new LinearLayout(getContext());
								layout.setOrientation(LinearLayout.VERTICAL);
								layout.addView(newField("Manufacturer", app().device().manufacturer(), v -> app().device().setOverrideManuf(v)));
								layout.addView(newField("Model", app().device().model(), v -> app().device().setOverrideModel(v)));
								layout.addView(newField("Product", app().device().product(), v -> app().device().setOverrideProduct(v)));
								layout.addView(newField("SID", app().device().sid(), v -> app().device().setOverrideSid(v)));
								layout.addView(newField("ANID", app().device().anid(), v -> app().device().setOverrideAnid(v)));
								new AlertDialog.Builder(getContext())
										.setView(layout)
										.setNegativeButton("Reset", (dialog, which) -> {
											int count = layout.getChildCount();
											for (int i = 0; i < count; i++) {
												LabeledEditText child = (LabeledEditText) layout.getChildAt(i);
												((Take<String>) child.getTag()).apply(null);
											}
											requireRestart();
										})
										.setNeutralButton("Cancel", null)
										.setPositiveButton("Save", (dialog, which) -> {
											int count = layout.getChildCount();
											for (int i = 0; i < count; i++) {
												LabeledEditText child = (LabeledEditText) layout.getChildAt(i);
												((Take<String>) child.getTag()).apply(child.getEditText().getText().toString());
											}
											requireRestart();
										})
										.show();
							}
						})
				.build());

		prefs.add(PreferenceViews.newHeader(this, "View Screens and Flows"));

		prefs.add(PreferenceViews.newActionBuilder(this, "UI Playground")
				.setOnClickListener(() -> startActivity(new Intent(getContext(), PocketUiPlaygroundActivity.class)))
				.setEnabledWhen(() -> app().pktcache().isLoggedIn())
				.build());

		prefs.add(PreferenceViews.newActionBuilder(this, "View Purchase View - Normal")
				.setOnClickListener(() -> app().premium().showUpgradeScreen(getActivity(), null))
				.setEnabledWhen(() -> app().pktcache().isLoggedIn())
				.build());
		
		prefs.add(PreferenceViews.newActionBuilder(this, "View Purchase Complete View")
				.setOnClickListener(() -> app().premium().showPurchaseComplete(getActivity(), null))
				.setEnabledWhen(() -> app().pktcache().isLoggedIn())
				.build());
		
		prefs.add(PreferenceViews.newActionBuilder(this, "Show popup User Message")
				.setOnClickListener(() -> app().messaging().show(
							new UserMessage.Builder()
								.message_id("fake_"+System.currentTimeMillis())
								.message_ui_id(UserMessageUi.MESSAGE_UI_CUSTOM_POPUP)
								.title("Your Premium Subscription Has Expired")
								.message("Your comped subscription to Pocket Premium has expired. Renew to keep using Premium features.")
								.buttons(
									Arrays.asList(
										new UserMessageButton.Builder()
											.label("No Thanks")
											.action(new UserMessageAction.Builder().id(UserMessageActionType.CLOSE).build())
											.build(),
										new UserMessageButton.Builder()
											.label("Renew")
											.action(new UserMessageAction.Builder().id(UserMessageActionType.RENEW).build())
											.build()
									))
								.build(),
							getAbsPocketActivity(),
							false))
				.build());
		
		
		
		prefs.add(PreferenceViews.newHeader(this, "Debugging and Logging"));

		prefs.add(PreferenceViews.newMultipleChoiceBuilder(this, "Network Logging Level", new MultipleChoicePreference.PrefHandler() {
					@Override public int getSelected() {
						switch (app().http().getLoggingLevel()) {
							case NONE: return 0;
							case API: return 1;
							case EVERYTHING: return 2;
						}
						return 0;
					}
					@Override public void setSelected(int index) {
						var http = app().http();
						switch (index) {
							case 0: http.setLoggingLevel(EclecticHttp.Logging.NONE); break;
							case 1: http.setLoggingLevel(EclecticHttp.Logging.API); break;
							case 2: http.setLoggingLevel(EclecticHttp.Logging.EVERYTHING); break;
							default: throw new RuntimeException("unknown " + index);
						}
					}
				})
				.addChoice("Off")
				.addChoice("API")
				.addChoice("Everything")
				.build());

		prefs.add(PreferenceViews.newMultipleChoiceBuilder(this, "Sync Logging Level", new MultipleChoicePreference.PrefHandler() {
					@Override
					public int getSelected() {
						switch (app().pocketSingleton().teamLoggingLevelPref().get()) {
							case OFF: return 0;
							case QA: return 1;
							case DEV: return 2;
							case DEBUG: return 3;
							case DEBUG_COMPACT: return 4;
							case PROFILING: return 5;
							default: throw new RuntimeException("unknown " + app().pocketSingleton().teamLoggingLevelPref().get());
						}
					}
					@Override
					public void setSelected(int index) {
						EnumPreference<PocketSingleton.LoggingLevel> pref = app().pocketSingleton().teamLoggingLevelPref();
						switch (index) {
							case 0 : pref.set(PocketSingleton.LoggingLevel.OFF); break;
							case 1 : pref.set(PocketSingleton.LoggingLevel.QA); break;
							case 2 : pref.set(PocketSingleton.LoggingLevel.DEV); break;
							case 3 : pref.set(PocketSingleton.LoggingLevel.DEBUG); break;
							case 4 : pref.set(PocketSingleton.LoggingLevel.DEBUG_COMPACT); break;
							case 5 : pref.set(PocketSingleton.LoggingLevel.PROFILING); break;
							default: throw new RuntimeException("unknown " + index);
						}
					}
				})
				.addChoice("Off")
				.addChoice("QA / Actions")
				.addChoice("Development")
				.addChoice("Debug Verbose")
				.addChoice("Debug Compact")
				.addChoice("Profiling")
				.setOnItemSelectedListener(newValue -> requireRestart())
				.build());
		
		// Dump Preferences
		prefs.add(PreferenceViews.newActionBuilder(this, "Email server request dump to Developers")
				.setOnClickListener(() -> {
					String data = 
							"SERVER REQUESTS\n" + app().pocketSingleton().dumpRequestLog();
					Email.startEmailIntent(
							null,
							"Data from " + app().pktcache().getUsername(),
							data,
							getContext(), null);
				})
				.build());
		
		prefs.add(PreferenceViews.newActionBuilder(this, "App Transplant")
				.setOnClickListener(
						() -> {
							toast("Copying...");
							new AppTransplant(getContext()).create();
							toast("Copied. Be sure to tap Clear Transplant after sending it.");
						})
				.build());
		
		prefs.add(PreferenceViews.newActionBuilder(this, "Clear Transplant")
				.setOnClickListener(() -> {
					new AppTransplant(getContext()).clear();
					toast("Cleared");
				})
				.build());
	}
	
	
	
	private void requireRestart() {
		new AlertDialog.Builder(getActivity())
				.setMessage("In order for this to take effect, the app will restart now")
				.setPositiveButton(R.string.ac_ok, null)
				.setOnDismissListener(dialog -> {
					getAbsPocketActivity().finishAllActivities(false);
					PPActivity.triggerRebirth(getActivity());
				})
				.show();
	}

	private void toast(CharSequence text) {
		var context = getContext();
		if (context != null) {
			Toast.makeText(context, text, Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	protected int getTitle() {
		return R.string.alpha_settings;
	}
	
	@Override
	protected View getBannerView() {
		return null;
	}
}
