package com.pocket.sdk.tts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.text.TextUtils;

/**
 * Older api Compat class for {@link EngineInfo} and TtsEngines.
 * 
 * @author max
 *
 */
public class TtsEngineCompat {
	
	/**
     * Gets a list of all installed TTS engines.
     *
     * @return A list of engines package names. The list can be empty, but never {@code null}.
     */
	public static List<EngineInfoCompat> getEngines(Context context) {
		PackageManager pm = context.getPackageManager();
	    Intent intent = new Intent("android.intent.action.TTS_SERVICE"); // Same as TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE;
        List<ResolveInfo> resolveInfos =
                pm.queryIntentServices(intent, PackageManager.MATCH_DEFAULT_ONLY);
        
        if (resolveInfos == null) {
        	return Collections.emptyList();
        }

        List<EngineInfoCompat> engines = new ArrayList<EngineInfoCompat>(resolveInfos.size());

        for (ResolveInfo resolveInfo : resolveInfos) {
        	EngineInfoCompat engine = getEngineInfo(resolveInfo, pm);
            if (engine != null) {
                engines.add(engine);
            }
        }
        Collections.sort(engines);

        return engines;
	}
	
	private static EngineInfoCompat getEngineInfo(ResolveInfo resolve, PackageManager pm) {
        ServiceInfo service = resolve.serviceInfo;
        if (service != null) {
        	CharSequence label = service.loadLabel(pm);
        	
            return new EngineInfoCompat(service.packageName,
            		TextUtils.isEmpty(label) ? service.packageName : label.toString(),
            		service.getIconResource(),
            		resolve.priority,
            		isSystemEngine(service));
        }

        return null;
    }
	
	private static boolean isSystemEngine(ServiceInfo info) {
        final ApplicationInfo appInfo = info.applicationInfo;
        return appInfo != null && (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }
	
	public static class EngineInfoCompat implements Comparable<EngineInfoCompat> {
		
		/** package name */
		public final String name;
		public final String label;
		public final int icon;
		public final int priority;
		public final boolean system;
		
		private EngineInfoCompat(String name, String label, int icon, int priority, boolean system) {
			this.name = name;
			this.label = label;
			this.icon = icon;
			this.priority = priority;
			this.system = system;
		}

		@Override
		public int compareTo(EngineInfoCompat another) {
			if (system && !another.system) {
                return -1;
            } else if (another.system && !system) {
                return 1;
            } else {
                return another.priority - priority;
            }
		}
		
	}

}
