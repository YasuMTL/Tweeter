package com.yasu_k.saezuri.data.source

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
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

//    private fun requestTwoPermissions(context: Context) {
//        // If at least one of two permissions isn't yet granted
//        if (ActivityCompat.checkSelfPermission(
//                ContentProviderCompat.requireContext(),
//                Manifest.permission.READ_EXTERNAL_STORAGE
//            ) != PackageManager.PERMISSION_GRANTED ||
//            ActivityCompat.checkSelfPermission(
//                ContentProviderCompat.requireContext(),
//                Manifest.permission.CAMERA
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                context, arrayOf(
//                    Manifest.permission.READ_EXTERNAL_STORAGE,
//                    Manifest.permission.CAMERA
//                ), Code.PERMISSION_REQUEST_CODE
//            )
//        }
//    }

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
                    return getDataColumn(
                        context,
                        contentUri,
                        selection,
                        selectionArgs
                    )
                }
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) { //MediaStore
            return getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) { // File
            return uri.path
        }
        return null
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

//    private fun notOverLetterLimit(): Boolean {
//        val tweetDraft = binding.etTweet.text.toString()
//        val numTweetLetters = tweetDraft.length
//        return numTweetLetters < 141
//    }

//    private fun takeOnePhoto(takenPhotoUri: Uri) {
//        // Determine a folder to save the captured image
//        val cFolder = mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) //DIRECTORY_DCIM
//        val fileDate = SimpleDateFormat(
//            "ddHHmmss", Locale.getDefault()
//        ).format(Date())
//        val fileName = String.format("CameraIntent_%s.jpg", fileDate)
//        cameraFile = File(cFolder, fileName)
//
//        // This is not very useful so far...
//        mCapturedImageURI = FileProvider.getUriForFile(
//            (mContext as Activity),
//            mContext.packageName + ".fileprovider",
//            cameraFile!!
//        )
//
//        //"ACTION_IMAGE_CAPTURE" with not granted CAMERA permission will result in SecurityException
//        val takeOnePhoto = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        takeOnePhoto.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI)
//        val resolveActivity = takeOnePhoto.resolveActivity(mContext.packageManager)
//        //if (resolveActivity != null) {
//            (mContext as Activity).startActivityForResult(takeOnePhoto, REQUEST_TAKE_PHOTO)
//        //}
        //takeImageResult.launch("image/*")
        //(mContext as Fragment).

//        takeImageResult.launch(takenPhotoUri)
//        Log.i(javaClass.name, "takeOnePhoto has been executed! : takenPhotoUri = $takenPhotoUri")
//    }

    suspend fun sendTweet(textTweet: String,
                      configurationBuilder: ConfigurationBuilder,
                      contentResolver: ContentResolver
    ): Int {
        var statusCode = 0

        try {
            val TAG = "SendTweet"
            var isVideoTooLarge = false
            var uploadedMoreThan4Images = false
            var didIUploadNothing = false
            //var statusCode = 0
            var errorCode = 0

            // onPreExecuteと同等の処理
            withContext(Dispatchers.Main) {
                Log.d(javaClass.name, "始めます")
                //binding.progressBar.visibility = View.VISIBLE
            }

            // doInBackgroundメソッドと同等の処理
            //Thread.sleep(800)
//            sizePhotoCheck(takenPhotoUri.path!!)
//            //sizePhotoCheck(takenPhotoUri.toString())
//            println("takenPhotoUri was checked before attaching it to the tweet")
//            println("takenPhotoUri = $takenPhotoUri")
//            println("takenPhotoUri.path = ${takenPhotoUri.path}")

            checkImagePathListState()

            try {
                val twitter = TwitterFactory(configurationBuilder.build()).instance
                val status = StatusUpdate(textTweet)

                //set video
                if (selectedVideoPath.isNotEmpty()) {
                    try {
                        // https://ja.stackoverflow.com/questions/28169/android%E3%81%8B%E3%82%89-twitter4j-%E3%82%92%E4%BD%BF%E7%94%A8%E3%81%97%E3%81%A6%E5%8B%95%E7%94%BB%E3%82%92%E6%8A%95%E7%A8%BF%E3%82%92%E3%81%97%E3%81%9F%E3%81%84
                        //val inputStream: FileInputStream?
                        //String path = Environment.getExternalStorageDirectory().toString() + "/video.mp4";
                        //val uriFile = imagesPathList[i].toUri()
                        val uriVideoFile = selectedVideoPath.toUri()
                        println("uriVideoFile = $uriVideoFile")
                        //TODO Decide what to do if the file is empty

                        val inputStream = contentResolver.openInputStream(uriVideoFile)
                            ?: throw Exception("inputStream is empty")
                        //todo: Need to check the size of input stream

//                        val file = File(selectedVideoPath)
//                        inputStream = FileInputStream(file)
                        val video = twitter.uploadMediaChunked("video.mp4", inputStream)
                        //https://github.com/Twitter4J/Twitter4J/issues/339
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
//                        val image = File(imagesPathList!![i])
//                        println("image.length(): " + image.length())
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