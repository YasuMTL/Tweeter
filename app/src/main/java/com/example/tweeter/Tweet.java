package com.example.tweeter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.UploadedMedia;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.conf.ConfigurationContext;
//<div>Icons made by <a href="https://www.flaticon.com/<?=_('authors/')?>freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>

public class Tweet extends AppCompatActivity implements View.OnClickListener
{
    Button btnTweet, btnLogin, btnLogout, btnClear, btnUploadPhoto;
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
    final int RESULT_LOAD_IMAGE = 1;
    private File imageToUpload;
    final int PERMISSION_REQUEST_CODE = 777;
    ArrayList<String> imagesPathList;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tweet);

        btnTweet = findViewById(R.id.btnSendTweet);
        btnLogin = findViewById(R.id.btnLogin);
        btnLogout = findViewById(R.id.btnLogOut);
        btnClear = findViewById(R.id.btnClear);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        etTweet = findViewById(R.id.etTweet);

        btnTweet.setOnClickListener(this);
        btnLogin.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        btnUploadPhoto.setOnClickListener(this);

        spTwitterToken = getSharedPreferences("twitterToken", MODE_PRIVATE);
        didILogIn = spTwitterToken.getBoolean("login", false);

        if (didILogIn){
            btnLogin.setVisibility(View.INVISIBLE);
            btnTweet.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.VISIBLE);
            btnClear.setVisibility(View.VISIBLE);
            btnUploadPhoto.setVisibility(View.VISIBLE);
        }else{
            btnLogin.setVisibility(View.VISIBLE);
            btnTweet.setVisibility(View.INVISIBLE);
            btnLogout.setVisibility(View.INVISIBLE);
            btnClear.setVisibility(View.INVISIBLE);
            btnUploadPhoto.setVisibility(View.INVISIBLE);
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

            case R.id.btnUploadPhoto:
                uploadPhotos();
                break;
        }
    }

    private void uploadPhotos(){
        //Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        Intent photoPickerIntent = new Intent();
        photoPickerIntent.setType("image/*");
        photoPickerIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        photoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(photoPickerIntent, RESULT_LOAD_IMAGE);
    }

    /*private String getPath(Context context, Uri uri) {
        String[] proj = {
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DISPLAY_NAME
        };

        String result = null,
                mime = null,
                name = null;

        Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                result = cursor.getString(0);
                mime = cursor.getString(1);
                name = cursor.getString(2);
            }
            cursor.close();
        }

        return result;
    }//END getPath*/

    public String getPathFromUri(final Context context, final Uri uri) {
        boolean isAfterKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        String TAG = "getPathFromUri";
        Log.e(TAG,"uri:" + uri.getAuthority());
        if (isAfterKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if ("com.android.externalstorage.documents".equals(
                    uri.getAuthority())) {// ExternalStorageProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }else {
                    return "/stroage/" + type +  "/" + split[1];
                }
            }else if ("com.android.providers.downloads.documents".equals(
                    uri.getAuthority())) {// DownloadsProvider
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }else if ("com.android.providers.media.documents".equals(
                    uri.getAuthority())) {// MediaProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                contentUri = MediaStore.Files.getContentUri("external");
                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }else if ("content".equalsIgnoreCase(uri.getScheme())) {//MediaStore
            return getDataColumn(context, uri, null, null);
        }else if ("file".equalsIgnoreCase(uri.getScheme())) {// File
            return uri.getPath();
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {
        Cursor cursor = null;
        final String[] projection = {
                MediaStore.Files.FileColumns.DATA
        };
        try {
            cursor = context.getContentResolver().query(
                    uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int cindex = cursor.getColumnIndexOrThrow(projection[0]);
                System.out.println("cursor.getString(cindex): " + cursor.getString(cindex));
                return cursor.getString(cindex);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){
            switch (requestCode){
                case RESULT_LOAD_IMAGE:
                    try{
                        imagesPathList = new ArrayList<String>();

                        System.out.println("++data" + data.getClipData().getItemCount());// Get count of image here.

                        for (int i=0; i<data.getClipData().getItemCount(); i++){
                            Uri selectedImage = data.getClipData().getItemAt(i).getUri();
                            //String type list which contains file path of each selected image file
                            imagesPathList.add(getPathFromUri(Tweet.this, selectedImage));
                            System.out.println("selectedImage: " + selectedImage);
                        }

                        /*String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        Cursor cursor = getContentResolver().query(imageUri, filePathColumn, null, null, null);
                        cursor.moveToFirst();

                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String imagePath = cursor.getString(columnIndex);
                        cursor.close();*/

                    }catch (Exception e){
                        e.printStackTrace();
                        Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }

        }else{
            Toast.makeText(this, "You haven't picked image from gallery", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkPermission() {

        int result = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);

        if (result == PackageManager.PERMISSION_GRANTED)
        {
            //Toast.makeText(this, "You are uploading an image...", Toast.LENGTH_SHORT).show();
            System.out.println("You are uploading an image...");
            return true;
        }
        // Permission denied
        else
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
                // User can get the permission via this dialog
                showInfoDialog();
            }else{
                // User cannot send an email anymore unless getting the permission manually in "App setting"
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }

            return false;
        }
    }//END checkPermission()

    // After clicking "Yes" on the dialog, you can have a permission request
    public void showInfoDialog(){
        new AlertDialog.Builder(this)
                .setTitle("What is this permission for?")
                .setMessage("You need the permission to upload an image on your tweet.\nPress \"OK\" to get the permission.")
                .setPositiveButton(
                        "OK",
                        new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which){
                                ActivityCompat.requestPermissions(Tweet.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                            }
                        }
                )
                .setNegativeButton(
                        "NO",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(Tweet.this, "Without the permission, you cannot upload an image!",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                )
                .show();
    }//END showInfoDialog

    private boolean notOverLetterLimit(){

        String tweetDraft = etTweet.getText().toString();
        int numTweetLetters = tweetDraft.length();

        return numTweetLetters < 141;
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
            //clearOutEtTweet();
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
                ConfigurationBuilder cb = setTwitterKeysAndTokens();
//                TwitterFactory twitterFactory = new TwitterFactory(cb.build());
                Twitter twitter = new TwitterFactory(cb.build()).getInstance();
                //twitterFactory.getInstance().updateStatus(tweetText[0]);

                //set text
                final StatusUpdate status = new StatusUpdate(tweetText[0]);
                //image set
                if (imagesPathList.size() > 1){
                    System.out.println("Uploading more than one image...");
                    //upload multiple image files (4 files at maximum)
                    long[] mediaIds = new long[imagesPathList.size()];
                    for (int i = 0; i < mediaIds.length; i++){
//                        UploadedMedia media = twitter.uploadMedia(new File(imagesPathList.get(i) + ".jpg"));
                        System.out.println("imagesPathList.get(i): " + imagesPathList.get(i));
                        UploadedMedia media = twitter.uploadMedia(new File(imagesPathList.get(i)));
                        mediaIds[i] = media.getMediaId();
                    }

                    status.setMediaIds(mediaIds);
                }else if (imagesPathList.size() == 1){
                    System.out.println("Uploading a single image...");
                    //upload one image file
                    status.media(new File(imagesPathList.get(0)));
                }else{
                    System.out.println("Uploading nothing...");
                    status.media(null);
                }
                /*if (imageToUpload.exists()){
                    status.media(imageToUpload);
                }else{
                    status.media(null);
                }*/
                //send tweet
                if (checkPermission()) {
                    //twitterFactory.getInstance().updateStatus(status);
                    twitter.updateStatus(status);
//                    clearOutEtTweet();
                }else{
                    Log.d("PERMISSION", "Permission denied");
                }
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