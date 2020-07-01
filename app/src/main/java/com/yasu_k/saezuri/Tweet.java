package com.yasu_k.saezuri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import twitter4j.HttpParameter;
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
    private EditText etTweet;
    // Login
    static OAuthAuthorization mOauth;
    static RequestToken mRequest;
    private String oAuthConsumerKey,
            oAuthConsumerSecret,
            oAuthAccessToken,
            oAuthAccessTokenSecret;
    private SharedPreferences spTwitterToken;
    private SharedPreferences.Editor editorTwitterToken;
    final int RESULT_LOAD_IMAGE = 1;
    final int PERMISSION_REQUEST_CODE = 777;
    final int RESULT_LOAD_VIDEO = 2;
    final int REQUEST_VIDEO_CAPTURE = 3;
    final int REQUEST_TAKE_PHOTO = 4;
    private ArrayList<String> imagesPathList;
    private String selectedVideoPath;
    private ProgressDialog progressDialog;
    private Uri mCapturedImageURI;
    private File cameraFile;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tweet);

        final Button btnTweet = findViewById(R.id.btnSendTweet),
                btnLogin = findViewById(R.id.btnLogin),
                btnLogout = findViewById(R.id.btnLogOut),
                btnClear = findViewById(R.id.btnClear),
                btnUploadPhotoVideo = findViewById(R.id.btnUploadPhotoVideo);
        TextInputLayout textInputLayout = findViewById(R.id.textInputLayout);
        etTweet = findViewById(R.id.etTweet);
        final TextView tvTweetTextCount = findViewById(R.id.tvTweetTextCount);

        btnTweet.setOnClickListener(this);
        btnLogin.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        btnUploadPhotoVideo.setOnClickListener(this);

        spTwitterToken = getSharedPreferences("twitterToken", MODE_PRIVATE);
        boolean didILogIn = spTwitterToken.getBoolean("login", false);

        if (didILogIn){
            btnLogin.setVisibility(View.INVISIBLE);
            btnTweet.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.VISIBLE);
            btnClear.setVisibility(View.VISIBLE);
            btnUploadPhotoVideo.setVisibility(View.VISIBLE);
            textInputLayout.setVisibility(View.VISIBLE);
        }else{
            btnLogin.setVisibility(View.VISIBLE);
            btnTweet.setVisibility(View.INVISIBLE);
            btnLogout.setVisibility(View.INVISIBLE);
            btnClear.setVisibility(View.INVISIBLE);
            btnUploadPhotoVideo.setVisibility(View.INVISIBLE);
            textInputLayout.setVisibility(View.INVISIBLE);
        }

        //hyperlink
        TextView privacyPolicy = (TextView) findViewById(R.id.tvPrivacyPolicy);
        privacyPolicy.setMovementMethod(LinkMovementMethod.getInstance());

        imagesPathList = new ArrayList<>();

        mOauth = new OAuthAuthorization(ConfigurationContext.getInstance());
        getTwitterKeysAndTokens();

        etTweet.addTextChangedListener(new TextWatcher(){

            public void beforeTextChanged(CharSequence s, int start, int count,int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final int textColor;
                int length = 140 - s.length();

                if(length < 0){
                    textColor = Color.RED;
                    btnTweet.setEnabled(false);
                    btnTweet.setTextColor(Color.GRAY);
                }else{
                    textColor = Color.GRAY;
                    btnTweet.setEnabled(true);
                    btnTweet.setTextColor(Color.WHITE);
                }
                tvTweetTextCount.setTextColor(textColor);
                tvTweetTextCount.setText(String.valueOf(length));
            }

            public void afterTextChanged(Editable s) {
            }
        });
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
                    Toast.makeText(this, getString(R.string.over140), Toast.LENGTH_SHORT).show();
                }

                break;

            case R.id.btnLogOut:
                logout();
                //Need to review. Look at "Enregistreur harcèlement"
                Intent redirect = new Intent(Tweet.this, Tweet.class);
                startActivity(redirect);
                break;

            case R.id.btnClear:
                clearOutEtTweet();
                break;

            case R.id.btnUploadPhotoVideo:
                requestTwoPermissions();
                showOptionMediaDialog();
                break;
        }
    }

    public void showOptionMediaDialog(){
        String[] mediaOptions = {"Select image(s)", "Select a video", "Capture a photo", "Capture a video"};

        /*ImageView image = new ImageView(this);
        image.setImageResource(R.drawable.twitter);*/

        new MaterialAlertDialogBuilder(this)
                .setItems(mediaOptions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        switch (which){
                            case 0://select images
                                if (checkPermissionToReadStorage()){
                                    uploadPhotos();
                                }
                                break;
                            case 1://select a video
                                if (checkPermissionToReadStorage()){
                                    uploadVideo();
                                }
                                break;
                            case 2://take a photo
                                if (checkPermissionToTakePhoto()){
                                    takeOnePhoto();
                                }
                                break;
                            case 3://capture a video
                                if (checkPermissionToTakePhoto()){
                                    captureOneVideo();
                                }
                                break;
                        }
                    }
                })
                //.setView(image)
                .show();
    }

    private void uploadVideo(){
        Intent videoPickerIntent = new Intent();
        videoPickerIntent.setType("video/*");
//        videoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
        videoPickerIntent.setAction(Intent.ACTION_PICK);
        startActivityForResult(Intent.createChooser(videoPickerIntent, "Select Video"), RESULT_LOAD_VIDEO);
    }

    private void uploadPhotos(){
        Intent photoPickerIntent = new Intent();
        photoPickerIntent.setType("image/*");
        photoPickerIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
        String[] mimeTypes = {"image/jpeg", "image/png"};
        photoPickerIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        //photoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
        photoPickerIntent.setAction(Intent.ACTION_PICK);
        startActivityForResult(Intent.createChooser(photoPickerIntent, "Select Image"), RESULT_LOAD_IMAGE);
    }

    private void takeOnePhoto(){
        // Determine a folder to save the captured image
        File cFolder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);//DIRECTORY_DCIM

        String fileDate = new SimpleDateFormat(
                "ddHHmmss", Locale.getDefault()).format(new Date());

        String fileName = String.format("CameraIntent_%s.jpg", fileDate);

        cameraFile = new File(cFolder, fileName);

        // This is not very useful so far...
        mCapturedImageURI = FileProvider.getUriForFile(
                Tweet.this,
                getApplicationContext().getPackageName() + ".fileprovider",
                cameraFile);

        Intent takeOnePhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takeOnePhoto.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

        if (takeOnePhoto.resolveActivity(getPackageManager()) != null){
            startActivityForResult(takeOnePhoto, REQUEST_TAKE_PHOTO);
        }
    }

    private void captureOneVideo(){
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    public String getPathFromUri(final Context context, final Uri uri) {
        //boolean isAfterKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        String TAG = "getPathFromUri";
        Log.e(TAG,"uri:" + uri.getAuthority());
        if (DocumentsContract.isDocumentUri(context, uri)) {
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
                        Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
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
            try {
                if (requestCode == RESULT_LOAD_IMAGE) {
                    if (null != data){
                        String imagePath;

                        if (data.getData() != null) {
                            //When an image is picked
                            Uri mImageUri = data.getData();
                            imagePath = getPathFromUri(this, mImageUri);

                            sizePhotoCheck(imagePath);
                        } else {
                            //When multiple images are picked
                            if (data.getClipData() != null) {
                                System.out.println("++data: " + data.getClipData().getItemCount());// Get count of image here.

                                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                                    Uri selectedImage = data.getClipData().getItemAt(i).getUri();
                                    //String type list which contains file path of each selected image file
                                    imagePath = getPathFromUri(Tweet.this, selectedImage);

                                    sizePhotoCheck(imagePath);
                                    System.out.println("selectedImage: " + selectedImage);
                                }
                            }
                        }

                        System.out.println("data.getData(): " + data.getData());
                    }else{
                        Toast.makeText(this, getString(R.string.not_picked_images), Toast.LENGTH_SHORT).show();
                    }
                }else if (requestCode == RESULT_LOAD_VIDEO){
                    Uri selectedImageUri = data.getData();

                    // MEDIA GALLERY
                    selectedVideoPath = getPathFromUri(this, selectedImageUri);
                    sizeVideoCheck(selectedVideoPath);
                }else if (requestCode == REQUEST_VIDEO_CAPTURE){
                    Uri newVideoUri = data.getData();

                    // MEDIA GALLERY
                    selectedVideoPath = getPathFromUri(this, newVideoUri);
                    sizeVideoCheck(selectedVideoPath);
                }else if (requestCode == REQUEST_TAKE_PHOTO){
                    if (cameraFile != null){
                        //registerDatabase(cameraFile);
                        System.out.println("cameraFile: " + cameraFile);
                        System.out.println("cameraFile.getAbsolutePath(): " + cameraFile.getAbsolutePath());
                        System.out.println("mCapturedImageURI: " + mCapturedImageURI);
                        System.out.println("mCapturedImageURI.getAuthority(): " + mCapturedImageURI.getAuthority());

                        String imagePath = cameraFile.getAbsolutePath();

                        if (cameraFile.length() <= 5000000){
                            imagesPathList.add(imagePath);
                        }else{
                            imagesPathList.clear();
                            Toast.makeText(this, getString(R.string.size_too_large), Toast.LENGTH_LONG).show();
                        }
                    }else{
                        Toast.makeText(this, getString(R.string.fail_photo), Toast.LENGTH_SHORT).show();
                    }
                }
            }catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, getString(R.string.attach_fail), Toast.LENGTH_SHORT).show();
            }
        }
    }//END onActivityResult()

    private void sizePhotoCheck(String filePath){
        File imageFile = new File(filePath);

        // Image size <= 5 MB (https://developer.twitter.com/en/docs/media/upload-media/uploading-media/media-best-practices)
        if (imageFile.length() <= 5000000){
            imagesPathList.add(filePath);
        }else{
            imagesPathList.clear();
            Toast.makeText(this, getString(R.string.size_too_large), Toast.LENGTH_LONG).show();
        }
    }

    private void sizeVideoCheck(String filePath){
        File fileToCheck = new File(filePath);

        //Video file size must not exceed 512 MB (https://developer.twitter.com/en/docs/media/upload-media/uploading-media/media-best-practices)
        if (fileToCheck.length() > 512000000) {
            selectedVideoPath = "";
        }
    }

    // Runtime Permission check
    private void requestTwoPermissions(){
        // If at least one of two permissions isn't yet granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(Tweet.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
        }
    }

    // Runtime Permission check
    private boolean checkPermissionToTakePhoto(){

        int checkPermissionCamera = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
        String whichPermission = "camera";

        if (checkPermissionCamera == PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        // Permission denied
        else
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)){
                // User can get the permission via this dialog
                showInfoDialog(whichPermission);
            }else{
                // User cannot send an email anymore unless getting the permission manually in "App setting"
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
            }
            return false;
        }
    }

    private boolean checkPermissionToReadStorage() {

        int result = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        String whichPermission = "read";

        if (result == PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        // Permission denied
        else
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
                // User can get the permission via this dialog
                showInfoDialog(whichPermission);
            }else{
                // User cannot send an email anymore unless getting the permission manually in "App setting"
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
            return false;
        }
    }//END checkPermission()

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE){
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED){
                //Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_SHORT).show();
                Toast.makeText(this, getString(R.string.need_permission), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // After clicking "Yes" on the dialog, you can have a permission request
    public void showInfoDialog(String whichPermission){
        if (whichPermission.equals("camera")){
            new AlertDialog.Builder(this)
                    //.setTitle("Permission to attach photos and video")
                    .setMessage("To attach images or video on your tweet, press \"OK\" to get the permissions.")
                    .setPositiveButton(
                            "OK",
                            new DialogInterface.OnClickListener(){
                                @Override
                                public void onClick(DialogInterface dialog, int which){
                                    ActivityCompat.requestPermissions(Tweet.this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
                                }
                            }
                    )
                    .setNegativeButton(
                            "NO",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Toast.makeText(Tweet.this, getString(R.string.warning_no_permission),
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                    )
                    .show();
        }
        else if (whichPermission.equals("read"))
        {
            new AlertDialog.Builder(this)
                    .setMessage("To attach images or video on your tweet, press \"OK\" to get the permissions.")
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
                                    Toast.makeText(Tweet.this, getString(R.string.warning_no_permission),
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                    )
                    .show();
        }

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

        Toast.makeText(this, getString(R.string.logout_success), Toast.LENGTH_SHORT).show();

        Log.d(TAG, "--------------- END ---------------");
    }

    private void login() {
        new LoginTwitter().execute();
    }//END login

    private void clearOutEtTweet(){
        etTweet.setText("");
    }

    private void tweet() {
        try {
            new SendTweet().execute(etTweet.getText().toString());
        }catch (Exception e){
            e.printStackTrace();
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

    private void flushOutUploadedImageVideo(){
        System.out.println("FLUSH !!!!!!!!!!!!!!!!!!!!!!!!!! ");
        imagesPathList.clear();
        selectedVideoPath = null;
    }

    //1st inner AsyncTask class
    @SuppressLint("StaticFieldLeak")
    class SendTweet extends AsyncTask<String, Integer, Integer>
    {
        final String TAG = "SendTweet";
        int statusCode, errorCode;
        boolean uploadedMoreThan4Images = false,
                isVideoTooLarge = false,
                didIUploadNothing = false;

        @Override
        protected void onPreExecute() {

            //https://www.youtube.com/watch?v=fg9C2fEE4bY
            progressDialog = new ProgressDialog(Tweet.this);
            progressDialog.setCancelable(true);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setProgressStyle(0);
            progressDialog.setMax(100);
            progressDialog.setMessage(getString(R.string.tweet_sending));

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
            /*catch (FileNotFoundException fe){
                fe.printStackTrace();
            }*/
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
                Toast.makeText(Tweet.this, getString(R.string.tweet_sent_success), Toast.LENGTH_SHORT).show();
                clearOutEtTweet();
            }else if(statusCode == 503){
                Toast.makeText(Tweet.this, getString(R.string.twitter_unavailable), Toast.LENGTH_LONG).show();
            }else if (statusCode == 403){
                switch (errorCode){
                    case 170:
                        Toast.makeText(Tweet.this, getString(R.string.no_text_to_tweet), Toast.LENGTH_SHORT).show();
                        break;

                    case 193:
                        Toast.makeText(Tweet.this, getString(R.string.media_is_too_large), Toast.LENGTH_LONG).show();
                        break;
                    case -1:
                        Toast.makeText(Tweet.this, getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
                        break;

                    default:
                }

            }else if(statusCode == 400){
                Toast.makeText(Tweet.this, getString(R.string.request_invalid), Toast.LENGTH_SHORT).show();
            }else if (uploadedMoreThan4Images){
                Toast.makeText(Tweet.this, getString(R.string.four_images_at_most), Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(Tweet.this, getString(R.string.unknown_error), Toast.LENGTH_SHORT).show();
            }

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