package com.yasu_k.saezuri.data.source

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.LifecycleCoroutineScope
import com.yasu_k.saezuri.LoginInfo
import com.yasu_k.saezuri.LoginInfo.oAuthConsumerKey
import com.yasu_k.saezuri.LoginInfo.oAuthConsumerSecret
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import twitter4j.conf.ConfigurationBuilder

//@Inject
//lateinit var spTwitterToken: SharedPreferences
data class TokenState(
    val accessToken: AccessToken? = null
)

class ReceiveTokenRepository {

    lateinit var twitter: Twitter
    lateinit var twitterDialog: Dialog
    var accToken: AccessToken? = null
    private val _accTokenState = MutableStateFlow(TokenState())
    val accTokenState: StateFlow<TokenState> = _accTokenState.asStateFlow()

    fun logout() {
        val TAG = "logout()"
        Log.d(TAG, "--------------- START ---------------")
        _accTokenState.update {
            it.copy(accessToken = null)
        }
        Log.d(TAG, "--------------- END ---------------")
    }

    fun updateAccTokenStateForUnitTest(accessToken: AccessToken) {
        _accTokenState.update {
            it.copy(accessToken = accessToken)
        }
    }

    fun getRequestToken(
        context: Context,
        scope: LifecycleCoroutineScope
    )
    = scope.launch(Dispatchers.Default) {
        twitter = getTwitterInstance()

        try {
            val requestToken = twitter.oAuthRequestToken

            withContext(Dispatchers.Main) {
                setupTwitterWebviewDialog(requestToken.authorizationURL, context, scope)
            }
        } catch (e: IllegalStateException) {
            Log.e("ERROR: ", e.toString())
        }
    }

    fun getTwitterInstance(): Twitter {
        val builder = ConfigurationBuilder()
            .setDebugEnabled(true)
            .setOAuthConsumerKey(oAuthConsumerKey)
            .setOAuthConsumerSecret(oAuthConsumerSecret)
            .setIncludeEmailEnabled(true)
        val config = builder.build()
        val factory = TwitterFactory(config)

        return factory.instance
    }

    // Show twitter login page in a dialog
    @SuppressLint("SetJavaScriptEnabled")
    fun setupTwitterWebviewDialog(
        url: String,
        context: Context,
        scope: LifecycleCoroutineScope
    ) {
        twitterDialog = Dialog(context)
        val webView = WebView(context)

        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.webViewClient = TwitterWebViewClient(scope)
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(url)
        Log.d("WebView", "url = $url")

        twitterDialog.setContentView(webView)
        twitterDialog.show()
    }

    // Get the oauth_verifier
    private fun handleUrl(url: String, scope: LifecycleCoroutineScope) {
        val uri = Uri.parse(url)
        val oauthVerifier = uri.getQueryParameter("oauth_verifier") ?: ""

        if (oauthVerifier.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                accToken =
                    withContext(Dispatchers.IO) { twitter.getOAuthAccessToken(oauthVerifier) }
                if (accToken == null) {
                    _accTokenState.update {
                        it.copy(accessToken = null)
                    }
                } else {
                    _accTokenState.update {
                        it.copy(accessToken = accToken)
                    }
                }
                getUserProfile()
            }
        }
    }

    private suspend fun getUserProfile() {
        val usr = withContext(Dispatchers.IO) { twitter.verifyCredentials() }
        //Twitter Id
        val twitterId = usr.id.toString()
        Log.d("Twitter Id: ", twitterId)

        //Twitter Handle
        val twitterHandle = usr.screenName
        Log.d("Twitter Handle: ", twitterHandle)
        //Twitter Name
        val twitterName = usr.name
        Log.d("Twitter Name: ", twitterName)
        //Twitter Email
        val twitterEmail = usr.email
        Log.d("Twitter Email: ", twitterEmail ?: "'Request email address from users' on the Twitter dashboard is disabled")
        // Twitter Profile Pic URL
        val twitterProfilePic = usr.profileImageURLHttps.replace("_normal", "")
        Log.d("Twitter Profile URL: ", twitterProfilePic)
        // Twitter Access Token
        Log.d("Twitter Access Token", accToken?.token ?: "")
    }

    inner class TwitterWebViewClient(private val scope: LifecycleCoroutineScope) : WebViewClient() {
        //@TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            if (request?.url.toString().startsWith(LoginInfo.CALLBACK_URL)) {
                Log.d("Authorization URL: ", request?.url.toString())
                handleUrl(request?.url.toString(), scope)

                // Close the dialog after getting the oauth_verifier
                if (request?.url.toString().contains(LoginInfo.CALLBACK_URL)) {
                    twitterDialog.dismiss()
                }
                return true
            }
            return false
        }

        // For API 19 and below
        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (url.startsWith(LoginInfo.CALLBACK_URL)) {
                Log.d("Authorization URL: ", url)
                handleUrl(url, scope)

                // Close the dialog after getting the oauth_verifier
                if (url.contains(LoginInfo.CALLBACK_URL)) {
                    twitterDialog.dismiss()
                }
                return true
            }
            return false
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            Log.d("WebView", "Start loading")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            Log.d("WebView", "Web View was loaded")
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            val requestStr: String = request.toString()
            val errorStr: String = error.toString()
            Log.d("WebView", "request = $requestStr error = $errorStr")
        }
    }
}