package com.dopalette.app.data

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Google Play Billing bridge for DoPalette Premium.
 *
 * Product to create in Play Console after uploading an AAB/APK with BILLING permission:
 *   dopalette_premium_lifetime
 *
 * This manager intentionally keeps the UI simple:
 * - guest users cannot purchase;
 * - signed-in users launch the Google Play purchase sheet;
 * - owned purchases are restored and written to local profile + Firebase;
 * - Premium removes ads, opens Specials forever, gives Rainbow Border, and Premium Artist.
 */
object BillingManager : PurchasesUpdatedListener {
    const val PREMIUM_PRODUCT_ID = "dopalette_premium_lifetime"

    val billingTick = mutableIntStateOf(0)
    val billingReady = mutableStateOf(false)
    val loadingProduct = mutableStateOf(false)
    val lastMessage = mutableStateOf("")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var billingClient: BillingClient? = null
    private var premiumDetails: ProductDetails? = null
    private var pendingAccount: GoogleAuthController.Account? = null
    private var pendingActivatedCallback: (() -> Unit)? = null
    private var pendingErrorCallback: ((String) -> Unit)? = null

    fun initialize(context: Context) {
        if (billingClient != null) return
        billingClient = BillingClient.newBuilder(context.applicationContext)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
        startConnection(context.applicationContext)
    }

    fun launchPremiumPurchase(
        activity: Activity,
        account: GoogleAuthController.Account,
        onActivated: () -> Unit,
        onError: (String) -> Unit
    ) {
        initialize(activity.applicationContext)
        pendingAccount = account
        pendingActivatedCallback = onActivated
        pendingErrorCallback = onError

        val client = billingClient
        if (client == null) {
            onError("Google Play Billing is not ready yet. Please try again.")
            return
        }

        fun launch(details: ProductDetails) {
            val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build()
            val result = client.launchBillingFlow(activity, params)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                onError(result.debugMessage.ifBlank { "Could not open Google Play purchase." })
            }
        }

        premiumDetails?.let { launch(it); return }

        loadingProduct.value = true
        loadPremiumProduct(activity.applicationContext) { details ->
            loadingProduct.value = false
            if (details == null) {
                onError("Premium is not ready in Google Play yet. Upload the Billing build, then create the product in Play Console.")
            } else {
                launch(details)
            }
        }
    }

    fun restoreOwnedPremium(
        context: Context,
        account: GoogleAuthController.Account?,
        onRestored: (() -> Unit)? = null
    ) {
        initialize(context.applicationContext)
        val currentAccount = account ?: GoogleAuthController.refreshCurrentAccount(context)
        val client = billingClient ?: return
        if (!client.isReady) {
            startConnection(context.applicationContext) {
                restoreOwnedPremium(context.applicationContext, currentAccount, onRestored)
            }
            return
        }
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val premiumPurchase = purchases.firstOrNull { it.products.contains(PREMIUM_PRODUCT_ID) && it.purchaseState == Purchase.PurchaseState.PURCHASED }
                if (premiumPurchase != null && currentAccount != null) {
                    activatePremiumFromPurchase(currentAccount, premiumPurchase, onRestored)
                }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val account = pendingAccount
                val premiumPurchase = purchases.orEmpty().firstOrNull {
                    it.products.contains(PREMIUM_PRODUCT_ID) && it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                if (account != null && premiumPurchase != null) {
                    activatePremiumFromPurchase(account, premiumPurchase, pendingActivatedCallback)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                lastMessage.value = "Purchase cancelled."
                pendingErrorCallback?.invoke("Purchase cancelled.")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                val account = pendingAccount
                if (account != null) {
                    restoreOwnedPremiumContext(account)
                } else {
                    pendingErrorCallback?.invoke("Premium is already owned. Sign in to restore it.")
                }
            }
            else -> {
                val message = result.debugMessage.ifBlank { "Purchase did not finish. Please try again." }
                lastMessage.value = message
                pendingErrorCallback?.invoke(message)
            }
        }
        billingTick.intValue += 1
    }

    private fun activatePremiumFromPurchase(
        account: GoogleAuthController.Account,
        purchase: Purchase,
        onActivated: (() -> Unit)?
    ) {
        val client = billingClient
        fun finishActivation() {
            scope.launch {
                MonetizationStore.activatePremiumForAccount(account)
                AccountProfileSync.syncLightweightProfile(account)
                onActivated?.invoke()
                lastMessage.value = "DoPalette Premium unlocked."
                billingTick.intValue += 1
            }
        }

        if (client != null && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.acknowledgePurchase(params) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    finishActivation()
                } else {
                    // Keep the user Premium locally even if acknowledgment is delayed; restore will retry later.
                    finishActivation()
                }
            }
        } else {
            finishActivation()
        }
    }

    private fun restoreOwnedPremiumContext(account: GoogleAuthController.Account) {
        val client = billingClient ?: return
        if (!client.isReady) return
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val premiumPurchase = purchases.firstOrNull { it.products.contains(PREMIUM_PRODUCT_ID) && it.purchaseState == Purchase.PurchaseState.PURCHASED }
                if (premiumPurchase != null) {
                    activatePremiumFromPurchase(account, premiumPurchase, pendingActivatedCallback)
                }
            }
        }
    }

    private fun startConnection(context: Context, onReady: (() -> Unit)? = null) {
        val client = billingClient ?: return
        if (client.isReady) {
            billingReady.value = true
            onReady?.invoke()
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                billingReady.value = result.responseCode == BillingClient.BillingResponseCode.OK
                billingTick.intValue += 1
                if (billingReady.value) {
                    loadPremiumProduct(context.applicationContext)
                    onReady?.invoke()
                } else {
                    lastMessage.value = result.debugMessage
                }
            }

            override fun onBillingServiceDisconnected() {
                billingReady.value = false
                billingTick.intValue += 1
            }
        })
    }

    private fun loadPremiumProduct(context: Context, onLoaded: ((ProductDetails?) -> Unit)? = null) {
        initialize(context.applicationContext)
        val client = billingClient ?: run { onLoaded?.invoke(null); return }
        if (!client.isReady) {
            startConnection(context.applicationContext) {
                loadPremiumProduct(context.applicationContext, onLoaded)
            }
            return
        }
        loadingProduct.value = true
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PREMIUM_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()
        client.queryProductDetailsAsync(params) { result, details ->
            loadingProduct.value = false
            premiumDetails = if (result.responseCode == BillingClient.BillingResponseCode.OK) details.firstOrNull() else null
            billingTick.intValue += 1
            onLoaded?.invoke(premiumDetails)
        }
    }
}
