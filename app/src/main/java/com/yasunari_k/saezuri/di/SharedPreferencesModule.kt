package com.yasunari_k.saezuri.di

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Inject

@Module
class SharedPreferencesModule {
    @Provides
    fun provideSharedPreferences(context: Context) : SharedPreferences {
        return context.getSharedPreferences("twitterToken", AppCompatActivity.MODE_PRIVATE)
    }
}