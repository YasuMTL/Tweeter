package com.yasu_k.saezuri

import android.app.Application
import com.yasu_k.saezuri.di.AppComponent
import com.yasu_k.saezuri.di.DaggerAppComponent

open class MyApplication : Application() {
    val appComponent : AppComponent by lazy {
        DaggerAppComponent.factory().create(applicationContext)
    }
}