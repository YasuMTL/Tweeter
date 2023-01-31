package com.yasunari_k.saezuri.di

import android.content.Context
import com.yasunari_k.saezuri.Tweet
import com.yasunari_k.saezuri.ui.ReceiveTokenFragment
import com.yasunari_k.saezuri.ui.TweetFragment
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component
interface AppComponent {
    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }

    fun inject(activity: Tweet)
    fun inject(fragment: ReceiveTokenFragment)
    fun inject(fragment: TweetFragment)
}