package com.yasu_k.saezuri

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.ads.AdView
import com.yasu_k.saezuri.databinding.TweetBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import twitter4j.StatusUpdate
import twitter4j.TwitterException
import twitter4j.TwitterFactory
import twitter4j.auth.OAuthAuthorization
import twitter4j.conf.ConfigurationBuilder
import twitter4j.conf.ConfigurationContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.inject.Inject

//<div>Icons made by <a href="https://www.flaticon.com/<?=_('authors/')?>freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
class Tweet : AppCompatActivity(), View.OnClickListener {
    private var mAdView: AdView? = null
    @Inject
    lateinit var spTwitterToken: SharedPreferences
    private lateinit var binding: TweetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        (application as MyApplication).appComponent.inject(this)//Dagger2

        super.onCreate(savedInstanceState)

        //(application as MyApplication).appComponent.inject(this)//Dagger2

        binding = TweetBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val adView = SetAdView(binding.adView, applicationContext)
        adView.initAd()
        setupButtons()
        val isUserAlreadyLoggedIn = spTwitterToken.getBoolean("login", false)
        Log.d(localClassName, "isUserAlreadyLoggedIn = $isUserAlreadyLoggedIn")

        showAndHideLayouts(isUserAlreadyLoggedIn)

        //hyperlink
        binding.tvPrivacyPolicy.movementMethod = LinkMovementMethod.getInstance()
        imagesPathList = ArrayList()
        LoginInfo.mOauth = OAuthAuthorization(ConfigurationContext.getInstance())
        twitterKeysAndTokens
        binding.etTweet.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(
                enteredText: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
                val textColor: Int
                val length = 280 - TextCounter().getTextLength(enteredText)
                if (length < 0) {
                    textColor = Color.RED
                    binding.btnSendTweet.isEnabled = false
                    binding.btnSendTweet.setTextColor(Color.GRAY)
                } else {
                    textColor = Color.GRAY
                    binding.btnSendTweet.isEnabled = true
                    binding.btnSendTweet.setTextColor(Color.WHITE)
                }
                binding.tvTweetTextCount.setTextColor(textColor)
                binding.tvTweetTextCount.text = length.toString()
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    private fun setupButtons() {
        binding.btnSendTweet.setOnClickListener(this)
        binding.btnLogin.setOnClickListener(this)
        binding.btnLogOut.setOnClickListener(this)
        binding.btnClear.setOnClickListener(this)
        binding.btnUploadPhotoVideo.setOnClickListener(this)
    }

    /** Called when leaving the activity  */
    public override fun onPause() {
        if (mAdView != null) {
            mAdView!!.pause()
        }
        super.onPause()
    }

    /** Called when returning to the activity  */
    public override fun onResume() {
        super.onResume()
        if (mAdView != null) {
            mAdView!!.resume()
        }
    }

    /** Called before the activity is destroyed  */
    public override fun onDestroy() {
        if (mAdView != null) {
            mAdView!!.destroy()
        }
        super.onDestroy()
    }

    //Retrieve token after the login (for the first time)
    private val twitterKeysAndTokens: Unit
        get() {
            //Retrieve token after the login (for the first time)
            if (intent.getStringExtra("token") != null) {
                saveTokenIntoSharedPreferences()
            } else if (spTwitterToken.contains("token")) {
                oAuthAccessToken = spTwitterToken.getString("token", "")
                oAuthAccessTokenSecret = spTwitterToken.getString("tokenSecret", "")
            } else {
                oAuthAccessToken = null
                oAuthAccessTokenSecret = null
            }
        }

    private fun saveTokenIntoSharedPreferences() {
        editorTwitterToken = spTwitterToken.edit()
        oAuthAccessToken = intent.getStringExtra("token")
        editorTwitterToken.putString("token", oAuthAccessToken)
        oAuthAccessTokenSecret = intent.getStringExtra("tokenSecret")
        editorTwitterToken.putString("tokenSecret", oAuthAccessTokenSecret)
        editorTwitterToken.apply()
    }

    private fun checkIfILoggedIn() {
        editorTwitterToken = spTwitterToken.edit()
        editorTwitterToken.putBoolean("login", true)
        editorTwitterToken.apply()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnLogin -> {
                checkIfILoggedIn()
                login()
            }
            R.id.btnSendTweet -> tweet()
            R.id.btnLogOut -> {
                logout()
                //Need to review. Look at "Enregistreur harcèlement"
                val redirect = Intent(this@Tweet, Tweet::class.java)
                startActivity(redirect)
            }
            R.id.btnClear -> clearOutEtTweet()
            R.id.btnUploadPhotoVideo -> {
                requestTwoPermissions()
                //ShowMediaOptions smo = new ShowMediaOptions(getApplicationContext());
                val smo = ShowMediaOptions(this@Tweet)
                smo.showOptionMediaDialog()
            }
        }
    }

    private fun getPathFromUri(context: Context, uri: Uri?): String? {
        //boolean isAfterKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        val TAG = "getPathFromUri"
        Log.e(TAG, "uri:" + uri!!.authority)
        if (DocumentsContract.isDocumentUri(context, uri)) {
            when (uri.authority) {
                "com.android.externalstorage.documents" -> { // ExternalStorageProvider
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    return if ("primary".equals(type, ignoreCase = true)) {
                        Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    } else {
                        "/stroage/" + type + "/" + split[1]
                    }
                }
                "com.android.providers.downloads.documents" -> { // DownloadsProvider
                    val id = DocumentsContract.getDocumentId(uri)
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), id.toLong()
                    )
                    return getDataColumn(context, contentUri, null, null)
                }
                "com.android.providers.media.documents" -> { // MediaProvider
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    //val type = split[0]
                    val contentUri: Uri? = MediaStore.Files.getContentUri("external")
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(
                        split[1]
                    )
                    return getDataColumn(context, contentUri, selection, selectionArgs)
                }
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) { //MediaStore
            return getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) { // File
            return uri.path
        }
        return null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            try {
                if (requestCode == Code.RESULT_LOAD_IMAGE) {
                    if (null != data) {
                        var imagePath: String?
                        if (data.data != null) {
                            //When an image is picked
                            val mImageUri = data.data
                            imagePath = getPathFromUri(this, mImageUri)
                            sizePhotoCheck(imagePath)
                        } else {
                            //When multiple images are picked
                            if (data.clipData != null) {
                                println("++data: " + data.clipData!!.itemCount) // Get count of image here.
                                for (i in 0 until data.clipData!!.itemCount) {
                                    val selectedImage = data.clipData!!.getItemAt(i).uri
                                    //String type list which contains file path of each selected image file
                                    imagePath = getPathFromUri(this@Tweet, selectedImage)
                                    sizePhotoCheck(imagePath)
                                    println("selectedImage: $selectedImage")
                                }
                            }
                        }
                        println("data.getData(): " + data.data)
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.not_picked_images),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else if (requestCode == Code.RESULT_LOAD_VIDEO) {
                    val selectedImageUri = data!!.data

                    // MEDIA GALLERY
                    selectedVideoPath = getPathFromUri(this, selectedImageUri)
                    sizeVideoCheck(selectedVideoPath)
                } else if (requestCode == Code.REQUEST_VIDEO_CAPTURE) {
                    val newVideoUri = data!!.data

                    // MEDIA GALLERY
                    selectedVideoPath = getPathFromUri(this, newVideoUri)
                    sizeVideoCheck(selectedVideoPath)
                } else if (requestCode == Code.REQUEST_TAKE_PHOTO) {
                    if (MediaOptions.cameraFile != null) {
                        //registerDatabase(cameraFile);
                        println("cameraFile: " + MediaOptions.cameraFile)
                        println("cameraFile.getAbsolutePath(): " + MediaOptions.cameraFile!!.absolutePath)
                        println("mCapturedImageURI: " + MediaOptions.mCapturedImageURI)
                        println("mCapturedImageURI.getAuthority(): " + (MediaOptions.mCapturedImageURI?.authority))
                        val imagePath = MediaOptions.cameraFile!!.absolutePath
                        if (MediaOptions.cameraFile!!.length() <= 5000000) {
                            imagesPathList!!.add(imagePath)
                        } else {
                            imagesPathList!!.clear()
                            Toast.makeText(
                                this,
                                getString(R.string.size_too_large),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.fail_photo), Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, getString(R.string.attach_fail), Toast.LENGTH_SHORT).show()
            }
        }
    } //END onActivityResult()

    private fun sizePhotoCheck(filePath: String?) {
        val imageFile = File(filePath)

        // Image size <= 5 MB (https://developer.twitter.com/en/docs/media/upload-media/uploading-media/media-best-practices)
        if (imageFile.length() <= 5000000) {
            imagesPathList!!.add(filePath)
        } else {
            imagesPathList!!.clear()
            Toast.makeText(this, getString(R.string.size_too_large), Toast.LENGTH_LONG).show()
        }
    }

    private fun sizeVideoCheck(filePath: String?) {
        val fileToCheck = File(filePath)

        //Video file size must not exceed 512 MB (https://developer.twitter.com/en/docs/media/upload-media/uploading-media/media-best-practices)
        if (fileToCheck.length() > 512000000) {
            selectedVideoPath = ""
        }
    }

    // Runtime Permission check
    private fun requestTwoPermissions() {
        // If at least one of two permissions isn't yet granted
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@Tweet, arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                ), Code.PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Code.PERMISSION_REQUEST_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                //Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_SHORT).show();
                Toast.makeText(this, getString(R.string.need_permission), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun notOverLetterLimit(): Boolean {
        val tweetDraft = binding.etTweet.text.toString()
        val numTweetLetters = tweetDraft.length
        return numTweetLetters < 141
    }

    private fun logout() {
        val TAG = "logout()"
        Log.d(TAG, "--------------- START ---------------")
        editorTwitterToken = spTwitterToken.edit()
        editorTwitterToken.putBoolean("login", false)
        editorTwitterToken.putString("token", null)
        editorTwitterToken.putString("tokenSecret", null)
        editorTwitterToken.apply()
        Toast.makeText(this, getString(R.string.logout_success), Toast.LENGTH_SHORT).show()
        Log.d(TAG, "--------------- END ---------------")
    }

    private fun login() {
        //LoginTwitter().execute()
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch { loginTwitter() }
    }

    private suspend fun loginTwitter()
    {
        try {
            // onPreExecuteと同等の処理
            withContext(Dispatchers.Main) {
                Log.d(localClassName, "始めます")
            }

            // doInBackgroundメソッドと同等の処理
            Thread.sleep(800)
            //withContext(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                Log.d(localClassName, "Logging in Twitter")
                LoginInfo.mOauth?.setOAuthConsumer(
                    LoginInfo.oAuthConsumerKey,
                    LoginInfo.oAuthConsumerSecret
                )
                LoginInfo.mOauth?.oAuthAccessToken = null

                try {
                    LoginInfo.mRequest =
                        LoginInfo.mOauth?.getOAuthRequestToken("callback://ReceiveToken") //ReceiveToken will receive user's token for login.
                    val uri = Uri.parse(LoginInfo.mRequest?.authenticationURL)
                    val login = Intent(Intent.ACTION_VIEW, uri)
                    startActivityForResult(login, 0) //Implicit intent to log in on web browser
                } catch (e: TwitterException) {
                    e.printStackTrace()
                }
            }
            Thread.sleep(800)

            // onPostExecuteメソッドと同等の処理
            withContext(Dispatchers.Main) {
                Log.d(localClassName, "終わります")
            }
        } catch (e: Exception) {
            // onCancelledメソッドと同等の処理
            Log.e(localClassName, "ここにキャンセル時の処理を記述", e)
        }
    }

    private fun clearOutEtTweet() {
        binding.etTweet.setText("")
    }

    private fun tweet() {
        try {
            //SendTweet(this@Tweet, etTweet).execute(etTweet.text.toString())
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch { sendTweet() } // 止めたいときは以下のように scope.coroutineContext.cancelChildren()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun sendTweet() {
        try {
            val TAG = "SendTweet"
            var isVideoTooLarge = false
            var uploadedMoreThan4Images = false
            var didIUploadNothing = false
            var statusCode = 0
            var errorCode = 0

            // onPreExecuteと同等の処理
            withContext(Dispatchers.Main) {
                Log.d(localClassName, "始めます")
                binding.progressBar.visibility = View.VISIBLE
            }

            // doInBackgroundメソッドと同等の処理
            Thread.sleep(800)

            try {
                val cb = setTwitterKeysAndTokens()
                val twitter = TwitterFactory(cb.build()).instance

                //set text
                val strTweet = binding.etTweet.text.toString()
                val status = StatusUpdate(strTweet)

                //set video
                if (selectedVideoPath != null) {
                    try {
                        // https://ja.stackoverflow.com/questions/28169/android%E3%81%8B%E3%82%89-twitter4j-%E3%82%92%E4%BD%BF%E7%94%A8%E3%81%97%E3%81%A6%E5%8B%95%E7%94%BB%E3%82%92%E6%8A%95%E7%A8%BF%E3%82%92%E3%81%97%E3%81%9F%E3%81%84
                        var inputStream: FileInputStream?
                        //String path = Environment.getExternalStorageDirectory().toString() + "/video.mp4";
                        val file = File(selectedVideoPath!!)
                        inputStream = FileInputStream(file)
                        val video = twitter.uploadMediaChunked("video.mp4", inputStream)
                        //https://github.com/Twitter4J/Twitter4J/issues/339
                        status.setMediaIds(video.mediaId)
                        println("Uploading a video...")
                        inputStream.close()
                    } catch (e: OutOfMemoryError) {
                        e.printStackTrace()
                        isVideoTooLarge = true
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else if (imagesPathList!!.size > 4) {
                    uploadedMoreThan4Images = true
                } else if (imagesPathList!!.size >= 1) {
                    println("Uploading image(s)...")
                    //upload multiple image files (4 files at most)
                    val mediaIds = LongArray(imagesPathList!!.size)
                    for (i in mediaIds.indices) {
                        println("imagesPathList.get(i): " + imagesPathList!![i])
                        val image = File(imagesPathList!![i])
                        println("image.length(): " + image.length())
                        val media = twitter.uploadMedia(image)
                        println("media.getImageType(): " + media.imageType + " media.getSize(): " + media.size)
                        mediaIds[i] = media.mediaId
                    }
                    status.setMediaIds(*mediaIds)
                } else {
                    println("Uploading nothing...")
                    didIUploadNothing = true
                    status.setMedia(null)
                }

                //send tweet
                if (uploadedMoreThan4Images) {
                    println("You cannot upload more than 4 images.")
                } else if (isVideoTooLarge) {
                    println("The video you uploaded is too large! Less than 27 seconds video should be uploaded without problem...")
                } else {
                    println("status=$status")
                    twitter.updateStatus(status)
                    statusCode = 200
                    Log.d("TWEET", "The tweet was sent as expected...")
                }
            } catch (te: TwitterException) {
                te.printStackTrace()
                Log.d(TAG, te.toString())
                println("te.getStatusCode(): " + te.statusCode)
                println("te.getMessage(): " + te.message)
                println("te.getErrorCode(): " + te.errorCode)
                println("te.getErrorMessage(): " + te.errorMessage)
                statusCode = te.statusCode
                errorCode = te.errorCode
            }

            flushOutUploadedImageVideo()
//            for (i in 1..10) {
//                //onProgressUpdateメソッドと同等の処理
//                withContext(Dispatchers.Main) {
//                    Log.d(localClassName, "ここでUIの更新")
//                }
//                Thread.sleep(800)
//            }
            Thread.sleep(800)

            // onPostExecuteメソッドと同等の処理
            withContext(Dispatchers.Main) {
                Log.d(localClassName, "終わります")
                binding.progressBar.visibility = View.GONE

                //Handling error
                if (statusCode == 200)
                {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.tweet_sent_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    clearOutEtTweet()
                }
                else if (statusCode == 503)
                {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.twitter_unavailable),
                        Toast.LENGTH_LONG
                    ).show()
                }
                else if (statusCode == 403)
                {
                    when (errorCode)
                    {
                        170 -> Toast.makeText(
                            applicationContext,
                            getString(R.string.no_text_to_tweet),
                            Toast.LENGTH_SHORT
                        ).show()

                        193 -> Toast.makeText(
                            applicationContext,
                            getString(R.string.media_is_too_large),
                            Toast.LENGTH_LONG
                        ).show()

                        -1 -> Toast.makeText(
                            applicationContext,
                            getString(R.string.unknown_error),
                            Toast.LENGTH_LONG
                        ).show()

                        else -> {}
                    }
                }
                else if (statusCode == 400)
                {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.request_invalid),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else if (uploadedMoreThan4Images)
                {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.four_images_at_most),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else
                {
                    Toast.makeText(applicationContext, getString(R.string.unknown_error), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        catch (e: Exception)
        {
            // onCancelledメソッドと同等の処理
            Log.e(localClassName, "ここにキャンセル時の処理を記述", e)
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun flushOutUploadedImageVideo() {
        imagesPathList!!.clear()
        selectedVideoPath = null
    }

    private fun setTwitterKeysAndTokens(): ConfigurationBuilder
    {
        val cb = ConfigurationBuilder()
        cb.setDebugEnabled(true)
            .setOAuthConsumerKey(LoginInfo.oAuthConsumerKey)
            .setOAuthConsumerSecret(LoginInfo.oAuthConsumerSecret)
            .setOAuthAccessToken(Tweet.oAuthAccessToken)
            .setOAuthAccessTokenSecret(Tweet.oAuthAccessTokenSecret)
        return cb
    }

    companion object {
        //@JvmField
        var oAuthAccessToken: String? = null
        //@JvmField
        var oAuthAccessTokenSecret: String? = null
        lateinit var editorTwitterToken: SharedPreferences.Editor
        //@JvmField
        var imagesPathList: ArrayList<String?>? = null
        //@JvmField
        var selectedVideoPath: String? = null
        fun getDataColumn(
            context: Context, uri: Uri?, selection: String?,
            selectionArgs: Array<String>?
        ): String? {
            var cursor: Cursor? = null
            val projection = arrayOf(
                MediaStore.Files.FileColumns.DATA
            )
            try {
                cursor = context.contentResolver.query(
                    uri!!, projection, selection, selectionArgs, null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    val cindex = cursor.getColumnIndexOrThrow(projection[0])
                    println("cursor.getString(cindex): " + cursor.getString(cindex))
                    return cursor.getString(cindex)
                }
            } finally {
                cursor?.close()
            }
            return null
        }
    }

    private fun showAndHideLayouts(isUserLoggedIn: Boolean)
    {
        if (isUserLoggedIn)
        {
            binding.btnLogin.visibility = View.INVISIBLE
            binding.btnSendTweet.visibility = View.VISIBLE
            binding.btnLogOut.visibility = View.VISIBLE
            binding.btnClear.visibility = View.VISIBLE
            binding.btnUploadPhotoVideo.visibility = View.VISIBLE
            binding.textInputLayout.visibility = View.VISIBLE
        }
        else
        {
            binding.btnLogin.visibility = View.VISIBLE
            binding.btnSendTweet.visibility = View.INVISIBLE
            binding.btnLogOut.visibility = View.INVISIBLE
            binding.btnClear.visibility = View.INVISIBLE
            binding.btnUploadPhotoVideo.visibility = View.INVISIBLE
            binding.textInputLayout.visibility = View.INVISIBLE
        }
    }
}