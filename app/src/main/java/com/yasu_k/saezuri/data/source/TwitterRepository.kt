package com.yasu_k.saezuri.data.source

class TwitterRepository(
    private val tweetRepository: TweetRepository,
    private val receiveTokenRepository: ReceiveTokenRepository
) {
    fun login(){
        receiveTokenRepository.login()
    }

    fun sendTweet(){
        tweetRepository.sendTweet()
    }
}