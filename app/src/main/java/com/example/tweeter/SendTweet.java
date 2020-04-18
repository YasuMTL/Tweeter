package com.example.tweeter;

import android.os.AsyncTask;

import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

class SendTweet extends AsyncTask<String, Integer, Integer>
{

    @Override
    protected Integer doInBackground(String... tweetText)
    {
        try
        {
            TwitterFactory tweet = new TwitterFactory();
            //tweet.getInstance().updateStatus("Test tweet from Twitter4j.");
            tweet.getInstance().updateStatus(tweetText[0]);
        }
        catch(TwitterException te)
        {
            te.printStackTrace();
            System.out.println(te);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Integer integer)
    {
        super.onPostExecute(integer);

        System.out.println("Tweet finish !!!");
    }
}
