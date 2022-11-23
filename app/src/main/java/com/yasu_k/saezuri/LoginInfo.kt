package com.yasu_k.saezuri

import twitter4j.auth.OAuthAuthorization
import twitter4j.auth.RequestToken

object LoginInfo {
    // Login
    @JvmField
    var mOauth: OAuthAuthorization? = null
    @JvmField
    var mRequest: RequestToken? = null

    // Twitter
    const val oAuthConsumerKey = "LJi96Jk8iC8HsipGdi7TazT9q"
    const val oAuthConsumerSecret = "N9Rf7eTxmy4YLHtaoQeAVoImowcAbFc0KYsUEoUTipi8Q80y6L"
    const val CALLBACK_URL = "callback://"
}