package com.nandohypesoft.vibecast.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {
    private var interstitialAd: InterstitialAd? = null
    var isAdReady by mutableStateOf(false)
        private set

    private const val TAG = "AdManager"
    private var isLoading = false
    
    // IDs Reais do VibeCast
    private const val INTERSTITIAL_ID = "ca-app-pub-7917358217182228/6519495287"
    const val BANNER_ID = "ca-app-pub-7917358217182228/8793603169"

    fun loadInterstitial(context: Context) {
        if (isLoading || (interstitialAd != null)) {
            if (interstitialAd != null) isAdReady = true
            return
        }
        
        Log.d(TAG, "Loading new interstitial ad...")
        isLoading = true
        isAdReady = false
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, INTERSTITIAL_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, "Ad failed to load: ${adError.message}")
                interstitialAd = null
                isAdReady = false
                isLoading = false
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d(TAG, "Ad was loaded successfully.")
                interstitialAd = ad
                isAdReady = true
                isLoading = false
            }
        })
    }

    fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad was dismissed.")
                    interstitialAd = null
                    isAdReady = false
                    loadInterstitial(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "Ad failed to show: ${adError.message}")
                    interstitialAd = null
                    isAdReady = false
                    loadInterstitial(activity)
                    onAdDismissed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            Log.d(TAG, "The interstitial ad wasn't ready yet.")
            isAdReady = false
            loadInterstitial(activity)
            onAdDismissed()
        }
    }
}
