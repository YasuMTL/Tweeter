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
import twitter4j.TwitterException
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import java.io.File
import java.io.IOException

class TweetRepository {

    companion object {
        val imagesPathList = arrayListOf<String>()
        //@JvmField
        var selectedVideoPath = ""
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

    fun setUri(uri: Uri){
        if (uri.toString().contains("image", true)) {
            imagesPathList.add(uri.toString())
            println("Image URI is set!")
        } else {
            selectedVideoPath = uri.toString()
            println("Video URI is set!")
        }
    }

    suspend fun sendTweet(textTweet: String,
                      configurationBuilder: ConfigurationBuilder,
                      contentResolver: ContentResolver
    ): Int {
        var statusCode = 0

        try {
            val TAG = "SendTweet"
            var isVideoTooLarge = false
            var uploadedMoreThan4Images = false
            var errorCode = 0

            // onPreExecuteと同等の処理
            withContext(Dispatchers.Main) {
                Log.d(javaClass.name, "始めます")
            }

            checkImagePathListState()

            try {
                val twitter = TwitterFactory(configurationBuilder.build()).instance
                val status = StatusUpdate(textTweet)

                //set video
                if (selectedVideoPath.isNotEmpty()) {
                    try {
                        // https://ja.stackoverflow.com/questions/28169/android%E3%81%8B%E3%82%89-twitter4j-%E3%82%92%E4%BD%BF%E7%94%A8%E3%81%97%E3%81%A6%E5%8B%95%E7%94%BB%E3%82%92%E6%8A%95%E7%A8%BF%E3%82%92%E3%81%97%E3%81%9F%E3%81%84
                        val uriVideoFile = selectedVideoPath.toUri()
                        println("uriVideoFile = $uriVideoFile")
                        //TODO Decide what to do if the file is empty

                        val inputStream = contentResolver.openInputStream(uriVideoFile)
                            ?: throw Exception("inputStream is empty")

                        val video = twitter.uploadMediaChunked("video.mp4", inputStream)
                        status.setMediaIds(video.mediaId)
                        println("Uploading a video...")
                        withContext(Dispatchers.IO) {
                            inputStream.close()
                        }
                    } catch (e: OutOfMemoryError) {
                        e.printStackTrace()
                        isVideoTooLarge = true
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else if (imagesPathList.size > 4) {
                    uploadedMoreThan4Images = true
                } else if (imagesPathList.size >= 1) {
                    println("Uploading image(s)...")
                    //upload multiple image files (4 files at most)
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

                        withContext(Dispatchers.IO) {
                            inputStream.close()
                            println("Closing inputStream...")
                        }
                    }
                    status.setMediaIds(*mediaIds)
                } else {
                    println("Uploading nothing...")
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
            //Thread.sleep(800)

            // onPostExecuteメソッドと同等の処理
            withContext(Dispatchers.Main) {
                Log.d(javaClass.name, "終わります")
            }
        }
        catch (e: Exception)
        {
            // onCancelledメソッドと同等の処理
            Log.e(javaClass.name, "ここにキャンセル時の処理を記述", e)
            //binding.progressBar.visibility = View.GONE
        }

        return statusCode
    }

    private fun flushOutUploadedImageVideo() {
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