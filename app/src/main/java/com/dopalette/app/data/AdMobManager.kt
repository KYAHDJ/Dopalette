package com.dopalette.app.data

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableIntStateOf
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdMobManager {
    // DoPalette production AdMob ad unit IDs.
    const val BANNER_AD_UNIT_ID = "ca-app-pub-9372606273046322/6214686999"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-9372606273046322/1128478909"
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-9372606273046322/5917771159"

    val adTick = mutableIntStateOf(0)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var initialized = false
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var loadingInterstitial = false
    private var loadingRewarded = false
    private var lastInterstitialLoadAttemptMs = 0L
    private var lastRewardedLoadAttemptMs = 0L

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        runCatching {
            MobileAds.initialize(context.applicationContext) {}
            preloadInterstitial(context, force = true)
            preloadRewarded(context, force = true)
        }
    }

    fun isInterstitialReady(): Boolean = interstitialAd != null && MonetizationStore.adsEnabled()
    fun isRewardedReady(): Boolean = rewardedAd != null && MonetizationStore.adsEnabled()
    fun isRewardedLoading(): Boolean = loadingRewarded && MonetizationStore.adsEnabled()

    fun preloadInterstitial(context: Context, force: Boolean = false) {
        if (!MonetizationStore.adsEnabled()) {
            interstitialAd = null
            loadingInterstitial = false
            adTick.intValue += 1
            return
        }
        if (loadingInterstitial || interstitialAd != null) return
        val now = System.currentTimeMillis()
        if (!force && now - lastInterstitialLoadAttemptMs < 8_000L) return
        lastInterstitialLoadAttemptMs = now
        loadingInterstitial = true
        InterstitialAd.load(
            context.applicationContext,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    loadingInterstitial = false
                    adTick.intValue += 1
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    loadingInterstitial = false
                    adTick.intValue += 1
                    mainHandler.postDelayed({ preloadInterstitial(context.applicationContext, force = true) }, 15_000L)
                }
            }
        )
    }

    /**
     * Called every time the Done result appears. It always attempts this placement:
     * - uses a preloaded ad if ready;
     * - otherwise loads a fresh interstitial immediately and shows it as soon as it loads;
     * - never blocks the user's saved artwork if AdMob has no fill or loading fails.
     */
    fun showInterstitialAfterDone(activity: Activity, onFinished: () -> Unit) {
        if (!MonetizationStore.adsEnabled()) {
            onFinished()
            return
        }
        val readyAd = interstitialAd
        if (readyAd != null) {
            showInterstitialInternal(activity, readyAd, onFinished)
            return
        }

        if (loadingInterstitial) {
            var completed = false
            val timeout = Runnable {
                if (!completed) {
                    completed = true
                    onFinished()
                }
            }
            mainHandler.postDelayed(timeout, 4_000L)
            fun waitForLoaded(attempt: Int) {
                if (completed) return
                val ad = interstitialAd
                if (ad != null) {
                    completed = true
                    mainHandler.removeCallbacks(timeout)
                    showInterstitialInternal(activity, ad, onFinished)
                } else if (attempt < 20) {
                    mainHandler.postDelayed({ waitForLoaded(attempt + 1) }, 150L)
                }
            }
            waitForLoaded(0)
            return
        }

        loadingInterstitial = true
        lastInterstitialLoadAttemptMs = System.currentTimeMillis()
        var callbackCompleted = false
        val finishIfSlow = Runnable {
            if (!callbackCompleted) {
                callbackCompleted = true
                loadingInterstitial = false
                onFinished()
            }
        }
        mainHandler.postDelayed(finishIfSlow, 4_500L)
        InterstitialAd.load(
            activity.applicationContext,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    if (callbackCompleted) {
                        interstitialAd = ad
                        loadingInterstitial = false
                        adTick.intValue += 1
                        return
                    }
                    callbackCompleted = true
                    mainHandler.removeCallbacks(finishIfSlow)
                    loadingInterstitial = false
                    interstitialAd = ad
                    adTick.intValue += 1
                    showInterstitialInternal(activity, ad, onFinished)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (callbackCompleted) return
                    callbackCompleted = true
                    mainHandler.removeCallbacks(finishIfSlow)
                    interstitialAd = null
                    loadingInterstitial = false
                    adTick.intValue += 1
                    mainHandler.postDelayed({ preloadInterstitial(activity.applicationContext, force = true) }, 15_000L)
                    onFinished()
                }
            }
        )
    }

    private fun showInterstitialInternal(activity: Activity, ad: InterstitialAd, onFinished: () -> Unit) {
        interstitialAd = null
        adTick.intValue += 1
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preloadInterstitial(activity.applicationContext, force = true)
                onFinished()
            }
            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                preloadInterstitial(activity.applicationContext, force = true)
                onFinished()
            }
            override fun onAdShowedFullScreenContent() {
                interstitialAd = null
            }
        }
        ad.show(activity)
    }

    fun preloadRewarded(context: Context, force: Boolean = false) {
        if (!MonetizationStore.adsEnabled()) {
            rewardedAd = null
            loadingRewarded = false
            adTick.intValue += 1
            return
        }
        if (loadingRewarded || rewardedAd != null) return
        val now = System.currentTimeMillis()
        if (!force && now - lastRewardedLoadAttemptMs < 8_000L) return
        lastRewardedLoadAttemptMs = now
        loadingRewarded = true
        adTick.intValue += 1
        RewardedAd.load(
            context.applicationContext,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    loadingRewarded = false
                    adTick.intValue += 1
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    loadingRewarded = false
                    adTick.intValue += 1
                    mainHandler.postDelayed({ preloadRewarded(context.applicationContext, force = true) }, 15_000L)
                }
            }
        )
    }

    fun showRewardedForSpecial(activity: Activity, onRewardEarned: () -> Unit, onClosedWithoutReward: () -> Unit) {
        if (!MonetizationStore.adsEnabled()) {
            onRewardEarned()
            return
        }
        val ad = rewardedAd
        if (ad == null) {
            preloadRewarded(activity.applicationContext, force = true)
            onClosedWithoutReward()
            return
        }
        rewardedAd = null
        adTick.intValue += 1
        var rewarded = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preloadRewarded(activity.applicationContext, force = true)
                if (!rewarded) onClosedWithoutReward()
            }
            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                preloadRewarded(activity.applicationContext, force = true)
                onClosedWithoutReward()
            }
            override fun onAdShowedFullScreenContent() {
                rewardedAd = null
            }
        }
        ad.show(activity) {
            rewarded = true
            onRewardEarned()
        }
    }
}
