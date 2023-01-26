package com.yasunari_k.saezuri

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus

class SetAdView(var mAdView: AdView?, var mContext: Context) {
    fun initAd() {
        MobileAds.initialize(mContext) { initializationStatus: InitializationStatus? -> }
        requireNotNull(mAdView) { "Can't initialize with null AdView." }

        //mAdView = findViewById(R.id.adView);
        val adRequest = AdRequest.Builder().build()
        mAdView!!.loadAd(adRequest)
    }
}