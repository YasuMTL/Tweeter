package com.yasu_k.saezuri.data.source

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat.startActivityForResult
import com.yasu_k.saezuri.LoginInfo
import com.yasu_k.saezuri.R
import com.yasu_k.saezuri.data.source.ReceiveTokenRepository.Companion.editorTwitterToken
import com.yasu_k.saezuri.data.source.ReceiveTokenRepository.Companion.oAuthAccessToken
import com.yasu_k.saezuri.data.source.ReceiveTokenRepository.Companion.oAuthAccessTokenSecret
import com.yasu_k.saezuri.ui.TweetFragment
import com.yasu_k.saezuri.ui.spTwitterToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import twitter4j.TwitterException
import twitter4j.conf.ConfigurationBuilder
import javax.inject.Inject

@Inject
lateinit var spTwitterToken: SharedPreferences

class ReceiveTokenRepository {
    init {
        val isUserAlreadyLoggedIn = spTwitterToken.getBoolean("login", false)
        Log.d(localClassName, "isUserAlreadyLoggedIn = $isUserAlreadyLoggedIn")
    }

    companion object {
        //@JvmField
        var oAuthAccessToken: String? = null
        //@JvmField
        var oAuthAccessTokenSecret: String? = null
        lateinit var editorTwitterToken: SharedPreferences.Editor
    }

    fun logout() {
        val TAG = "logout()"
        Log.d(TAG, "--------------- START ---------------")
        TweetFragment.editorTwitterToken = spTwitterToken.edit()
        TweetFragment.editorTwitterToken.putBoolean("login", false)
        TweetFragment.editorTwitterToken.putString("token", null)
        TweetFragment.editorTwitterToken.putString("tokenSecret", null)
        TweetFragment.editorTwitterToken.apply()
        Toast.makeText(this, getString(R.string.logout_success), Toast.LENGTH_SHORT).show()
        Log.d(TAG, "--------------- END ---------------")
    }

    fun login() {
        //LoginTwitter().execute()
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch { loginTwitter() }
    }

    private suspend fun loginTwitter()
    {
        try {
            // onPreExecuteと同等の処理
            withContext(Dispatchers.Main) {
                Log.d(localClassName, "始めます")
            }

            // doInBackgroundメソッドと同等の処理
            Thread.sleep(800)
            //withContext(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                Log.d(localClassName, "Logging in Twitter")
                LoginInfo.mOauth?.setOAuthConsumer(
                    LoginInfo.oAuthConsumerKey,
                    LoginInfo.oAuthConsumerSecret
                )
                LoginInfo.mOauth?.oAuthAccessToken = null

                try {
                    LoginInfo.mRequest =
                        LoginInfo.mOauth?.getOAuthRequestToken("callback://ReceiveToken") //ReceiveToken will receive user's token for login.
                    val uri = Uri.parse(LoginInfo.mRequest?.authenticationURL)
                    val login = Intent(Intent.ACTION_VIEW, uri)
                    startActivityForResult(login, 0) //Implicit intent to log in on web browser
                } catch (e: TwitterException) {
                    e.printStackTrace()
                }
            }
            Thread.sleep(800)

            // onPostExecuteメソッドと同等の処理
            withContext(Dispatchers.Main) {
                Log.d(localClassName, "終わります")
            }
        } catch (e: Exception) {
            // onCancelledメソッドと同等の処理
            Log.e(localClassName, "ここにキャンセル時の処理を記述", e)
        }
    }

    private fun setTwitterKeysAndTokens(): ConfigurationBuilder
    {
        val cb = ConfigurationBuilder()
        cb.setDebugEnabled(true)
            .setOAuthConsumerKey(LoginInfo.oAuthConsumerKey)
            .setOAuthConsumerSecret(LoginInfo.oAuthConsumerSecret)
            .setOAuthAccessToken(TweetFragment.oAuthAccessToken)
            .setOAuthAccessTokenSecret(TweetFragment.oAuthAccessTokenSecret)
        return cb
    }
}