package com.example.tweeter;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.conf.ConfigurationContext;
//<div>Icons made by <a href="https://www.flaticon.com/<?=_('authors/')?>freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>

public class Tweet extends AppCompatActivity implements View.OnClickListener
{
    Button btnTweet, btnLogin, btnLogout, btnClear;
    EditText etTweet;
    // Login
    static OAuthAuthorization mOauth;
    static RequestToken mRequest;
    private String oAuthConsumerKey,
            oAuthConsumerSecret,
            oAuthAccessToken,
            oAuthAccessTokenSecret;
    private boolean didILogIn;
    private SharedPreferences spTwitterToken;
    private SharedPreferences.Editor editorTwitterToken;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tweet);

        btnTweet = findViewById(R.id.btnSendTweet);
        btnLogin = findViewById(R.id.btnLogin);
        btnLogout = findViewById(R.id.btnLogOut);
        btnClear = findViewById(R.id.btnClear);
        etTweet = findViewById(R.id.etTweet);

        btnTweet.setOnClickListener(this);
        btnLogin.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
        btnClear.setOnClickListener(this);

        spTwitterToken = getSharedPreferences("twitterToken", MODE_PRIVATE);
        didILogIn = spTwitterToken.getBoolean("login", false);

        if (didILogIn){
            btnLogin.setVisibility(View.INVISIBLE);
            btnTweet.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.VISIBLE);
            btnClear.setVisibility(View.VISIBLE);
        }else{
            btnLogin.setVisibility(View.VISIBLE);
            btnTweet.setVisibility(View.INVISIBLE);
            btnLogout.setVisibility(View.INVISIBLE);
            btnClear.setVisibility(View.INVISIBLE);
        }

        mOauth = new OAuthAuthorization(ConfigurationContext.getInstance());
        getTwitterKeysAndTokens();
    }

    private void getTwitterKeysAndTokens(){
        oAuthConsumerKey = "LJi96Jk8iC8HsipGdi7TazT9q";
        oAuthConsumerSecret = "N9Rf7eTxmy4YLHtaoQeAVoImowcAbFc0KYsUEoUTipi8Q80y6L";

        //Retrieve token after the login (for the first time)
        if (getIntent().getStringExtra("token") != null)
        {
            saveTokenIntoSharedPreferences();
        }
        //Retrieve token from SharedPreferences
        else if(spTwitterToken.contains("token"))
        {
            oAuthAccessToken = spTwitterToken.getString("token", "");
            oAuthAccessTokenSecret = spTwitterToken.getString("tokenSecret", "");
        }
        else
        {
            oAuthAccessToken = null;
            oAuthAccessTokenSecret = null;
        }
    }//END getKeysAndTokens

    private void saveTokenIntoSharedPreferences() {
        editorTwitterToken = spTwitterToken.edit();

        oAuthAccessToken = getIntent().getStringExtra("token");
        editorTwitterToken.putString("token", oAuthAccessToken);

        oAuthAccessTokenSecret = getIntent().getStringExtra("tokenSecret");
        editorTwitterToken.putString("tokenSecret", oAuthAccessTokenSecret);

        editorTwitterToken.apply();
    }

    private void checkIfILoggedIn(){
        editorTwitterToken = spTwitterToken.edit();
        editorTwitterToken.putBoolean("login", true);
        editorTwitterToken.apply();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btnLogin:
                checkIfILoggedIn();
                login();
                break;

            case R.id.btnSendTweet:
                if (notOverLetterLimit()){
                    tweet();
                }else{
                    Toast.makeText(this, "Over 140 letters!", Toast.LENGTH_SHORT).show();
                }

                break;

            case R.id.btnLogOut:
                logout();
                //Need to review. Look at "Enregistreur harcèlement"
                Intent redirect = new Intent(Tweet.this, Tweet.class);
                startActivity(redirect);
                break;

            case R.id.btnClear:
                etTweet.setText("");
                break;
        }
    }

    private boolean notOverLetterLimit(){

        String tweetDraft = etTweet.getText().toString();
        int numTweetLetters = tweetDraft.length();

        if (numTweetLetters < 141){
            return true;
        }else{
            return false;
        }
    }

    private void logout(){
        final String TAG = "logout()";
        Log.d(TAG, "--------------- START ---------------");

        editorTwitterToken = spTwitterToken.edit();
        editorTwitterToken.putBoolean("login", false);
        editorTwitterToken.putString("token", null);
        editorTwitterToken.putString("tokenSecret", null);
        editorTwitterToken.apply();

        Toast.makeText(this, "You have successfully logged out", Toast.LENGTH_SHORT).show();

        Log.d(TAG, "--------------- END ---------------");
    }

    private void login() {
        new LoginTwitter().execute();
    }//END login

    private void clearOutEtTweet(){
        etTweet.setText("");
    }

    private void tweet() {
        Toast.makeText(this, "Now sending", Toast.LENGTH_SHORT).show();
        boolean wasTweetSent = true;

        try {
            new SendTweet().execute(etTweet.getText().toString());
        }catch (Exception e){
            e.printStackTrace();
            wasTweetSent = false;
        }

        if (wasTweetSent){
            clearOutEtTweet();
            Toast.makeText(this, "Finish", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "Something is wrong...", Toast.LENGTH_SHORT).show();
        }
    }

    private ConfigurationBuilder setTwitterKeysAndTokens(){
        ConfigurationBuilder cb = new ConfigurationBuilder();

        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(oAuthConsumerKey)
                .setOAuthConsumerSecret(oAuthConsumerSecret)
                .setOAuthAccessToken(oAuthAccessToken)
                .setOAuthAccessTokenSecret(oAuthAccessTokenSecret);

        return cb;
    }

    //1st inner AsyncTask class
    class SendTweet extends AsyncTask<String, Integer, Integer>
    {
        final String TAG = "SendTweet";

        @Override
        protected Integer doInBackground(String... tweetText)
        {
            try
            {
                //uploadVideo();
                ConfigurationBuilder cb = setTwitterKeysAndTokens();
                TwitterFactory twitterFactory = new TwitterFactory(cb.build());
                twitterFactory.getInstance().updateStatus(tweetText[0]);
            }
            catch(TwitterException te)
            {
                te.printStackTrace();
                Log.d(TAG, te.toString());
            }

            return null;
        }

        @Override
        protected void onPostExecute(Integer integer)
        {
            super.onPostExecute(integer);

            System.out.println("Tweet finish !!!");
        }
    }//END SendTweet

    //2nd inner AsyncTask class
    class LoginTwitter extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {

            mOauth.setOAuthConsumer(oAuthConsumerKey, oAuthConsumerSecret);
            mOauth.setOAuthAccessToken(null);

            try {
                mRequest = mOauth.getOAuthRequestToken("callback://ReceiveToken"); //ReceiveToken will receive user's token for login.
                Uri uri = Uri.parse(mRequest.getAuthenticationURL());
                Intent login = new Intent(Intent.ACTION_VIEW, uri);
                startActivityForResult(login, 0); //Implicit intent to log in on web browser
            } catch (TwitterException e) {
                e.printStackTrace();
            } catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }
    }//END LoginTwitter
}