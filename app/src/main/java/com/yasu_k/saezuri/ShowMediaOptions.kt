package com.yasu_k.saezuri

import android.content.Context
import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yasu_k.saezuri.MediaOptions.CAPTURE_A_VIDEO
import com.yasu_k.saezuri.MediaOptions.SELECT_A_VIDEO
import com.yasu_k.saezuri.MediaOptions.SELECT_IMAGES
import com.yasu_k.saezuri.MediaOptions.TAKE_A_PHOTO
import com.yasu_k.saezuri.MediaOptions.mediaOptions


//
//interface OnDialogueItemsClickListener {
//    fun onOptionClick(whichOption: Option)
//}
//
//class ShowMediaOptions(private var onDialogueItemsClickListener: OnDialogueItemsClickListener? = null) {
//
//    private var chosenOption = Option.NOT_CHOSEN
//
//    fun showOptionMediaDialog(context: Context) {
////        val response =
//        MaterialAlertDialogBuilder(context)
//            .setItems(
//                mediaOptions
//            ) { dialogInterface: DialogInterface?, whichOption: Int ->
//                chosenOption = when (whichOption) {
//                    SELECT_IMAGES -> Option.SELECT_IMAGES
//                    SELECT_A_VIDEO -> Option.SELECT_ONE_VIDEO
//                    TAKE_A_PHOTO -> Option.TAKE_ONE_PHOTO
//                    CAPTURE_A_VIDEO -> Option.CAPTURE_ONE_VIDEO
//                    else -> Option.NOT_CHOSEN
//                }
//            }
//            .show()
//    }
//
//    fun setOnItemsClickListener(onDialogueItemsClickListener: OnDialogueItemsClickListener) {
//        this.onDialogueItemsClickListener = onDialogueItemsClickListener
//        println("Performing some task, prior to invoking the callback")
//
//        if (this.onDialogueItemsClickListener != null) {
//            onDialogueItemsClickListener.onOptionClick(whichOption = chosenOption)
//        }
//    }
//}

//class ShowMediaOptions(var mContext: Context, private val takeImageResult: ActivityResultLauncher<Uri>) {
//
//    // After clicking "Yes" on the dialog, you can have a permission request
//    fun showInfoDialog(whichPermission: String) {
//        if (whichPermission == "camera") {
//            AlertDialog.Builder(mContext)
//                .setMessage("To attach images or video on your tweet, press \"OK\" to get the permissions.")
//                .setPositiveButton(
//                    "OK"
//                ) { dialog: DialogInterface?, which: Int ->
//                    ActivityCompat.requestPermissions(
//                        (mContext as Activity),
//                        arrayOf(Manifest.permission.CAMERA),
//                        PERMISSION_REQUEST_CODE
//                    )
//                }
//                .setNegativeButton(
//                    "NO"
//                ) { dialogInterface: DialogInterface?, i: Int ->
//                    Toast.makeText(
//                        mContext as Activity,
//                        mContext.getString(R.string.warning_no_permission),
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//                .show()
//        } else if (whichPermission == "read") {
//            AlertDialog.Builder(mContext)
//                .setMessage("To attach images or video on your tweet, press \"OK\" to get the permissions.")
//                .setPositiveButton(
//                    "OK"
//                ) { dialog: DialogInterface?, which: Int ->
//                    ActivityCompat.requestPermissions(
//                        (mContext as Activity),
//                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
//                        PERMISSION_REQUEST_CODE
//                    )
//                }
//                .setNegativeButton(
//                    "NO"
//                ) { dialogInterface: DialogInterface?, i: Int ->
//                    Toast.makeText(
//                        mContext as Activity,
//                        mContext.getString(R.string.warning_no_permission),
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//                .show()
//        }
//    }
//
//    // Runtime Permission check
//    private fun checkPermissionToTakePhoto(): Boolean {
//        val checkPermissionCamera =
//            ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)
//        val whichPermission = "camera"
//        return if (checkPermissionCamera == PackageManager.PERMISSION_GRANTED) {
//            true
//        } else {
//            if (ActivityCompat.shouldShowRequestPermissionRationale(
//                    (mContext as Activity),
//                    Manifest.permission.CAMERA
//                )
//            ) {
//                // User can get the permission via this dialog
//                showInfoDialog(whichPermission)
//            } else {
//                // User cannot send an email anymore unless getting the permission manually in "App setting"
//                ActivityCompat.requestPermissions(
//                    (mContext as Activity),
//                    arrayOf(Manifest.permission.CAMERA),
//                    PERMISSION_REQUEST_CODE
//                )
//            }
//            false
//        }
//    }
//
//    private fun checkPermissionToReadStorage(): Boolean {
//        val result =
//            ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE)
//        val whichPermission = "read"
//        return if (result == PackageManager.PERMISSION_GRANTED) {
//            true
//        } else {
//            if (ActivityCompat.shouldShowRequestPermissionRationale(
//                    (mContext as Activity),
//                    Manifest.permission.READ_EXTERNAL_STORAGE
//                )
//            ) {
//                // User can get the permission via this dialog
//                showInfoDialog(whichPermission)
//            } else {
//                // User cannot send an email anymore unless getting the permission manually in "App setting"
//                ActivityCompat.requestPermissions(
//                    (mContext as Activity),
//                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
//                    PERMISSION_REQUEST_CODE
//                )
//            }
//            false
//        }
//    }
//
//    fun showOptionMediaDialog() {
//        MaterialAlertDialogBuilder(mContext)
//            .setItems(
//                mediaOptions
//            ) { dialogInterface: DialogInterface?, whichOption: Int ->
//                when (whichOption) {
//                    SELECT_IMAGES -> if (checkPermissionToReadStorage()) {
//                        uploadPhotos()
//                    }
//                    SELECT_A_VIDEO -> if (checkPermissionToReadStorage()) {
//                        uploadVideo()
//                    }
//
//                    TAKE_A_PHOTO -> takeOnePhoto()
//
//                    CAPTURE_A_VIDEO -> if (checkPermissionToTakePhoto()) {
//                        captureOneVideo()
//                    }
//                }
//            }
//            .show()
//    }
//
//    private fun uploadVideo() {
//        val videoPickerIntent = Intent()
//        videoPickerIntent.type = "video/*"
//        //        videoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
//        videoPickerIntent.action = Intent.ACTION_PICK
//        (mContext as Activity).startActivityForResult(
//            Intent.createChooser(
//                videoPickerIntent,
//                "Select Video"
//            ), RESULT_LOAD_VIDEO
//        )
//    }
//
//    private fun uploadPhotos() {
//        val photoPickerIntent = Intent()
//        photoPickerIntent.type = "image/*"
//        photoPickerIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//
//        //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
//        val mimeTypes = arrayOf("image/jpeg", "image/png")
//        photoPickerIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
//
//        //photoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
//        photoPickerIntent.action = Intent.ACTION_PICK
//        (mContext as Activity).startActivityForResult(
//            Intent.createChooser(
//                photoPickerIntent,
//                "Select Image"
//            ), RESULT_LOAD_IMAGE
//        )
//    }
//
//    private fun takeOnePhoto(takenPhotoUri: Uri) {
//        // Determine a folder to save the captured image
////        val cFolder = mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) //DIRECTORY_DCIM
////        val fileDate = SimpleDateFormat(
////            "ddHHmmss", Locale.getDefault()
////        ).format(Date())
////        val fileName = String.format("CameraIntent_%s.jpg", fileDate)
////        cameraFile = File(cFolder, fileName)
////
////        // This is not very useful so far...
////        mCapturedImageURI = FileProvider.getUriForFile(
////            (mContext as Activity),
////            mContext.packageName + ".fileprovider",
////            cameraFile!!
////        )
////
////        //"ACTION_IMAGE_CAPTURE" with not granted CAMERA permission will result in SecurityException
////        val takeOnePhoto = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
////        takeOnePhoto.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI)
////        val resolveActivity = takeOnePhoto.resolveActivity(mContext.packageManager)
////        //if (resolveActivity != null) {
////            (mContext as Activity).startActivityForResult(takeOnePhoto, REQUEST_TAKE_PHOTO)
////        //}
//        //takeImageResult.launch("image/*")
//        //(mContext as Fragment).
//
//
//        takeImageResult.launch(takenPhotoUri)
//        Log.i(javaClass.name, "takeOnePhoto has been executed! : takenPhotoUri = $takenPhotoUri")
//    }
//
//    private fun captureOneVideo() {
//        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
//        if (takeVideoIntent.resolveActivity(mContext.packageManager) != null) {
//            (mContext as Activity).startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
//        }
//    }
//}