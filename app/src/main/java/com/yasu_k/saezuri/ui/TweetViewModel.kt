package com.yasu_k.saezuri.ui

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.*
import com.yasu_k.saezuri.data.SettingDataStore
import com.yasu_k.saezuri.data.source.ReceiveTokenRepository
import com.yasu_k.saezuri.data.source.TweetRepository
import com.yasu_k.saezuri.data.source.TwitterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LoginUiState(
    val isLoggedIn: Boolean = false,
    val token: String = "",
    val tokenSecret: String = ""
)

const val TAG = "TweetViewModel"

class TweetViewModel(
    tweetRepository: TweetRepository,
    receiveTokenRepository: ReceiveTokenRepository,
    dataStore: SettingDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    //TODO: Use Dependency Injection later
    private val twitterRepository = TwitterRepository(tweetRepository, receiveTokenRepository, dataStore)

    init {
        viewModelScope.launch {
            //Read login state from Data Store
            val testIsLoggedIn = dataStore.preferenceIsLoggedInFlow.first()
            Log.i(TAG, "testIsLoggedIn=$testIsLoggedIn")
            if (testIsLoggedIn) {
                _uiState.update {
                    it.copy(
                        testIsLoggedIn,
                        dataStore.preferenceTokenFlow.first(),
                        dataStore.preferenceTokenSecretFlow.first()
                    )
                }
                Log.i(TAG, "Update completed. uiState.value: ${uiState.value}")
            }

//            dataStore.preferenceIsLoggedInFlow.collect { storedLoginState ->
//                if (storedLoginState) {
//                    _uiState.update {
//                        it.copy(
//                            isLoggedIn = storedLoginState,
//                            token = dataStore.preferenceTokenFlow.asLiveData().value ?: "",
//                            tokenSecret = dataStore.preferenceTokenSecretFlow.asLiveData().value ?: ""
//                        )
//                    }
//                }
//            }

            receiveTokenRepository.accTokenState.collect { tokenState ->
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
        twitterRepository.logout()
        _uiState.update {
            it.copy(
                isLoggedIn = false,
                token = "",
                tokenSecret = "")
        }
        Log.i(TAG, "logout...")
    }

    fun isLoggedIn(): Boolean {
        return uiState.value.isLoggedIn
    }

//    fun sendTweet(textTweet: String, contentResolver: ContentResolver): Int {
//        viewModelScope.launch(Dispatchers.IO) {
//            return twitterRepository.sendTweet(textTweet, uiState.value.token, uiState.value.tokenSecret, contentResolver)
//            //TODO After sending a tweet successfully, clear out the edit text widget
//            //Log.i(TAG, "The tweet was sent successfully")
//        }
//    }
    suspend fun sendTweet(textTweet: String, contentResolver: ContentResolver): Int = withContext(Dispatchers.IO){
        return@withContext twitterRepository.sendTweet(textTweet, uiState.value.token, uiState.value.tokenSecret, contentResolver)
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
        //if(!uiState.value.isLoggedIn){
            viewModelScope.launch {
                twitterRepository.apply {
                    removeLoginStateFromPreferencesStore(context)
                    removeTokenFromPreferencesStore(context)
                    removeTokenSecretFromPreferencesStore(context)
                }
                Log.i(TAG, "Clear out store data from preference")
            }
//        } else {
//            Log.i(TAG, "Use is still logged in!")
//        }
    }

    fun takeOnePhoto(context: Context, launcher: ActivityResultLauncher<Uri>){ twitterRepository.takeOnePhoto(context, launcher) }
    fun takeOneVideo(context: Context, launcher: ActivityResultLauncher<Uri>){ twitterRepository.takeOneVideo(context, launcher) }
}

class TweetViewModelFactory(
    private val tweetRepository: TweetRepository,
    private val receiveTokenRepository: ReceiveTokenRepository,
    private val dataStore: SettingDataStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(TweetViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return TweetViewModel(
                tweetRepository = tweetRepository,
                receiveTokenRepository = receiveTokenRepository,
                dataStore = dataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}