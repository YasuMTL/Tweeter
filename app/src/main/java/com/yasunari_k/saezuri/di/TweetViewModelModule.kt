package com.yasunari_k.saezuri.di

import androidx.lifecycle.ViewModelProvider
import com.yasunari_k.saezuri.data.SettingDataStore
import com.yasunari_k.saezuri.data.source.TwitterRepository
import com.yasunari_k.saezuri.ui.TweetViewModel
import dagger.Module
import dagger.Provides

@Module
class TweetViewModelModule {
    @Provides
    fun provideViewModelFactory(
        twitterRepository: TwitterRepository,
        dataStore: SettingDataStore
    ): ViewModelProvider.Factory {
        return TweetViewModel.provideViewModelFactory(
            twitterRepository = twitterRepository,
            dataStore = dataStore
        )
    /*object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if(modelClass.isAssignableFrom(TweetViewModel::class.java)){
                    @Suppress("UNCHECKED_CAST")
                    return TweetViewModel(
                        twitterRepository = twitterRepository,
                        dataStore = dataStore
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }*/
    }
}