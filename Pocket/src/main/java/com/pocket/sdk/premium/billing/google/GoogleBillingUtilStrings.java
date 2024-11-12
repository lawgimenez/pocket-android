package com.pocket.sdk.premium.billing.google;

public abstract class GoogleBillingUtilStrings {

	static final Sku SKU_MONTHLY = 			new Sku("pocket.premium.1month.v3",
															"pocket.premium.1month");
	static final Sku SKU_YEARLY = 			new Sku("pocket.premium.1year.v3",
															"pocket.premium.1year.v2",
															"pocket.premium.1year");
	static final Sku SKU_AD_FREE_MONTHLY = 	new Sku("pocket.premium.adfree.1month.v1");
	static final Sku SKU_AD_FREE_YEARLY = 	new Sku("pocket.premium.adfree.1year.v1");

}
