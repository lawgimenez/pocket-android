package com.pocket.sdk.premium.billing.google

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.pocket.app.App

/**
 * Loads the [GooglePlayProduct] that match the skus provided.
 *
 *
 * Returns a [Products] instance with the monthly and yearly products.
 *
 *
 * If there are new products available they will not be loaded. If the expected skus aren't available this will fail.
 */
class LoadInventoryTask internal constructor(
    private val skus: ProductList,
    private val client: BillingClient,
    private val callback: LoadInventoryCallbacks
) {
    fun execute() {
        loadAvailableProducts()
    }

    private fun loadAvailableProducts() {
        client.querySkuDetailsAsync(
            SkuDetailsParams.newBuilder()
                .setSkusList(skus.all)
                .setType(skus.type)
                .build()
        ) { billingResult: BillingResult, skuDetailsList: List<SkuDetails?>? ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productList = ArrayList<GooglePlayProduct>()
                if (skuDetailsList != null) {
                    for (skuDetails in skuDetailsList) {
                        productList.add(GooglePlayProduct(skuDetails))
                    }
                }
                val products = Products.create(skus, productList)
                if (products != null) {
                    loadCompletedPurchases(products) { responseCode ->
                        postUiOnComplete(responseCode, products)
                    }
                } else {
                    postUiOnComplete(BillingClient.BillingResponseCode.ERROR, null)
                }
            } else {
                postUiOnComplete(billingResult.responseCode, null)
            }
        }
    }

    private fun loadCompletedPurchases(products: Products, onLoaded: (Int) -> Unit) {
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchasesList ->
            val responseCode = result.responseCode
            if (responseCode == BillingClient.BillingResponseCode.OK) {
                products.attachPurchases(purchasesList)
                onLoaded(BillingClient.BillingResponseCode.OK)
            } else {
                // Error
                onLoaded(responseCode)
            }
        }
    }

    private fun postUiOnComplete(responseCode: Int, products: Products?) {
        App.getApp().threads().runOrPostOnUiThread { uiOnComplete(responseCode, products) }
    }

    private fun uiOnComplete(responseCode: Int, products: Products?) {
        if (products != null) {
            callback.onInventoryLoaded(products)
        } else {
            callback.onInventoryLoadError(
                if (responseCode == BillingClient.BillingResponseCode.OK) {
                    BillingClient.BillingResponseCode.ERROR
                } else {
                    responseCode
                }
            )
        }
    }

    internal interface LoadInventoryCallbacks {
        /**
         * Loading success
         * @param products Not null, has at least one or both of the monthly or yearly products.
         */
        fun onInventoryLoaded(products: Products?)

        /**
         * Loading failed
         * @param responseCode One of [BillingClient.BillingResponseCode]
         */
        fun onInventoryLoadError(responseCode: Int)
    }
}