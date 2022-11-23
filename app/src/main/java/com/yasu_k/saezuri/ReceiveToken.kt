package com.yasu_k.saezuri

import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.yasu_k.saezuri.LoginInfo.mOauth
import com.yasu_k.saezuri.LoginInfo.mRequest
import com.yasu_k.saezuri.Tweet
import twitter4j.TwitterException
import twitter4j.auth.AccessToken
import java.util.concurrent.ExecutionException

class ReceiveToken : AppCompatActivity() {
    private var uri: Uri? = null
    private var mToken: AccessToken? = null
    private var token: String? = null
    private var tokenSecret: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receive_token)
        uri = intent.data
        if (uri != null && uri.toString().startsWith("callback://ReceiveToken")) {
            try {
                fetchTwitterToken().execute().get()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } //END if
//        val btnBackToTweet = findViewById<Button>(R.id.btnBackToTweet)
//        btnBackToTweet.setOnClickListener {
//            val backToTweet = Intent(this@ReceiveToken, Tweet::class.java)
//            backToTweet.putExtra("token", token)
//            backToTweet.putExtra("tokenSecret", tokenSecret)
//            startActivity(backToTweet)
//        }
    }

    internal inner class fetchTwitterToken : AsyncTask<Any?, Any?, Any?>() {
        override fun doInBackground(objects: Array<Any?>): Any? {
            val verifier = uri!!.getQueryParameter("oauth_verifier")
            try {
                mToken = mOauth?.getOAuthAccessToken(mRequest, verifier)
            } catch (e: TwitterException) {
                e.printStackTrace()
            }
            return mToken
        }

        override fun onPostExecute(o: Any?) {
            token = mToken!!.token
            tokenSecret = mToken!!.tokenSecret
        }
    } //END fetchTwitterToken
}