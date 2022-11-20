package com.yasu_k.saezuri.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yasu_k.saezuri.data.source.ReceiveTokenRepository
import com.yasu_k.saezuri.data.source.TweetRepository
import com.yasu_k.saezuri.data.source.TwitterRepository
import kotlinx.coroutines.launch
import twitter4j.conf.ConfigurationBuilder

class TweetViewModel(
    private val tweetRepository: TweetRepository,
    private val receiveTokenRepository: ReceiveTokenRepository
) : ViewModel() {

    private val _configurationBuilder = MutableLiveData<ConfigurationBuilder>()
    val configurationBuilder: LiveData<ConfigurationBuilder> = _configurationBuilder

    //TODO: Use Dependency Injection later
    private val twitterRepository = TwitterRepository(tweetRepository, receiveTokenRepository)

    fun login() {
        twitterRepository.login()
    }

    fun logout() {
        twitterRepository.logout()
    }

    fun isLoggedIn(): Boolean {
        return configurationBuilder.value != null
    }

    fun sendTweet(textTweet: String, configurationBuilder: ConfigurationBuilder) {
        viewModelScope.launch {
            twitterRepository.sendTweet(textTweet, configurationBuilder)
        }
    }

    fun setConfigurationBuilder(configBuilder: ConfigurationBuilder) {
        _configurationBuilder.value = configBuilder
    }
}