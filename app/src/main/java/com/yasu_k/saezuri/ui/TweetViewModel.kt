package com.yasu_k.saezuri.ui

import android.content.Context
import androidx.lifecycle.*
import com.yasu_k.saezuri.data.source.ReceiveTokenRepository
import com.yasu_k.saezuri.data.source.TweetRepository
import com.yasu_k.saezuri.data.source.TwitterRepository
import kotlinx.coroutines.launch
import twitter4j.conf.ConfigurationBuilder

class TweetViewModel(
    tweetRepository: TweetRepository,
    receiveTokenRepository: ReceiveTokenRepository
) : ViewModel() {

    private val _configurationBuilder = MutableLiveData<ConfigurationBuilder>()
    val configurationBuilder: LiveData<ConfigurationBuilder> = _configurationBuilder

    //TODO: Use Dependency Injection later
    private val twitterRepository = TwitterRepository(tweetRepository, receiveTokenRepository)

    fun login(context: Context, scope: LifecycleCoroutineScope) {
        twitterRepository.login(context, scope)
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

class TweetViewModelFactory(
    private val tweetRepository: TweetRepository,
    private val receiveTokenRepository: ReceiveTokenRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(TweetViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return TweetViewModel(tweetRepository = tweetRepository, receiveTokenRepository = receiveTokenRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}