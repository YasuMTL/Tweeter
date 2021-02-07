package com.yasu_k.saezuri;

import android.content.Context;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class SetAdView {
    AdView mAdView;
    Context mContext;

    public SetAdView(AdView adView, Context context){
        this.mAdView = adView;
        this.mContext = context;
    }

    public void initAd(){
        MobileAds.initialize(mContext, initializationStatus -> {
        });

        if (mAdView == null) {
            throw new IllegalArgumentException(
                    "Can't initialize with null AdView.");
        }

        //mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }
}
