package com.yasunari_k.saezuri

import android.app.Application
import com.yasunari_k.saezuri.di.AppComponent
import com.yasunari_k.saezuri.di.DaggerAppComponent

open class MyApplication : Application() {
    val appComponent : AppComponent by lazy {
        DaggerAppComponent.factory().create(applicationContext)
    }
}