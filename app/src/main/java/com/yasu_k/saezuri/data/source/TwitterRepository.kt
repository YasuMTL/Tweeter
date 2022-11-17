package com.yasu_k.saezuri.data.source

import twitter4j.conf.ConfigurationBuilder

class TwitterRepository(
    private val tweetRepository: TweetRepository,
    private val receiveTokenRepository: ReceiveTokenRepository
) {
    fun login(){
        receiveTokenRepository.login()
    }

    fun logout(){
        receiveTokenRepository.logout()
    }

    suspend fun sendTweet(textTweet: String, configurationBuilder: ConfigurationBuilder){
        tweetRepository.sendTweet(textTweet, configurationBuilder)
    }
}