package com.yasu_k.saezuri.data.source

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import com.yasu_k.saezuri.R
import com.yasu_k.saezuri.data.source.ReceiveTokenRepository.Companion.editorTwitterToken
import com.yasu_k.saezuri.data.source.ReceiveTokenRepository.Companion.oAuthAccessToken
import com.yasu_k.saezuri.data.source.ReceiveTokenRepository.Companion.oAuthAccessTokenSecret
import com.yasu_k.saezuri.data.source.TweetRepository.Companion.getDataColumn
import com.yasu_k.saezuri.data.source.TweetRepository.Companion.imagesPathList
import com.yasu_k.saezuri.data.source.TweetRepository.Companion.selectedVideoPath
import com.yasu_k.saezuri.ui.TweetFragment
import com.yasu_k.saezuri.ui.binding
import com.yasu_k.saezuri.ui.spTwitterToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import twitter4j.StatusUpdate
import twitter4j.TwitterException
import twitter4j.TwitterFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class TweetRepository {

    companion object {
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

    fun sendTweet() {

    }

    //Retrieve token after the login (for the first time)
    private val twitterKeysAndTokens: Unit
        get() {
            //Retrieve token after the login (for the first time)
            if (intent.getStringExtra("token") != null) {
                saveTokenIntoSharedPreferences()
            } else if (spTwitterToken.contains("token")) {
                TweetFragment.oAuthAccessToken = spTwitterToken.getString("token", "")
                TweetFragment.oAuthAccessTokenSecret = spTwitterToken.getString("tokenSecret", "")
            } else {
                TweetFragment.oAuthAccessToken = null
                TweetFragment.oAuthAccessTokenSecret = null
            }
        }

    private fun saveTokenIntoSharedPreferences() {
        TweetFragment.editorTwitterToken = spTwitterToken.edit()
        TweetFragment.oAuthAccessToken = intent.getStringExtra("token")
        TweetFragment.editorTwitterToken.putString("token", TweetFragment.oAuthAccessToken)
        TweetFragment.oAuthAccessTokenSecret = intent.getStringExtra("tokenSecret")
        TweetFragment.editorTwitterToken.putString("tokenSecret",
            TweetFragment.oAuthAccessTokenSecret
        )
        TweetFragment.editorTwitterToken.apply()
    }

    private fun checkIfILoggedIn() {
        TweetFragment.editorTwitterToken = spTwitterToken.edit()
        TweetFragment.editorTwitterToken.putBoolean("login", true)
        TweetFragment.editorTwitterToken.apply()
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
                    return TweetFragment.getDataColumn(context, contentUri, null, null)
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
                    return TweetFragment.getDataColumn(
                        context,
                        contentUri,
                        selection,
                        selectionArgs
                    )
                }
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) { //MediaStore
            return TweetFragment.getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) { // File
            return uri.path
        }
        return null
    }

    private fun sizePhotoCheck(filePath: String?) {
        val imageFile = File(filePath)

        // Image size <= 5 MB (https://developer.twitter.com/en/docs/media/upload-media/uploading-media/media-best-practices)
        if (imageFile.length() <= 5000000) {
            TweetFragment.imagesPathList!!.add(filePath)
        } else {
            TweetFragment.imagesPathList!!.clear()
            Toast.makeText(requireContext(), getString(R.string.size_too_large), Toast.LENGTH_LONG).show()
        }
    }

    private fun sizeVideoCheck(filePath: String?) {
        val fileToCheck = File(filePath)

        //Video file size must not exceed 512 MB (https://developer.twitter.com/en/docs/media/upload-media/uploading-media/media-best-practices)
        if (fileToCheck.length() > 512000000) {
            TweetFragment.selectedVideoPath = ""
        }
    }

    private fun notOverLetterLimit(): Boolean {
        val tweetDraft = binding.etTweet.text.toString()
        val numTweetLetters = tweetDraft.length
        return numTweetLetters < 141
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
                if (TweetFragment.selectedVideoPath != null) {
                    try {
                        // https://ja.stackoverflow.com/questions/28169/android%E3%81%8B%E3%82%89-twitter4j-%E3%82%92%E4%BD%BF%E7%94%A8%E3%81%97%E3%81%A6%E5%8B%95%E7%94%BB%E3%82%92%E6%8A%95%E7%A8%BF%E3%82%92%E3%81%97%E3%81%9F%E3%81%84
                        var inputStream: FileInputStream?
                        //String path = Environment.getExternalStorageDirectory().toString() + "/video.mp4";
                        val file = File(TweetFragment.selectedVideoPath!!)
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
                } else if (TweetFragment.imagesPathList!!.size > 4) {
                    uploadedMoreThan4Images = true
                } else if (TweetFragment.imagesPathList!!.size >= 1) {
                    println("Uploading image(s)...")
                    //upload multiple image files (4 files at most)
                    val mediaIds = LongArray(TweetFragment.imagesPathList!!.size)
                    for (i in mediaIds.indices) {
                        println("imagesPathList.get(i): " + TweetFragment.imagesPathList!![i])
                        val image = File(TweetFragment.imagesPathList!![i])
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
        TweetFragment.imagesPathList!!.clear()
        TweetFragment.selectedVideoPath = null
    }
}