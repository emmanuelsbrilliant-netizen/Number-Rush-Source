package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions

object UnityAdsManager {
    private const val TAG = "UnityAdsManager"
    const val UNITY_GAME_ID = "500357161"
    const val REWARDED_PLACEMENT_ID = "Rewarded_iOS"
    const val INTERSTITIAL_PLACEMENT_ID = "Interstitial_iOS"
    const val TEST_MODE = false

    private var isInitialized = false

    fun initialize(context: Context, onInitComplete: (() -> Unit)? = null) {
        if (UnityAds.isInitialized) {
            isInitialized = true
            loadRewardedAd()
            loadInterstitialAd()
            onInitComplete?.invoke()
            return
        }

        UnityAds.initialize(
            context.applicationContext,
            UNITY_GAME_ID,
            TEST_MODE,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    Log.d(TAG, "Unity Ads Initialization Complete")
                    isInitialized = true
                    loadRewardedAd()
                    loadInterstitialAd()
                    onInitComplete?.invoke()
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError?,
                    message: String?
                ) {
                    Log.e(TAG, "Unity Ads Initialization Failed: $error - $message")
                    isInitialized = false
                }
            }
        )
    }

    fun loadRewardedAd(onLoaded: (() -> Unit)? = null) {
        UnityAds.load(REWARDED_PLACEMENT_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                Log.d(TAG, "Rewarded Ad Loaded: $placementId")
                onLoaded?.invoke()
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                Log.e(TAG, "Rewarded Ad Failed to Load: $placementId - $error - $message")
            }
        })
    }

    fun loadInterstitialAd(onLoaded: (() -> Unit)? = null) {
        UnityAds.load(INTERSTITIAL_PLACEMENT_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                Log.d(TAG, "Interstitial Ad Loaded: $placementId")
                onLoaded?.invoke()
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                Log.e(TAG, "Interstitial Ad Failed to Load: $placementId - $error - $message")
            }
        })
    }

    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdClosedOrSkipped: () -> Unit = {},
        onAdFailed: (String) -> Unit = {}
    ) {
        UnityAds.show(
            activity,
            REWARDED_PLACEMENT_ID,
            UnityAdsShowOptions(),
            object : IUnityAdsShowListener {
                override fun onUnityAdsShowFailure(
                    placementId: String,
                    error: UnityAds.UnityAdsShowError,
                    message: String
                ) {
                    Log.e(TAG, "Rewarded Show Failed: $placementId - $error - $message")
                    loadRewardedAd()
                    onAdFailed(message)
                }

                override fun onUnityAdsShowStart(placementId: String) {
                    Log.d(TAG, "Rewarded Show Started: $placementId")
                }

                override fun onUnityAdsShowClick(placementId: String) {
                    Log.d(TAG, "Rewarded Show Clicked: $placementId")
                }

                override fun onUnityAdsShowComplete(
                    placementId: String,
                    state: UnityAds.UnityAdsShowCompletionState
                ) {
                    Log.d(TAG, "Rewarded Show Complete: $placementId - $state")
                    loadRewardedAd()
                    if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                        onRewardEarned()
                    } else {
                        onAdClosedOrSkipped()
                    }
                }
            }
        )
    }

    fun showInterstitialAd(
        activity: Activity,
        onCompleteOrDismissed: () -> Unit
    ) {
        UnityAds.show(
            activity,
            INTERSTITIAL_PLACEMENT_ID,
            UnityAdsShowOptions(),
            object : IUnityAdsShowListener {
                override fun onUnityAdsShowFailure(
                    placementId: String,
                    error: UnityAds.UnityAdsShowError,
                    message: String
                ) {
                    Log.e(TAG, "Interstitial Show Failed: $placementId - $error - $message")
                    loadInterstitialAd()
                    onCompleteOrDismissed()
                }

                override fun onUnityAdsShowStart(placementId: String) {
                    Log.d(TAG, "Interstitial Show Started: $placementId")
                }

                override fun onUnityAdsShowClick(placementId: String) {
                    Log.d(TAG, "Interstitial Show Clicked: $placementId")
                }

                override fun onUnityAdsShowComplete(
                    placementId: String,
                    state: UnityAds.UnityAdsShowCompletionState
                ) {
                    Log.d(TAG, "Interstitial Show Complete: $placementId - $state")
                    loadInterstitialAd()
                    onCompleteOrDismissed()
                }
            }
        )
    }
}
