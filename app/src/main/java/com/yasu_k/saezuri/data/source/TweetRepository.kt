package com.yasu_k.saezuri.data.source

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import twitter4j.StatusUpdate
import twitter4j.Twitter
import twitter4j.TwitterException
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import java.io.File
import java.io.IOException
import java.io.InputStream

class TweetRepository {

    companion object {
        val imagesPathList = arrayListOf<String>()
        //@JvmField
        var selectedVideoPath = ""
        var storedTwitterException: TwitterException? = null
    }

    init {
        imagesPathList.clear()
    }

    private lateinit var takenPhotoUri: Uri
    private lateinit var takenVideoUri: Uri

    private fun checkImagePathListState(){
        val size = imagesPathList.size
        println("TweetRepository")
        println("imagesPathList.size = $size")
        println("imagesPathList = $imagesPathList")
    }

    fun takeOnePhoto(context: Context, launcher: ActivityResultLauncher<Uri>){
        takenPhotoUri = ImageFileHelper.getImageUri(context = context)
        launcher.launch(takenPhotoUri)

        sizePhotoCheck(takenPhotoUri.toString())
    }

    fun takeOneVideo(context: Context, launcher: ActivityResultLauncher<Uri>){
        takenVideoUri = ImageFileHelper.getVideoUri(context = context)
        launcher.launch(takenVideoUri)

        sizeVideoCheck(takenVideoUri.toString())
    }

    private fun sizePhotoCheck(filePath: String) {
        println("sizePhotoCheck() is called")
        println("filePath = $filePath")

        checkImagePathListState()
        val imageFile = File(filePath)

        // Image size <= 5 MB (https://developer.twitter.com/en/docs/media/upload-media/uploading-media/media-best-practices)
        if (imageFile.length() <= 5000000) {
            imagesPathList.add(filePath)
            println("Image is small enough to attach to the tweet")
            checkImagePathListState()
        } else {
            imagesPathList.clear()
            //Toast.makeText(requireContext(), getString(R.string.size_too_large), Toast.LENGTH_LONG).show()
            println("Image is too large to attach to the tweet")
        }
        println("sizePhotoCheck() comes to an end")
    }

    private fun sizeVideoCheck(filePath: String) {
        val fileToCheck = File(filePath)

        //Video file size must not exceed 512 MB (https://developer.twitter.com/en/docs/media/upload-media/uploading-media/media-best-practices)
        if (fileToCheck.length() > 512000000) {
            selectedVideoPath = ""
        } else {
            selectedVideoPath = filePath
            println("selectedVideoPath = $filePath")
        }
    }

    fun setUri(chosenURIs: MutableList<Uri>){
        chosenURIs.forEach { uri ->
            if (uri.toString().contains("image", true)) {
                imagesPathList.add(uri.toString())
                println("Image URI is set!")
            } else if (uri.toString().contains("jpg", true)) {
                imagesPathList.add(uri.toString())
                println("Image URI is set!")
            } else {
                selectedVideoPath = uri.toString()
                println("Video URI is set!")
            }
        }
    }

    /**
     * Upload media file(s), send the tweet and return a status code
     *
     * @param textTweet the message to tweet
     * @param configurationBuilder to instantiate a Twitter object
     * @param contentResolver to pick up media file(s)
     * @return a status code
     * @since 2020
     */
    suspend fun sendTweet(textTweet: String,
                      configurationBuilder: ConfigurationBuilder,
                      contentResolver: ContentResolver
    ): Int {
        var statusCode = 0
        storedTwitterException = null

        try {
            val TAG = "SendTweet"

            // onPreExecuteと同等の処理
            withContext(Dispatchers.Main) {
                Log.d(javaClass.name, "始めます")
            }

            try {
                val twitter = TwitterFactory(configurationBuilder.build()).instance
                val status = StatusUpdate(textTweet)

                val isSuccess: Boolean = try {
                    uploadMediaFiles(twitter, status, contentResolver)
                } catch (e: Exception) {
                    println(e.message)
                    Log.d(TAG, e.message.toString())
                    false
                }

                if (isSuccess) {
                    twitter.updateStatus(status)
                    statusCode = 200
                    Log.d(TAG, "The tweet was sent as expected...")
                }
            } catch (te: TwitterException) {
                storedTwitterException = te
                te.printStackTrace()
                Log.d(TAG, te.toString())
                println("te.getStatusCode(): " + te.statusCode)
                println("te.getMessage(): " + te.message)
                println("te.getErrorCode(): " + te.errorCode)
                println("te.getErrorMessage(): " + te.errorMessage)
                statusCode = te.statusCode
            } catch (e: Exception) {
                Log.d(TAG, e.message.toString())
                e.printStackTrace()
                statusCode = 400
            }

            flushOutUploadedImageVideo()

            // onPostExecuteメソッドと同等の処理
            withContext(Dispatchers.Main) {
                Log.d(javaClass.name, "終わります")
            }
        }
        catch (e: Exception)
        {
            // onCancelledメソッドと同等の処理
            Log.e(javaClass.name, "ここにキャンセル時の処理を記述", e)
        }

        return statusCode
    }

    fun getStoredTwitterException(): TwitterException? = storedTwitterException

    private fun uploadMediaFiles(
        twitter: Twitter,
        statusUpdate: StatusUpdate,
        contentResolver: ContentResolver
    ): Boolean {
        if (selectedVideoPath.isNotEmpty()) {
            return uploadOneVideo(twitter, statusUpdate, contentResolver)
        } else if (imagesPathList.size > 4) {
            return false
        } else if (imagesPathList.size >= 1) {
            return uploadPhotos(twitter, statusUpdate, contentResolver)
        }

        println("Uploading nothing...")
        statusUpdate.setMedia(null)
        return true
    }

    private fun uploadPhotos(
        twitter: Twitter,
        statusUpdate: StatusUpdate,
        contentResolver: ContentResolver
    ): Boolean {
        println("Uploading image(s)...")
        val mediaIds = LongArray(imagesPathList.size)
        for (i in mediaIds.indices) {
            println("imagesPathList.get(i): " + imagesPathList[i])
            val uriPhotoFile = imagesPathList[i].toUri()
            println("uriPhotoFile = $uriPhotoFile")
            //TODO Decide what to do if the file is empty

            val inputStream = contentResolver.openInputStream(uriPhotoFile)
                ?: continue//Should use break?
            //todo: Need to check the size of input stream

            val fileName = "image_$i"
            val media = twitter.uploadMedia(fileName, inputStream)
            println("media.getImageType(): " + media.imageType + " media.getSize(): " + media.size)
            mediaIds[i] = media.mediaId

            inputStream.close()
            println("Closing inputStream...")
        }

        statusUpdate.setMediaIds(*mediaIds)
        return true
    }

    private fun uploadOneVideo(
        twitter: Twitter,
        statusUpdate: StatusUpdate,
        contentResolver: ContentResolver
    ): Boolean {
        var inputStream: InputStream? = null

        try {
            // https://ja.stackoverflow.com/questions/28169/android%E3%81%8B%E3%82%89-twitter4j-%E3%82%92%E4%BD%BF%E7%94%A8%E3%81%97%E3%81%A6%E5%8B%95%E7%94%BB%E3%82%92%E6%8A%95%E7%A8%BF%E3%82%92%E3%81%97%E3%81%9F%E3%81%84
            val uriVideoFile = selectedVideoPath.toUri()
            println("uriVideoFile = $uriVideoFile")

            inputStream = contentResolver.openInputStream(uriVideoFile)
                ?: throw Exception("inputStream is empty")

            val video = twitter.uploadMediaChunked("video.mp4", inputStream)
            statusUpdate.setMediaIds(video.mediaId)
            println("Uploading a video...")
            //inputStream.close()
            return true
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            //isVideoTooLarge = true
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }

        return false
    }

    fun flushOutUploadedImageVideo() {
        imagesPathList.clear()
        selectedVideoPath = ""
    }
}

object ImageFileHelper {
    fun getImageUri(context: Context): Uri {
        val directory = File(context.cacheDir, "images")

        directory.mkdirs()
        val file = File.createTempFile(
            "captured_image_",
            ".jpg",
            directory,
        )
        val authority = context.packageName + ".fileprovider"
        return FileProvider.getUriForFile(
            context,
            authority,
            file,
        )
    }

    fun getVideoUri(context: Context): Uri {
        val directory = File(context.cacheDir, "video")

        directory.mkdirs()
        val file = File.createTempFile(
            "captured_video_",
            ".mp4",
            directory,
        )
        val authority = context.packageName + ".fileprovider"
        return FileProvider.getUriForFile(
            context,
            authority,
            file,
        )
    }
}