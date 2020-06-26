package com.yasu_k.saezuri;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.concurrent.ExecutionException;

import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;

public class ReceiveToken extends AppCompatActivity {
    private Uri uri;
    private AccessToken mToken;
    private String token, tokenSecret;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_token);

        uri = getIntent().getData();

        if (uri != null && uri.toString().startsWith("callback://ReceiveToken")){
            try {
                new fetchTwitterToken().execute().get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e){
                e.printStackTrace();
            }
        }//END if

        Button btnBackToTweet = findViewById(R.id.btnBackToTweet);
        btnBackToTweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent backToTweet = new Intent(ReceiveToken.this, Tweet.class);

                backToTweet.putExtra("token", token);
                backToTweet.putExtra("tokenSecret", tokenSecret);

                startActivity(backToTweet);
            }
        });
    }

    class fetchTwitterToken extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            String varifier = uri.getQueryParameter("oauth_verifier");

            try {
                mToken = Tweet.mOauth.getOAuthAccessToken(Tweet.mRequest, varifier);
            } catch (TwitterException e) {
                e.printStackTrace();
            }

            return mToken;
        }

        @Override
        protected void onPostExecute(Object o) {
            token = mToken.getToken();
            tokenSecret = mToken.getTokenSecret();
        }
    }//END fetchTwitterToken
}