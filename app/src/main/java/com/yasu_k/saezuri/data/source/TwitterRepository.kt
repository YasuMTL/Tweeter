package com.yasu_k.saezuri.data.source

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import twitter4j.conf.ConfigurationBuilder

class TwitterRepository(
    private val tweetRepository: TweetRepository,
    private val receiveTokenRepository: ReceiveTokenRepository
) {
    fun login(context: Context, scope: LifecycleCoroutineScope){
        //receiveTokenRepository.login()
        receiveTokenRepository.getRequestToken(context, scope)
    }

    fun logout(){
        receiveTokenRepository.logout()
    }

    suspend fun sendTweet(textTweet: String, configurationBuilder: ConfigurationBuilder){
        tweetRepository.sendTweet(textTweet, configurationBuilder)
    }
}