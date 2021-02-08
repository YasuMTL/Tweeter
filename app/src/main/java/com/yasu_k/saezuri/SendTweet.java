package com.yasu_k.saezuri;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.UploadedMedia;
import twitter4j.conf.ConfigurationBuilder;

import static com.yasu_k.saezuri.LoginInfo.oAuthConsumerKey;
import static com.yasu_k.saezuri.LoginInfo.oAuthConsumerSecret;
import static com.yasu_k.saezuri.Tweet.imagesPathList;
import static com.yasu_k.saezuri.Tweet.oAuthAccessToken;
import static com.yasu_k.saezuri.Tweet.oAuthAccessTokenSecret;
import static com.yasu_k.saezuri.Tweet.selectedVideoPath;

//1st inner AsyncTask class
@SuppressLint("StaticFieldLeak")
class SendTweet extends AsyncTask<String, Integer, Integer>
{
    final String TAG = "SendTweet";
    int statusCode, errorCode;
    boolean uploadedMoreThan4Images = false,
            isVideoTooLarge = false,
            didIUploadNothing = false;
    Context mContext;
    private ProgressDialog progressDialog;
    EditText mEditText;

    public SendTweet(Context context, EditText editText){
        this.mContext = context;
        this.mEditText = editText;
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

    private void flushOutUploadedImageVideo(){
        imagesPathList.clear();
        selectedVideoPath = null;
    }

    @Override
    protected void onPreExecute() {
        //https://www.youtube.com/watch?v=fg9C2fEE4bY
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setCancelable(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgressStyle(0);
        progressDialog.setMax(100);
        progressDialog.setMessage(mContext.getString(R.string.tweet_sending));

        progressDialog.show();
        super.onPreExecute();
    }

    @Override
    protected Integer doInBackground(String... tweetText)
    {
        try
        {
            ConfigurationBuilder cb = setTwitterKeysAndTokens();
            Twitter twitter = new TwitterFactory(cb.build()).getInstance();

            //set text
            final StatusUpdate status = new StatusUpdate(tweetText[0]);

            //set video
            if (selectedVideoPath != null){

                try{
                    // https://ja.stackoverflow.com/questions/28169/android%E3%81%8B%E3%82%89-twitter4j-%E3%82%92%E4%BD%BF%E7%94%A8%E3%81%97%E3%81%A6%E5%8B%95%E7%94%BB%E3%82%92%E6%8A%95%E7%A8%BF%E3%82%92%E3%81%97%E3%81%9F%E3%81%84
                    FileInputStream is = null;
                    //String path = Environment.getExternalStorageDirectory().toString() + "/video.mp4";
                    File file = new File(selectedVideoPath);
                    is = new FileInputStream(file);
                    UploadedMedia video = twitter.uploadMediaChunked("video.mp4", is);
                    //https://github.com/Twitter4J/Twitter4J/issues/339

                    status.setMediaIds(video.getMediaId());
                    System.out.println("Uploading a video...");
                    is.close();
                }catch(OutOfMemoryError e){
                    e.printStackTrace();
                    isVideoTooLarge = true;
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            //Image set
            else if(imagesPathList.size() > 4){
                uploadedMoreThan4Images = true;
            }else if (imagesPathList.size() >= 1){
                System.out.println("Uploading image(s)...");
                //upload multiple image files (4 files at most)
                long[] mediaIds = new long[imagesPathList.size()];
                for (int i = 0; i < mediaIds.length; i++){
                    System.out.println("imagesPathList.get(i): " + imagesPathList.get(i));

                    File image = new File(imagesPathList.get(i));
                    System.out.println("image.length(): " + image.length());

                    UploadedMedia media = twitter.uploadMedia(image);
                    System.out.println("media.getImageType(): " + media.getImageType() + " media.getSize(): " + media.getSize());

                    mediaIds[i] = media.getMediaId();
                }

                status.setMediaIds(mediaIds);
            }else{
                System.out.println("Uploading nothing...");
                didIUploadNothing = true;
                status.setMedia(null);
            }

            //send tweet
            if (uploadedMoreThan4Images){
                System.out.println("You cannot upload more than 4 images.");
            }else if (isVideoTooLarge){
                System.out.println("The video you uploaded is too large! Less than 27 seconds video should be uploaded without problem...");
            }else{
                twitter.updateStatus(status);
                statusCode = 200;
                Log.d("TWEET", "The tweet was sent as expected...");
            }
        }
        catch(TwitterException te)
        {
            te.printStackTrace();
            Log.d(TAG, te.toString());
            System.out.println("te.getStatusCode(): " + te.getStatusCode());
            System.out.println("te.getMessage(): " + te.getMessage());
            System.out.println("te.getErrorCode(): " + te.getErrorCode());
            System.out.println("te.getErrorMessage(): " + te.getErrorMessage());
            statusCode = te.getStatusCode();
            errorCode = te.getErrorCode();
        }

        flushOutUploadedImageVideo();
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        progressDialog.setProgress(values[0]);
    }

    @Override
    protected void onPostExecute(Integer integer)
    {
        super.onPostExecute(integer);

        progressDialog.dismiss();
        //Handling error
        if(statusCode == 200){
            Toast.makeText(mContext, mContext.getString(R.string.tweet_sent_success), Toast.LENGTH_SHORT).show();
            clearOutEtTweet();
        }else if(statusCode == 503){
            Toast.makeText(mContext, mContext.getString(R.string.twitter_unavailable), Toast.LENGTH_LONG).show();
        }else if (statusCode == 403){
            switch (errorCode){
                case 170:
                    Toast.makeText(mContext, mContext.getString(R.string.no_text_to_tweet), Toast.LENGTH_SHORT).show();
                    break;

                case 193:
                    Toast.makeText(mContext, mContext.getString(R.string.media_is_too_large), Toast.LENGTH_LONG).show();
                    break;
                case -1:
                    Toast.makeText(mContext, mContext.getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
                    break;

                default:
            }

        }else if(statusCode == 400){
            Toast.makeText(mContext, mContext.getString(R.string.request_invalid), Toast.LENGTH_SHORT).show();
        }else if (uploadedMoreThan4Images){
            Toast.makeText(mContext, mContext.getString(R.string.four_images_at_most), Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(mContext, mContext.getString(R.string.unknown_error), Toast.LENGTH_SHORT).show();
        }

    }

    private void clearOutEtTweet(){
        mEditText.setText("");
    }
}