package com.pocket.sdk.premium.billing.google;

import com.android.billingclient.api.BillingClient;

public abstract class GoogleBillingUtil {
	
	static final String TYPE = BillingClient.ProductType.SUBS;

	/**
	 * Has the normal premium product as the purchasable sku, and all other variants as purchased possibilities.
	 */
	public static final ProductList PREMIUM = new ProductList(TYPE, GoogleBillingUtilStrings.SKU_MONTHLY, GoogleBillingUtilStrings.SKU_YEARLY)
													.addAlternativeSkusMonthly(GoogleBillingUtilStrings.SKU_AD_FREE_MONTHLY)
													.addAlternativeSkusYearly(GoogleBillingUtilStrings.SKU_AD_FREE_YEARLY);

	/**
	 * Has the normal premium product as the purchasable sku, and all other variants as purchased possibilities.
	 */
	public static final ProductList AD_FREE = new ProductList(TYPE, GoogleBillingUtilStrings.SKU_AD_FREE_MONTHLY, GoogleBillingUtilStrings.SKU_AD_FREE_YEARLY)
													.addAlternativeSkusMonthly(GoogleBillingUtilStrings.SKU_MONTHLY)
													.addAlternativeSkusYearly(GoogleBillingUtilStrings.SKU_YEARLY);
}
