package com.yasu_k.source

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yasunari_k.saezuri.data.source.ReceiveTokenRepository
import org.junit.Assert.*

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import twitter4j.auth.AccessToken

@RunWith(AndroidJUnit4::class)
class ReceiveTokenRepositoryTest {
    val receiveTokenRepository = ReceiveTokenRepository()
    lateinit var accessToken: AccessToken
    val token = "token"
    val tokenSecret = "tokenSecret"

    @Before
    fun setUp() {
        accessToken = AccessToken(token, tokenSecret)
        println("accessToken.token ${accessToken.token}")
    }

    @Test
    fun logout_SetAccessToken_ThenLogOut_AccTokenStateIsNull() {
        receiveTokenRepository.updateAccTokenStateForUnitTest(accessToken = accessToken)
        receiveTokenRepository.logout()

        assert(receiveTokenRepository.accTokenState.value.accessToken == null)
    }

    @Test
    fun getTwitterInstance_CheckInstance() {
        val twitter = receiveTokenRepository.getTwitterInstance()

        val url = twitter.oAuthRequestToken.authorizationURL
        println("url = $url")

        assert(url.isNotEmpty())
    }
}