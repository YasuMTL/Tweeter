package com.yasunari_k.saezuri.di

import android.content.Context
import com.yasunari_k.saezuri.Tweet
import com.yasunari_k.saezuri.ui.ReceiveTokenFragment
import com.yasunari_k.saezuri.ui.TweetFragment
import com.yasunari_k.saezuri.ui.TweetViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [DataStoreModule::class, TweetViewModelModule::class])
interface AppComponent {
    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }

    fun inject(activity: Tweet)
    fun inject(fragment: ReceiveTokenFragment)
    fun inject(fragment: TweetFragment)
    fun inject(tweetViewModel: TweetViewModel)
}