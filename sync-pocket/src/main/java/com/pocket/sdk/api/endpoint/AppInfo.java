package com.pocket.sdk.api.endpoint;

/**
 * Information about the app connecting to the v3 api. For the most part you will want to check with the data and server teams for what values you should use here.
 * @see Credentials
 */
public class AppInfo {

	public final String consumerKey;
	public final String company;
	public final String product;
	public final String productVersion;
	public final String build;
	public final String store;
	
	public AppInfo(String consumerKey, String company, String product, String productVersion, String build, String store) {
		this.consumerKey = consumerKey;
		this.company = company;
		this.product = product;
		this.productVersion = productVersion;
		this.build = build;
		this.store = store;
	}

}
