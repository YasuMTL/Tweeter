package com.yasu_k.saezuri.data.source

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContentProviderCompat.requireContext
import com.yasu_k.saezuri.Code
import com.yasu_k.saezuri.LoginInfo
import com.yasu_k.saezuri.MediaOptions
import com.yasu_k.saezuri.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import twitter4j.TwitterException
import twitter4j.conf.ConfigurationBuilder
import javax.inject.Inject

@Inject
lateinit var spTwitterToken: SharedPreferences

class ReceiveTokenRepository {
    init {
        val isUserAlreadyLoggedIn = spTwitterToken.getBoolean("login", false)
        Log.d(javaClass.name, "isUserAlreadyLoggedIn = $isUserAlreadyLoggedIn")
    }

    companion object {
        //@JvmField
        var oAuthAccessToken: String? = null
        //@JvmField
        var oAuthAccessTokenSecret: String? = null
        lateinit var editorTwitterToken: SharedPreferences.Editor
    }

    fun logout() {
        val TAG = "logout()"
        Log.d(TAG, "--------------- START ---------------")
        editorTwitterToken = spTwitterToken.edit()
        editorTwitterToken.putBoolean("login", false)
        editorTwitterToken.putString("token", null)
        editorTwitterToken.putString("tokenSecret", null)
        editorTwitterToken.apply()
        //Toast.makeText(this, getString(R.string.logout_success), Toast.LENGTH_SHORT).show()
        Log.d(TAG, "--------------- END ---------------")
    }

    fun login() {
        CoroutineScope(Dispatchers.Default).launch {
            loginTwitter()
        }
    }

    private suspend fun loginTwitter()
    {
        try {
            // onPreExecuteと同等の処理
            withContext(Dispatchers.Main) {
                Log.d(javaClass.name, "始めます")
            }

            // doInBackgroundメソッドと同等の処理
            Thread.sleep(800)
            //withContext(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                Log.d(javaClass.name, "Logging in Twitter")
                LoginInfo.mOauth?.setOAuthConsumer(
                    LoginInfo.oAuthConsumerKey,
                    LoginInfo.oAuthConsumerSecret
                )
                LoginInfo.mOauth?.oAuthAccessToken = null

                try {
                    LoginInfo.mRequest =
                        LoginInfo.mOauth?.getOAuthRequestToken("callback://ReceiveToken") //ReceiveToken will receive user's token for login.

                    val uri = Uri.parse(LoginInfo.mRequest?.authenticationURL)

                    val login = Intent(Intent.ACTION_VIEW, uri) as Activity

                    //TODO
                    //1. Use Intent
                    //2. Use WebView
                    startActivityForResult(login, 0) //Implicit intent to log in on web browser
                } catch (e: TwitterException) {
                    e.printStackTrace()
                }
            }
            Thread.sleep(800)

            // onPostExecuteメソッドと同等の処理
            withContext(Dispatchers.Main) {
                Log.d(javaClass.name, "終わります")
            }
        } catch (e: Exception) {
            // onCancelledメソッドと同等の処理
            Log.e(javaClass.name, "ここにキャンセル時の処理を記述", e)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK) {
            try {
                if (requestCode == Code.RESULT_LOAD_IMAGE) {
                    if (null != data) {
                        var imagePath: String?
                        if (data.data != null) {
                            //When an image is picked
                            val mImageUri = data.data
                            imagePath = getPathFromUri(requireContext(), mImageUri)
                            sizePhotoCheck(imagePath)
                        } else {
                            //When multiple images are picked
                            if (data.clipData != null) {
                                println("++data: " + data.clipData!!.itemCount) // Get count of image here.
                                for (i in 0 until data.clipData!!.itemCount) {
                                    val selectedImage = data.clipData!!.getItemAt(i).uri
                                    //String type list which contains file path of each selected image file
                                    imagePath = getPathFromUri(requireContext(), selectedImage)
                                    sizePhotoCheck(imagePath)
                                    println("selectedImage: $selectedImage")
                                }
                            }
                        }
                        println("data.getData(): " + data.data)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.not_picked_images),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else if (requestCode == Code.RESULT_LOAD_VIDEO) {
                    val selectedImageUri = data!!.data

                    // MEDIA GALLERY
                    TweetRepository.selectedVideoPath = getPathFromUri(requireContext(), selectedImageUri)
                    sizeVideoCheck(TweetRepository.selectedVideoPath)
                } else if (requestCode == Code.REQUEST_VIDEO_CAPTURE) {
                    val newVideoUri = data!!.data

                    // MEDIA GALLERY
                    TweetRepository.selectedVideoPath = getPathFromUri(requireContext(), newVideoUri)
                    sizeVideoCheck(TweetRepository.selectedVideoPath)
                } else if (requestCode == Code.REQUEST_TAKE_PHOTO) {
                    if (MediaOptions.cameraFile != null) {
                        //registerDatabase(cameraFile);
                        println("cameraFile: " + MediaOptions.cameraFile)
                        println("cameraFile.getAbsolutePath(): " + MediaOptions.cameraFile!!.absolutePath)
                        println("mCapturedImageURI: " + MediaOptions.mCapturedImageURI)
                        println("mCapturedImageURI.getAuthority(): " + (MediaOptions.mCapturedImageURI?.authority))
                        val imagePath = MediaOptions.cameraFile!!.absolutePath
                        if (MediaOptions.cameraFile!!.length() <= 5000000) {
                            TweetRepository.imagesPathList!!.add(imagePath)
                        } else {
                            TweetRepository.imagesPathList!!.clear()
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.size_too_large),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.fail_photo), Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), getString(R.string.attach_fail), Toast.LENGTH_SHORT).show()
            }
        }
    } //END onActivityResult()

    fun setTwitterKeysAndTokens(): ConfigurationBuilder
    {
        val cb = ConfigurationBuilder()
        cb.setDebugEnabled(true)
            .setOAuthConsumerKey(LoginInfo.oAuthConsumerKey)
            .setOAuthConsumerSecret(LoginInfo.oAuthConsumerSecret)
            .setOAuthAccessToken(oAuthAccessToken)
            .setOAuthAccessTokenSecret(oAuthAccessTokenSecret)
        return cb
    }
}