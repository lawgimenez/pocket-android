package com.pocket.sdk.dev;

import android.content.Context;

import com.pocket.app.App;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * Tool for copying a team member's app state for debugging.
 * This was quickly thrown together and could obivously use improvement, but here is how to use it so far:
 *
 * 1. Have the person go into Alpha settings and select App Transplant
 * 2. Have them run this command in terminal `adb pull /sdcard/Android/data/com.ideashower.readitlater.pro/files/transplant ~/Downloads/transplant`
 * 	or for alpha `adb pull /sdcard/Android/data/com.pocket.team.a/files/transplant ~/Downloads/transplant`
 *  or if they don't have adb, use the Android File Manager to download that folder
 * 3. Have them send it to you, copy it to your Downloads folder
 * 4. Uninstall Pocket from your device.
 * 5. Put a break point into the first line of restore()
 * 6. Add this to the first line of App.onCreate:   `new AppTransplant(this).restore()`
 * 7. Build and run the build in debug mode, MAKE SURE the applicationId of your build matches the packagename the user had on their build
 * 8. When it hits the breakpoint run this `adb push ~/Downloads/transplant /sdcard/Android/data/com.ideashower.readitlater.pro/files/transplant`
 * 		(or whatever package name your build is)
 * 9. Advance the debugger.
 * 10. It should start up the app logged into their app state!
 */
public class AppTransplant {
	
	private final Context context;
	
	public AppTransplant(Context context) {
		this.context = context;
	}
	
	private File outputDir() {
		return new File(context.getExternalFilesDir(null), "transplant");
	}
	
	public void create() {
		try {
			File internal = appDir();
			File out = outputDir();
			FileUtils.copyDirectory(internal, out, pathname -> true);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}
	
	private File appDir() {
		return context.getFilesDir().getParentFile();
	}
	
	public void clear() {
		try {
			FileUtils.deleteDirectory(outputDir());
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}
	
}
