package com.pocket.sdk;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import com.pocket.sdk.api.endpoint.AndroidDeviceInfo;
import com.pocket.sdk.api.endpoint.AppInfo;
import com.pocket.sdk.api.endpoint.DeviceInfo;
import com.pocket.sdk.api.generated.Modeller;
import com.pocket.sync.source.threads.AndroidThreadPools;
import com.pocket.sync.source.threads.AndroidUiThreadPublisher;
import com.pocket.sync.space.mutable.MutableSpace;
import com.pocket.sync.space.persist.DumbStorage;
import com.pocket.sync.space.persist.MigrationStorage;
import com.pocket.sync.space.persist.SqliteBinaryStorage;
import com.pocket.sync.value.protect.TinkEncrypter;
import com.pocket.util.android.ApiLevel;

/**
 * Factories and builders for creating {@link Pocket} instances with defaults for Android platform 
 */ 
public class AndroidPocket {
	
	/**
	 * A Pocket with the default settings for an Android device.
	 */
	public static Pocket pocket(Context context, AppInfo app) {
		return new Pocket(new Config.Builder(context, app).build());
	}
	
	public static Pocket.AuthenticationExtras authenticationExtras(Context context, String referrer)  {
		return new Pocket.AuthenticationExtras(referrer,
				Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID),
				ApiLevel.isPreOreo() ? Build.SERIAL : null);
	}
	
	public interface Config {
		class Builder extends Pocket.Config.Builder {
			
			private final Context context;
			
			/**
			 * A new config builder with the default settings and device info for an Android 
			 * environment.
			 */
			public Builder(Context context, AppInfo app) {
				this(context, app, new AndroidDeviceInfo(context));
			}
			
			/**
			 * A new config builder with the default settings for an Android environment.
			 */
			public Builder(Context context, AppInfo app, DeviceInfo deviceInfo) {
				super("pkt", app, deviceInfo);
				this.context = context;
			}
			
			@Override public Pocket.Config build() {
				if (publisher == null) publisher = new AndroidUiThreadPublisher();
				if (threads == null) threads = new AndroidThreadPools();
				if (space == null) {
					DumbStorage storage = new SqliteBinaryStorage(context,
							name,
							threads,
							Modeller.OBJECT_MAPPER,
							new TinkEncrypter(context, name));
					if (migration != null) storage = new MigrationStorage(migration, storage);
					space = new MutableSpace(storage);
				}
				
				return super.build();
			}
		}
	}
	
	private AndroidPocket() {
		throw new AssertionError("No instances.");
	}
}
