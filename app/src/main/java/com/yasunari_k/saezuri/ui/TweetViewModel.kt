package com.yasunari_k.saezuri.ui

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yasunari_k.saezuri.data.SettingDataStore
import com.yasunari_k.saezuri.data.source.SendTweetResult
import com.yasunari_k.saezuri.data.source.TwitterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import twitter4j.TwitterException
import javax.inject.Inject

data class LoginUiState(
    val isLoggedIn: Boolean = false,
    val token: String = "",
    val tokenSecret: String = ""
)

const val TAG = "TweetViewModel"

class TweetViewModel @Inject constructor(
    private val twitterRepository: TwitterRepository,
    private val dataStore: SettingDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    companion object {
//        val Factory: ViewModelProvider.Factory =
        fun provideViewModelFactory(
            twitterRepository: TwitterRepository,
            dataStore: SettingDataStore
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
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
        }
    }

    init {
        viewModelScope.launch {
            //Read login state from Data Store
            val testIsLoggedIn = dataStore.preferenceIsLoggedInFlow.first()
            Log.i(TAG, "testIsLoggedIn=$testIsLoggedIn")
            if (testIsLoggedIn) {
                _uiState.update {
                    it.copy(
                        isLoggedIn = testIsLoggedIn,
                        token = dataStore.preferenceTokenFlow.first(),
                        tokenSecret = dataStore.preferenceTokenSecretFlow.first()
                    )
                }
                Log.i(TAG, "Update completed. uiState.value: ${uiState.value}")
            }

            val accessTokenState = twitterRepository.getTokenState()
            accessTokenState.collect { tokenState ->
                val token = tokenState.accessToken?.token ?: ""
                val tokenSecret = tokenState.accessToken?.tokenSecret ?: ""

                if(token.isNotBlank()) {
                    _uiState.update {
                        it.copy(
                            isLoggedIn = true,
                            token = token,
                            tokenSecret = tokenSecret
                        )
                    }
                    Log.i(TAG, "accTokenState: _uiState is updated")
                }

                Log.i(TAG, "token=$token")
            }
        }
    }

    fun login(context: Context, scope: LifecycleCoroutineScope) {
        Log.i(TAG, "login...")
        twitterRepository.login(context, scope)
    }

    fun logout() {
        resetUiState()
        Log.i(TAG, "logout...")
        twitterRepository.logout()
    }

    fun resetUiState(){
        println("resetUiState is called")
        _uiState.update {
            it.copy(
                isLoggedIn = false,
                token = "",
                tokenSecret = "")
        }
    }

    fun isLoggedIn(): Boolean {
        return uiState.value.isLoggedIn
    }

    suspend fun sendTweet(textTweet: String, contentResolver: ContentResolver) = withContext(Dispatchers.IO) {
        return@withContext twitterRepository.sendTweet(
            textTweet,
            uiState.value.token,
            uiState.value.tokenSecret,
            contentResolver
        )
    }

    suspend fun sendTweetWithChosenUri(textTweet: String, contentResolver: ContentResolver, chosenURIs: MutableList<Uri>): SendTweetResult = withContext(Dispatchers.IO){
        return@withContext twitterRepository.sendTweetWithChosenUri(textTweet, uiState.value.token, uiState.value.tokenSecret, contentResolver, chosenURIs)
    }

    fun saveLoginStateToPreferenceStore(loginState: Boolean, context: Context) {
        viewModelScope.launch {
            twitterRepository.saveLoginStateToPreferencesStore(loginState, context)
            Log.i(TAG, "Store login state to preference")
        }
    }

    fun saveTokenInfoToPreferenceStore(token: String, context: Context) {
        viewModelScope.launch {
            twitterRepository.saveTokenToPreferencesStore(token, context)
            Log.i(TAG, "Store token to preference")
        }
    }

    fun saveTokenSecretInfoToPreferenceStore(tokenSecret: String, context: Context) {
        viewModelScope.launch {
            twitterRepository.saveTokenSecretToPreferencesStore(tokenSecret, context)
            Log.i(TAG, "Store token secret to preference")
        }
    }

    fun clearTokenInfoFromPreferenceStore(context: Context) {
        viewModelScope.launch {
            twitterRepository.apply {
                removeLoginStateFromPreferencesStore(context)
                removeTokenFromPreferencesStore(context)
                removeTokenSecretFromPreferencesStore(context)
            }
            Log.i(TAG, "Clear out store data from preference")
        }
    }

    fun takeOnePhoto(context: Context, launcher: ActivityResultLauncher<Uri>){ twitterRepository.takeOnePhoto(context, launcher) }
    fun takeOneVideo(context: Context, launcher: ActivityResultLauncher<Uri>){ twitterRepository.takeOneVideo(context, launcher) }
    fun clearUploadedMediaFiles() { twitterRepository.clearUploadedMediaFiles() }
    fun getStoredTwitterException(): TwitterException? {
        return twitterRepository.getStoredTwitterException()
    }
}