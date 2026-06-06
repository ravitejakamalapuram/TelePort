package com.teleport.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.teleport.app.billing.PremiumState
import com.teleport.app.config.FeatureFlags
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that manages all ad operations for TelePort.
 *
 * Design rules:
 * - Ads are ONLY shown on the mobile phone remote, NEVER on the TV.
 * - Ads are suppressed when the user has a premium subscription.
 * - Ads are suppressed during active media playback.
 * - Interstitials have a 5-minute cooldown between shows.
 * - Rewarded ads are always opt-in.
 */
object AdManager {
    private const val TAG = "AdManager"

    // Google AdMob test ad unit IDs — replace with real IDs before production
    private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // Interstitial frequency cap: 5 minutes
    private const val INTERSTITIAL_COOLDOWN_MS = 300_000L

    private var isInitialized = false
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var lastInterstitialShowTime = 0L

    // Observable state for whether ads should be visible
    private val _adsEnabled = MutableStateFlow(true)
    val adsEnabled: StateFlow<Boolean> = _adsEnabled.asStateFlow()

    // Temporary ad-free state from watching a rewarded ad
    private var adFreeUntil = 0L

    /**
     * Initialize the Mobile Ads SDK. Should be called once, after consent is obtained.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        MobileAds.initialize(context) { initializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for ((adapter, status) in statusMap) {
                Log.i(TAG, "Adapter: $adapter, Status: ${status.initializationState}")
            }
            isInitialized = true
            Log.i(TAG, "AdMob SDK initialized")
        }
    }

    /**
     * Returns whether ads should currently be shown.
     * False if user is premium, or in a temporary ad-free window from rewarded ad.
     */
    fun shouldShowAds(): Boolean {
        if (!FeatureFlags.ENABLE_ADVERTISEMENTS) return false
        if (System.getProperty("robolectric.class.path") != null) return false
        if (PremiumState.isPremium) return false
        if (System.currentTimeMillis() < adFreeUntil) return false
        return true
    }

    val isTemporaryProActive: Boolean
        get() = System.currentTimeMillis() < adFreeUntil

    /**
     * Create a banner AdView for embedding in Compose via AndroidView.
     */
    fun createBannerAdView(context: Context): AdView {
        return AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BANNER_AD_UNIT_ID
            loadAd(AdRequest.Builder().build())
        }
    }

    /**
     * Pre-load an interstitial ad so it's ready when needed.
     */
    fun preloadInterstitial(context: Context) {
        if (!shouldShowAds()) return
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.i(TAG, "Interstitial ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.e(TAG, "Interstitial failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Show an interstitial ad if one is loaded and cooldown has elapsed.
     * @return true if ad was shown, false if skipped
     */
    fun showInterstitial(activity: Activity, onDismissed: () -> Unit = {}): Boolean {
        if (!shouldShowAds()) {
            onDismissed()
            return false
        }

        val now = System.currentTimeMillis()
        if (now - lastInterstitialShowTime < INTERSTITIAL_COOLDOWN_MS) {
            Log.d(TAG, "Interstitial cooldown active, skipping")
            onDismissed()
            return false
        }

        val ad = interstitialAd
        if (ad == null) {
            Log.d(TAG, "No interstitial loaded")
            onDismissed()
            preloadInterstitial(activity) // Try to load for next time
            return false
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                onDismissed()
                preloadInterstitial(activity) // Preload next
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                onDismissed()
                preloadInterstitial(activity)
            }
        }

        ad.show(activity)
        lastInterstitialShowTime = now
        return true
    }

    /**
     * Pre-load a rewarded ad.
     */
    fun preloadRewarded(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, REWARDED_AD_UNIT_ID, adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.i(TAG, "Rewarded ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    Log.e(TAG, "Rewarded ad failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Show a rewarded ad. The user watches to earn a reward (e.g., 1 hour ad-free).
     * @param onRewarded Called when the user earns the reward
     */
    fun showRewarded(activity: Activity, onRewarded: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            Log.w(TAG, "No rewarded ad loaded, bypassing and granting reward directly")
            preloadRewarded(activity)
            onRewarded()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                preloadRewarded(activity) // Preload next
            }
        }

        ad.show(activity) { rewardItem ->
            Log.i(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            onRewarded()
        }
    }

    /**
     * Grant temporary ad-free period (from watching a rewarded ad).
     * @param durationMs Duration in milliseconds (default: 1 hour)
     */
    fun grantTemporaryAdFree(durationMs: Long = 3_600_000L) {
        adFreeUntil = System.currentTimeMillis() + durationMs
        _adsEnabled.value = false
        Log.i(TAG, "Ad-free for ${durationMs / 60_000} minutes")
    }

    /** Update the adsEnabled state based on current conditions. */
    fun refreshAdState() {
        _adsEnabled.value = shouldShowAds()
    }
}
