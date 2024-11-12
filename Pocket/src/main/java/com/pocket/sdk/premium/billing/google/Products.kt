package com.pocket.sdk.premium.billing.google

import com.android.billingclient.api.Purchase
import java.util.ArrayList

/**
 * The monthly and yearly products available and their purchased states.
 * Obtain an instance with [.create]
 */
class Products {
    private val monthlySkus = ArrayList<GooglePlayProduct>()
    private val yearlySkus = ArrayList<GooglePlayProduct>()

    /**
     * @return The monthly product to show to the user for purchase.
     */
    var monthly: GooglePlayProduct? = null
        private set

    /**
     * @return The yearly product to show to the user for purchase.
     */
    var yearly: GooglePlayProduct? = null
        private set

    private fun addMonthly(value: GooglePlayProduct) {
        monthlySkus.add(value)
    }

    private fun addYearly(value: GooglePlayProduct) {
        yearlySkus.add(value)
    }

    private fun setCurrentMonthly(value: GooglePlayProduct) {
        monthly = value
    }

    private fun setCurrentYearly(value: GooglePlayProduct) {
        yearly = value
    }

    private val isValid: Boolean
        get() = monthly != null || yearly != null

    /**
     * Adds purchased data to the products.
     */
    fun attachPurchases(purchases: List<Purchase>) {
        val all: MutableList<GooglePlayProduct> = ArrayList()
        all.addAll(monthlySkus)
        all.addAll(yearlySkus)
        for (purchase in purchases) {
            val sku: String? = purchase.skus.firstOrNull()
            if (sku.isNullOrBlank()) {
                continue  // invalid
            }
            for (product in all) {
                if (product.sku == sku) {
                    product.setPurchased(purchase.originalJson)
                }
            }
        }
    }

    companion object {
        /**
         * Combines what the app knows to be the latest skus, with what are available from Google Play's API
         * and creates a [Products] instance.
         *
         *
         * @param skus The [ProductList] that defines what the app wants to present to the user as options.
         * @param available Our google play products that are currently available from google's api and their data.
         */
        fun create(skus: ProductList, available: List<GooglePlayProduct>): Products? {
            val products = Products()
            for (product in available) {
                val sku = product.sku
                if (skus.isMonthly(sku)) {
                    products.addMonthly(product)
                    if (skus.isCurrentMonthly(sku)) {
                        products.setCurrentMonthly(product)
                    }
                } else if (skus.isYearly(sku)) {
                    products.addYearly(product)
                    if (skus.isCurrentYearly(sku)) {
                        products.setCurrentYearly(product)
                    }
                }
            }
            return if (products.isValid) {
                products
            } else {
                null
            }
        }
    }
}