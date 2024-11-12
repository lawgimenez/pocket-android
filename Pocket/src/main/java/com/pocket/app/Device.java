package com.pocket.app;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import com.pocket.util.android.ApiLevel;
import com.pocket.util.java.Safe;
import com.pocket.util.prefs.Preferences;
import com.pocket.util.prefs.StringPreference;

import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Helper for getting device info and/or overriding the device info we send to various endpoints to test certain features in team and dev builds.
 */
@Singleton
public class Device {
	
	private final boolean overridesEnabled;
	private final String defaultManuf;
	private final String defaultModel;
	private final String defaultProduct;
	private final String defaultAnid;
	private final String defaultSid;
	private final StringPreference overrideManuf;
	private final StringPreference overrideModel;
	private final StringPreference overrideProduct;
	private final StringPreference overrideAnid;
	private final StringPreference overrideSid;
	
	/**
	 * Creates defaults based on the running Android environment
	 */
	@Inject
	public Device(@ApplicationContext Context context, Preferences prefs, AppMode mode) {
		this(prefs, mode, Build.MANUFACTURER, Build.MODEL, Build.PRODUCT,
				Safe.get(() -> Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)),
				Safe.get(() -> ApiLevel.isPreOreo() ? Build.SERIAL : null));
	}
	
	/**
	 * Manually supply defaults.
	 */
	public Device(Preferences prefs, AppMode mode, String defaultManuf, String defaultModel, String defaultProduct, String defaultAnid, String defaultSid) {
		this.overridesEnabled = mode.isForInternalCompanyOnly();
		
		this.defaultManuf = defaultManuf;
		this.defaultModel = defaultModel;
		this.defaultProduct = defaultProduct;
		this.defaultAnid = defaultAnid;
		this.defaultSid = defaultSid;
		
		Preferences group = prefs.group("dcfig_device");
		this.overrideManuf = group.forApp("device_manuf", (String) null);
		this.overrideModel = group.forApp("device_model", (String) null);
		this.overrideProduct = group.forApp("device_product", (String) null);
		this.overrideAnid = group.forApp("device_anid", (String) null);
		this.overrideSid = group.forApp("device_sid", (String) null);
	}
	
	public String manufacturer() {
		return get(defaultManuf, overrideManuf);
	}
	
	public String model() {
		return get(defaultModel, overrideModel);
	}
	
	public String product() {
		return get(defaultProduct, overrideProduct);
	}
	
	public String anid() {
		return get(defaultAnid, overrideAnid);
	}
	
	public String sid() {
		return get(defaultSid, overrideSid);
	}
	
	private String get(String defaultValue, StringPreference overridePref) {
		return overridesEnabled ? StringUtils.defaultIfEmpty(overridePref.get(), defaultValue) : defaultValue;
	}
	
	public void setOverrideManuf(String value) {
		if (overridesEnabled) overrideManuf.set(value);
	}
	
	public void setOverrideModel(String value) {
		if (overridesEnabled) overrideModel.set(value);
	}
	
	public void setOverrideProduct(String value) {
		if (overridesEnabled) overrideProduct.set(value);
	}
	
	public void setOverrideAnid(String value) {
		if (overridesEnabled) overrideAnid.set(value);
	}
	
	public void setOverrideSid(String value) {
		if (overridesEnabled) overrideSid.set(value);
	}
	
}
