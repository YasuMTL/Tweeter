package com.yasu_k.saezuri.ui

import androidx.lifecycle.ViewModel
import com.yasu_k.saezuri.data.source.ReceiveTokenRepository
import com.yasu_k.saezuri.data.source.TweetRepository
import com.yasu_k.saezuri.data.source.TwitterRepository

class TweetViewModel(
    private val tweetRepository: TweetRepository,
    private val receiveTokenRepository: ReceiveTokenRepository
) : ViewModel() {

    //TODO: Use Dependency Injection later
    private val twitterRepository = TwitterRepository(tweetRepository, receiveTokenRepository)

    fun login() {
        receiveTokenRepository.login()
    }

    fun logout() {
        receiveTokenRepository.logout()
    }

    fun sendTweet() {
        tweetRepository.sendTweet()
    }
}