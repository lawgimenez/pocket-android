package com.pocket.app.help;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Environment;
import android.webkit.WebView;

import com.pocket.app.App;
import com.pocket.app.PocketApp;
import com.pocket.sdk.build.AppVersion;
import com.pocket.sdk.offline.cache.AssetUser;
import com.pocket.sdk.preferences.AppPrefs;
import com.pocket.sdk.tts.TtsEngineCompat;
import com.pocket.sdk.tts.TtsEngines;
import com.pocket.sdk.util.ErrorReport;
import com.pocket.sdk2.api.legacy.PocketCache;
import com.pocket.util.android.ApiLevel;
import com.pocket.util.android.Email;
import com.pocket.util.android.FormFactor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Help {

	public enum Type {
		// TODO check with support if they still have work flows based on these subject lines, would be nice to remove this api
		SYNC,
		FETCH
	}

	public static void requestHelp(Type type, ErrorReport error, Context context) {
		String subject = null;
		if (type != null) {
			switch (type) {
			case SYNC:
				subject = "Android App - Need help syncing";
				break;
			case FETCH:
				subject = "Android App - Need help downloading my list";
				break;
			}
		}
		if (subject == null) subject = "Android App Error";
		requestHelp(context, getSupportEmail(), subject, "", true, false, error, null);
	}
	
	public static String getSupportEmail() {
		return "support@getpocket.com";
	}

	public static void requestHelp(Context context, String to, String subject, String message, boolean includeSupportSpecs, boolean includeTroubleshooterData, ErrorReport error, String screenshotPath) {
        ArrayList<Email.Attachment> attachments = new ArrayList<>();
        message = StringUtils.defaultString(message);
		
		if (includeSupportSpecs) {
			message = concatBuildAndDeviceInfo(message);
			message = concatSettings(message, App.from(context));
		}
		
		message = concatErrorInfo(message, error);

        if (includeTroubleshooterData) {
            message = attachTroubleshooterData(message, attachments);
        }

        if (screenshotPath != null && App.from(context).mode().isForInternalCompanyOnly()) { // For now, screen shots are only supported for internal Pocket builds. Not production ready. Sending a bad path can basically break Gmail.
            attachments.add(new Email.Attachment("application/image", screenshotPath));
        }
		
		Email.startEmailIntent(to, subject, message, context, attachments);
	}

    private static String attachTroubleshooterData(String message, ArrayList<Email.Attachment> attachments) {
        String report = App.getApp().troubleshooter().getFormattedReport();
        if (StringUtils.isEmpty(report)) {
            return message;
        }

        // Try to attach a text file via external storage for sharing, if it fails, just attach the text to the message.
        try {
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                throw new IOException("external storage unavailable");
            }

            // Attach as text file
            String path = Environment.getExternalStorageDirectory().toString() + "/Android/data/" + App.getContext().getPackageName() + "/files/bug_report.txt";
            FileUtils.writeStringToFile(new File(path), report);
            long bytes = FileUtils.sizeOf(new File(path));
            App.getApp().assets().registerAssetUser(path, AssetUser.forSession());
			App.getApp().assets().written(path, bytes);

            attachments.add(new Email.Attachment("text/plain", "file://" + path));

        } catch (Throwable ignore) {
            // Append to message
            message += "\n\n\nTroubleshooting Logs:\n";
            message += report;
        }

        return message;
    }

    public static String concatBuildAndDeviceInfo(String info) { // OPT String builder
		App app = App.getApp();
		AppVersion version =  app.build();
		info += "\n\n\n";
		PocketCache pktcache = app.pktcache();
		if (pktcache.hasUserSetUsername()) {
			info += "Username: " + pktcache.getUsername() + "\n";
		} else {
			info += "User Id: " + pktcache.getUsername() + "\n";
		}
		info += "Primary Email: " + StringUtils.defaultString(pktcache.getEmail()) + "\n";
		info += "Version: " + version.getVersionName() + " (" + version.getVersionCode()+ ") \n";
		info += "Market: " + (isSideloaded(app) ? "Direct" : version.getStoreName(true)) + "\n";
		info += "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n";
		info += "Type: " + FormFactor.getLabel() + "\n";
		info += "Android: " + Build.VERSION.RELEASE + "\n";
		info += "Language: " + App.getContext().getResources().getConfiguration().locale + "\n";
		if (App.getApp().pktcache().hasPremium()) {
			info += "Premium: Yes \n";
		}
		if (ApiLevel.isOreoOrGreater()) {
			info += concatWebViewInfo();
		}
		return info;
	}

	/** Returns true is the app was installed via an APK download, such as the Mozilla archive site */
	private static boolean isSideloaded(Context context) {
		return context.getPackageManager().getInstallerPackageName(context.getPackageName()) == null;
	}
	
	@TargetApi(26)
	private static String concatWebViewInfo() {
		PackageInfo webViewPackageInfo = WebView.getCurrentWebViewPackage();
		return "WebView: " + webViewPackageInfo.versionName + " " + webViewPackageInfo.packageName;
	}
	
	private static String concatSettings(String info, PocketApp app) {
		AppPrefs prefs = App.getApp().prefs();
		String download;
		if (prefs.DOWNLOAD_TEXT.get()) {
			download = "Article Only";
		} else {
			download = "None";
		}
		info += "Download Setting: " + download + "\n";

		info += app.assets().getHelpInfo();
		info += "Background Sync: " + App.getStringResource(App.getApp().backgroundSync().getSelectedSettingLabel()) + "\n";
		info += "Listen: ";
		if (app.prefs().LISTEN_USE_STREAMING_VOICE.get()) {
			info += "Streaming";
		} else {
			TtsEngineCompat.EngineInfoCompat engine = new TtsEngines().getPreferredTtsEngine();
			info += engine != null ? engine.label : "Local";
		}
		info += "\n";
		return info;
	}
	
	private static String concatErrorInfo(String info, ErrorReport error) {
		if (error == null) {
			return info;
		}
		
		info += "\n";
		info += "Error Message: " + StringUtils.defaultString(error.messageSeenByUser) + "\n";
		info += "Screen: " + StringUtils.defaultString(error.activity) + "\n"; // TODO on tablets this should be getting the current fragment
		if (error.cause != null) {
			info += "Error Details: " + ExceptionUtils.getStackTrace(error.cause) + "\n";
		}
		return info;
	}
}
