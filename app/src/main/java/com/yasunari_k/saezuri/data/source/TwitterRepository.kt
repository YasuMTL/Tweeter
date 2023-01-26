package com.yasunari_k.saezuri.data.source

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleCoroutineScope
import com.yasunari_k.saezuri.LoginInfo
import com.yasunari_k.saezuri.data.SettingDataStore
import twitter4j.TwitterException
import twitter4j.conf.ConfigurationBuilder

class TwitterRepository(
    private val tweetRepository: TweetRepository,
    private val receiveTokenRepository: ReceiveTokenRepository,
    private val dataStore: SettingDataStore
) {
    fun login(context: Context, scope: LifecycleCoroutineScope) {
        receiveTokenRepository.getRequestToken(context, scope)
    }

    private fun getConfigurationBuilder(token: String, tokenSecret: String): ConfigurationBuilder {
        return ConfigurationBuilder()
            .setDebugEnabled(true)
            .setOAuthConsumerKey(LoginInfo.oAuthConsumerKey)
            .setOAuthConsumerSecret(LoginInfo.oAuthConsumerSecret)
            .setOAuthAccessToken(token)
            .setOAuthAccessTokenSecret(tokenSecret)
    }

    fun logout(){ receiveTokenRepository.logout() }

    suspend fun sendTweet(textTweet: String, token: String, tokenSecret: String, contentResolver: ContentResolver): Int {
        return tweetRepository.sendTweet(textTweet, getConfigurationBuilder(token, tokenSecret), contentResolver)
    }

    suspend fun sendTweetWithChosenUri(textTweet: String, token: String, tokenSecret: String, contentResolver: ContentResolver, chosenURIs: MutableList<Uri>): Int {
        tweetRepository.setUri(chosenURIs)
        return tweetRepository.sendTweet(textTweet, getConfigurationBuilder(token, tokenSecret), contentResolver)
    }

    suspend fun saveLoginStateToPreferencesStore(loginState: Boolean, context: Context) {
        dataStore.saveLoginInfoToPreferencesStore(loginState, context)
    }

    suspend fun saveTokenToPreferencesStore(twitterToken: String, context: Context) {
        dataStore.saveTokenToPreferencesStore(twitterToken, context)
    }

    suspend fun saveTokenSecretToPreferencesStore(twitterTokenSecret: String, context: Context) {
        dataStore.saveTokenSecretToPreferencesStore(twitterTokenSecret, context)
    }

    suspend fun removeLoginStateFromPreferencesStore(context: Context) {
        dataStore.removeLoginStateFromPreferencesStore(context)
    }

    suspend fun removeTokenFromPreferencesStore(context: Context) {
        dataStore.removeTokenFromPreferencesStore(context)
    }

    suspend fun removeTokenSecretFromPreferencesStore(context: Context) {
        dataStore.removeTokenSecretFromPreferencesStore(context)
    }

    fun takeOnePhoto(context: Context, launcher: ActivityResultLauncher<Uri>){ tweetRepository.takeOnePhoto(context, launcher) }
    fun takeOneVideo(context: Context, launcher: ActivityResultLauncher<Uri>){ tweetRepository.takeOneVideo(context, launcher) }
    fun clearUploadedMediaFiles() { tweetRepository.flushOutUploadedImageVideo() }
    fun getStoredTwitterException(): TwitterException? = tweetRepository.getStoredTwitterException()
}