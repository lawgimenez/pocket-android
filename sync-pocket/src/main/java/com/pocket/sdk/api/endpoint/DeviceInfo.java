package com.pocket.sdk.api.endpoint;

/**
 * Information about a device connecting to v3.
 * This will be used to assemble the User-Agent header and/or parameters that are sent to all API requests.
 * If you have any questions about what values to use, check with the backend and analytics teams.
 * See the general v3 endpoint docs in the spec for more details.
 * @see Credentials
 * @see Endpoint
 */
public class DeviceInfo {

	/** The name of the OS, such as "Android", "iOS", etc. */
	public final String os;
	/** The version number of the OS. */
	public final String osVersion;
	/** The name of the company that made this device, such as "Apple", "Samsung", "Megadodo Publications" etc. */
	public final String deviceManufactuer;
	/** The model number or name of the device. */
	public final String deviceModel;
	/** Not sure how to explain this cross platform, but on android, the {@link android.os.Build#PRODUCT} */
	public final String deviceProduct;
	/** "tablet" for tablets and larger mobile devices. "mobile" for phones and handsets. */
	public final String deviceType;
	/** The device/user's preferred language as a code in the format of `en-us` */
	public final String locale;
	/** If known, the `User-Agent` that this device would typically use when opening a page in a browser. Something like "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5 Build/MMB29K; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/47.0.2526.100 Mobile Safari/537.36".  This is currently only used for spoc tracking. */
	public final String userAgent;
	
	/**
	 * @param os See {@link #os}
	 * @param osVersion See {@link #osVersion}
	 * @param deviceManufactuer See {@link #deviceManufactuer}
	 * @param deviceModel See {@link #deviceModel}
	 * @param deviceProduct See {@link #deviceProduct}
	 * @param deviceType See {@link #deviceType}
	 * @param locale See {@link #locale}
	 * @param userAgent See {@link #userAgent}
	 */
	public DeviceInfo(String os, String osVersion, String deviceManufactuer, String deviceModel, String deviceProduct, String deviceType, String locale, String userAgent) {
		this.os = os;
		this.osVersion = osVersion;
		this.deviceManufactuer = deviceManufactuer;
		this.deviceModel = deviceModel;
		this.deviceProduct = deviceProduct;
		this.deviceType = deviceType;
		this.locale = locale;
		this.userAgent = userAgent;
	}

}
