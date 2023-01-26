package com.yasunari_k.saezuri.di

import android.content.Context
import com.yasunari_k.saezuri.Tweet
import dagger.BindsInstance
import dagger.Component

@Component(modules = [SharedPreferencesModule::class])
interface AppComponent {
    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }

    fun inject(activity: Tweet)
}