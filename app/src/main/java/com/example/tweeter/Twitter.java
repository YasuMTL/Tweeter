package com.example.tweeter;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Twitter extends AppCompatActivity
{
    Button btnTweet;
    TextView tv;
    EditText etTweet;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tweet);

        btnTweet = findViewById(R.id.btnTweet);
        tv = findViewById(R.id.tv);
        etTweet = findViewById(R.id.etTweet);


        //https://qiita.com/yuta-tsussy/items/e6ff450667fcaeffa2ed
    }

    public void onClickTweet(View v)
    {
        tv.setText("Now Sending");

        new SendTweet().execute(etTweet.getText().toString());

        tv.setText("Finish");
    }

    public void logOut(View v)
    {
        finish();
    }
}
