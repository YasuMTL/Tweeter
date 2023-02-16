package com.yasunari_k.saezuri.di

import android.content.Context
import com.yasunari_k.saezuri.data.SettingDataStore
import dagger.Module
import dagger.Provides

@Module
class DataStoreModule {
    @Provides
    fun provideDataStore(context: Context): SettingDataStore {
        return SettingDataStore(context)
    }
}