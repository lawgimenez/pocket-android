package com.pocket.sdk.premium.billing.google

import android.app.Activity
import android.os.Bundle
import com.android.billingclient.api.*
import com.pocket.app.App
import com.pocket.sdk.api.thing.AccountUtil
import com.pocket.sdk.api.value.Timestamp
import com.pocket.sdk.premium.billing.PremiumBillingCallbacks
import com.pocket.sdk.premium.billing.PremiumBillingError
import com.pocket.sdk.premium.billing.PremiumProduct
import com.pocket.sdk.premium.billing.google.LoadInventoryTask.LoadInventoryCallbacks
import com.pocket.sdk.util.ErrorReport
import com.pocket.sync.source.result.SyncException
import com.pocket.util.android.BundleUtil

/**
 * Methods to access our IAP subscription products.
 *
 *
 * To use, create an instance and get a list of products with [.getProductsAsync]. To start a purchase flow
 * with Google, pass one of those products to [.startPurchase].
 *
 *
 * See those methods for more information about implementation.
 *
 *
 * **Be sure to call [.disconnect]** when you are finished with this. For example, in onDestroy of your activity or fragment.
 * @param skus The list of skus to load
 */
class GooglePlayBilling(
    private val skus: ProductList,
    private val activity: Activity,
    private val callbacks: PremiumBillingCallbacks,
    savedInstanceState: Bundle?
) {
    enum class SendType {
        NEW_PURCHASE, RESTORE
    }

    private val client: BillingClient = BillingClient.newBuilder(activity)
        .setListener(this::updatePurchases)
        .enablePendingPurchases()
        .build()
    private var products: Products? = null
    private var pendingProduct: GooglePlayProduct? = null

    init {
        if (savedInstanceState != null) {
            pendingProduct = BundleUtil.getParcelable(savedInstanceState,
                STATE_PENDING_PURCHASE,
                GooglePlayProduct::class.java)
        }
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(STATE_PENDING_PURCHASE, pendingProduct)
    }

    /**
     * Creates a new connection to Google Play Billing and loads the available products. If [.getProductsAsync]
     * has been invoked, it will callback to the [PremiumBillingCallbacks]. Otherwise it will quietly fail or load.
     *
     *
     * If already connected or connecting this does nothing.
     */
    private fun connect() {
        if (client.connectionState == BillingClient.ConnectionState.CONNECTED
            || client.connectionState == BillingClient.ConnectionState.CONNECTING) {
            return  // Already connecting or connected
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                // REVIEW anything to do here?

                // The documentation has this to say:
                // Implement here your own retry policy to handle lost connections to Google Play
                // in the event the client loses connection. For example, the BillingClient may
                // lose its connection if the Google Play Store service is updating in the
                // background. The BillingClient must call the startConnection() method to restart
                // the connection before making further requests.
                // https://developer.android.com/google/play/billing/billing_library_overview#Connect
            }

            override fun onBillingSetupFinished(result: BillingResult) {
                if (client.connectionState == BillingClient.ConnectionState.DISCONNECTED) {
                    return
                }
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    LoadInventoryTask(skus, client, object : LoadInventoryCallbacks {
                        override fun onInventoryLoaded(products: Products?) {
                            this@GooglePlayBilling.products = products
                            callbacks.onProductsLoaded(products)
                        }

                        override fun onInventoryLoadError(responseCode: Int) {
                            invokeProductLoadFailedCallback(responseCode)
                        }
                    }).execute()
                } else {
                    invokeProductLoadFailedCallback(result.responseCode)
                }
            }
        })
    }

    /**
     * Invoke [PremiumBillingCallbacks.onProductsLoadError] if [.mIsAwaitingProductCallback].
     * @param code One of [BillingClient.BillingResponseCode]
     */
    private fun invokeProductLoadFailedCallback(code: Int) {
        client.endConnection()
        callbacks.onProductsLoadError(createError(code, null))
    }

    /**
     * Creates a [PremiumBillingError] based on the error code.
     */
    private fun createError(responseCode: Int, error: ErrorReport?): PremiumBillingError =
        when (responseCode) {
            BillingClient.BillingResponseCode.USER_CANCELED ->
                PremiumBillingError(PremiumBillingError.Type.CANCEL, error)
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                PremiumBillingError(PremiumBillingError.Type.ALREADY_PURCHASED, error)
            else -> if (App.from(activity).http().status().isOnline) {
                PremiumBillingError(PremiumBillingError.Type.FATAL, error)
            } else {
                // Assume connection error for safety
                PremiumBillingError(PremiumBillingError.Type.TEMPORARY, error)
            }
        }

    fun disconnect() {
        client.endConnection()
    }

    /**
     * Will asynchronously load return the results (or an error) via the [PremiumBillingCallbacks] set in the constructor.
     */
    fun getProductsAsync() {
        if (client.connectionState == BillingClient.ConnectionState.DISCONNECTED) {
            connect()
            return
        }
        if (products != null) {
            callbacks.onProductsLoaded(products)
        } else {
            connect()
        }
    }

    /**
     * Will asynchronously start the Google Play Billing purchase flow for the item.
     *
     *
     * The [PremiumBillingCallbacks] set in the constructor will receive callbacks about success or failure.
     *
     *
     * *It is expected that you already retrieved products via [.getProductsAsync]. If you have not yet, then the internal service
     * will not yet be connected and will throw an exception.*
     *
     * @param product Must be a [GooglePlayProduct] but leaving generic for perhaps a generic purchase api in the future.
     */
    fun startPurchase(product: PremiumProduct) {
        if (client.connectionState == BillingClient.ConnectionState.DISCONNECTED) {
            return
        }
        if (product !is GooglePlayProduct) {
            throw RuntimeException("Wrong product type $product")
        }
        try {
            val response = client.launchBillingFlow(
                activity,
                BillingFlowParams.newBuilder().setSkuDetails(
                    product.skuDetails
                ).build()
            )
            if (response.responseCode == BillingClient.BillingResponseCode.OK) {
                pendingProduct = product
            } else {
                callbacks.onProductPurchaseFailed(createError(response.responseCode, null))
            }
        } catch (e: Exception) {
            App.from(activity).errorReporter().reportError(e)
            callbacks.onProductPurchaseFailed(
                createError(BillingClient.BillingResponseCode.ERROR, ErrorReport(e, null)))
        }
    }

    @Suppress("LoopWithTooManyJumpStatements")
    private fun updatePurchases(billingResult: BillingResult, purchases: List<Purchase>?) {
        when {
            client.connectionState == BillingClient.ConnectionState.DISCONNECTED -> return
            billingResult.responseCode != BillingClient.BillingResponseCode.OK -> {
                callbacks.onProductPurchaseFailed(createError(billingResult.responseCode, null))
            }
            purchases == null -> {
                callbacks.onProductPurchaseFailed(
                    createError(BillingClient.BillingResponseCode.ERROR, null))
            }
            else -> {
                for (purchase in purchases) {
                    if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
                    if (purchase.isAcknowledged) continue
                    val product = pendingProduct ?: continue

                    callbacks.onProductPurchaseActivationStarted()
                    client.acknowledgePurchase(purchase.purchaseToken) {
                        activatePurchase(
                            product,
                            purchase.originalJson,
                            SendType.NEW_PURCHASE
                        )
                    }
                }
            }
        }
    }

    fun activatePurchase(product: GooglePlayProduct, purchaseData: String?, type: SendType) {
        callbacks.onProductPurchaseActivationStarted()
        val pocket = App.from(activity).pocket()
        pocket.syncRemote( // Also make sure we sync down the latest account info so we are set to the correct premium settings
            AccountUtil.getuser(pocket.spec()),  // Send a purchase action
            pocket.spec().actions().purchase()
                .source("googleplay")
                .product_id(product.sku)
                .amount(product.priceMicros)
                .amount_display(product.price)
                .currency(product.priceCurrencyCode)
                .transaction_info(purchaseData)
                .transaction_type(if (type == SendType.NEW_PURCHASE) "purchase" else "restore")
                .time(Timestamp.now())
                .build())
            .onSuccess {
                callbacks.onProductPurchaseSuccess()
            }
            .onFailure { e: SyncException? ->
                callbacks.onProductPurchaseActivationFailed(ErrorReport(e, e?.userFacingMessage))
            }
    }

    private fun BillingClient.acknowledgePurchase(
        purchaseToken: String,
        onSuccess: () -> Unit
    ) {
        acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build(),
        ) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onSuccess()
            }
        }
    }

    companion object {
        private const val STATE_PENDING_PURCHASE = "pendingPurchase"
    }
}