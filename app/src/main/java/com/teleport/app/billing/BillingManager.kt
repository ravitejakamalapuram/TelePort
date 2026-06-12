package com.teleport.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages Google Play Billing for TelePort Pro/Pro+ subscriptions and one-time tip purchases.
 */
class BillingManager(
    private val context: Context,
    private val scope: CoroutineScope
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"

        // Subscription product IDs (configure these in Google Play Console)
        const val PRODUCT_PRO_MONTHLY = "teleport_pro_monthly"
        const val PRODUCT_PRO_YEARLY = "teleport_pro_yearly"
        const val PRODUCT_PRO_PLUS_MONTHLY = "teleport_pro_plus_monthly"
        const val PRODUCT_PRO_PLUS_YEARLY = "teleport_pro_plus_yearly"

        // One-time tip product IDs
        const val PRODUCT_TIP_SMALL = "teleport_tip_small"    // $1.99
        const val PRODUCT_TIP_MEDIUM = "teleport_tip_medium"  // $4.99
        const val PRODUCT_TIP_LARGE = "teleport_tip_large"    // $9.99

        private val PRO_PRODUCTS = setOf(PRODUCT_PRO_MONTHLY, PRODUCT_PRO_YEARLY)
        private val PRO_PLUS_PRODUCTS = setOf(PRODUCT_PRO_PLUS_MONTHLY, PRODUCT_PRO_PLUS_YEARLY)
    }

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _subscriptionProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    val subscriptionProducts: StateFlow<List<ProductDetails>> = _subscriptionProducts.asStateFlow()

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    /** Connect to Google Play Billing service. Call in Application.onCreate() or MainActivity. */
    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Billing client connected")
                    _isReady.value = true
                    queryExistingPurchases()
                    queryAvailableProducts()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected, will retry on next operation")
                _isReady.value = false
            }
        })
    }

    /** Query existing purchases to restore premium state on app launch. */
    private fun queryExistingPurchases() {
        scope.launch(Dispatchers.IO) {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    processPurchases(purchases)
                }
            }
        }
    }

    /** Query available subscription products for display on the paywall. */
    private fun queryAvailableProducts() {
        val allProductIds = listOf(
            PRODUCT_PRO_MONTHLY, PRODUCT_PRO_YEARLY,
            PRODUCT_PRO_PLUS_MONTHLY, PRODUCT_PRO_PLUS_YEARLY
        )
        val productList = allProductIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _subscriptionProducts.value = productDetailsList
                Log.i(TAG, "Found ${productDetailsList.size} subscription products")
            }
        }
    }

    /** Launch the subscription purchase flow. */
    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails, offerToken: String) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.let { processPurchases(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "User cancelled purchase")
            }
            else -> {
                Log.e(TAG, "Purchase error: ${billingResult.debugMessage}")
            }
        }
    }

    /** Process and acknowledge purchases, then update PremiumState. */
    private fun processPurchases(purchases: List<Purchase>) {
        var highestTier = PremiumState.Tier.FREE

        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                // Acknowledge if not already
                if (!purchase.isAcknowledged) {
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    scope.launch(Dispatchers.IO) {
                        billingClient.acknowledgePurchase(ackParams) { result ->
                            Log.i(TAG, "Acknowledge result: ${result.responseCode}")
                        }
                    }
                }

                // Determine tier from product IDs
                for (productId in purchase.products) {
                    when {
                        productId in PRO_PLUS_PRODUCTS -> highestTier = PremiumState.Tier.PRO_PLUS
                        productId in PRO_PRODUCTS && highestTier != PremiumState.Tier.PRO_PLUS -> highestTier = PremiumState.Tier.PRO
                    }
                }
            }
        }

        PremiumState.updateTier(highestTier)
        Log.i(TAG, "Premium state updated to: $highestTier")
    }

    /** Disconnect billing client. Call in onDestroy(). */
    fun disconnect() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
