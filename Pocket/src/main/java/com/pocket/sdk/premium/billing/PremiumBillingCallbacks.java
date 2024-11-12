package com.pocket.sdk.premium.billing;

import com.pocket.sdk.util.ErrorReport;
import com.pocket.sdk.premium.billing.google.Products;

public interface PremiumBillingCallbacks {
	/**
	 * The products have loaded and are ready for display and purchase
	 * @param products
	 */
	public void onProductsLoaded(Products products);
	
	/**
	 * The products could not be loaded
	 * @param error A reason why
	 */
	public void onProductsLoadError(PremiumBillingError error);
	
	/**
	 * The purchase is now being sent to Pocket's server
	 */
	public void onProductPurchaseActivationStarted();
	
	/**
	 * The purchase is completely successful including sending to Pocket's server.
	 */
	public void onProductPurchaseSuccess();
	
	/**
	 * The product could not be purchased
	 * @param error A reason why
	 */
	public void onProductPurchaseFailed(PremiumBillingError error);
	
	/**
	 * The product was purchased, but we weren't able to notify Pocket about it yet.
	 * @param errorReport
	 */
	public void onProductPurchaseActivationFailed(ErrorReport errorReport);
}