package com.yasu_k.saezuri

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yasu_k.saezuri.Code.PERMISSION_REQUEST_CODE
import com.yasu_k.saezuri.Code.REQUEST_TAKE_PHOTO
import com.yasu_k.saezuri.Code.REQUEST_VIDEO_CAPTURE
import com.yasu_k.saezuri.Code.RESULT_LOAD_IMAGE
import com.yasu_k.saezuri.Code.RESULT_LOAD_VIDEO
import com.yasu_k.saezuri.MediaOptions.CAPTURE_A_VIDEO
import com.yasu_k.saezuri.MediaOptions.SELECT_A_VIDEO
import com.yasu_k.saezuri.MediaOptions.SELECT_IMAGES
import com.yasu_k.saezuri.MediaOptions.TAKE_A_PHOTO
import com.yasu_k.saezuri.MediaOptions.cameraFile
import com.yasu_k.saezuri.MediaOptions.mCapturedImageURI
import com.yasu_k.saezuri.MediaOptions.mediaOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ShowMediaOptions(var mContext: Context) {
    // After clicking "Yes" on the dialog, you can have a permission request
    fun showInfoDialog(whichPermission: String) {
        if (whichPermission == "camera") {
            AlertDialog.Builder(mContext)
                .setMessage("To attach images or video on your tweet, press \"OK\" to get the permissions.")
                .setPositiveButton(
                    "OK"
                ) { dialog: DialogInterface?, which: Int ->
                    ActivityCompat.requestPermissions(
                        (mContext as Activity),
                        arrayOf(Manifest.permission.CAMERA),
                        PERMISSION_REQUEST_CODE
                    )
                }
                .setNegativeButton(
                    "NO"
                ) { dialogInterface: DialogInterface?, i: Int ->
                    Toast.makeText(
                        mContext as Activity,
                        mContext.getString(R.string.warning_no_permission),
                        Toast.LENGTH_LONG
                    ).show()
                }
                .show()
        } else if (whichPermission == "read") {
            AlertDialog.Builder(mContext)
                .setMessage("To attach images or video on your tweet, press \"OK\" to get the permissions.")
                .setPositiveButton(
                    "OK"
                ) { dialog: DialogInterface?, which: Int ->
                    ActivityCompat.requestPermissions(
                        (mContext as Activity),
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        PERMISSION_REQUEST_CODE
                    )
                }
                .setNegativeButton(
                    "NO"
                ) { dialogInterface: DialogInterface?, i: Int ->
                    Toast.makeText(
                        mContext as Activity,
                        mContext.getString(R.string.warning_no_permission),
                        Toast.LENGTH_LONG
                    ).show()
                }
                .show()
        }
    }

    // Runtime Permission check
    private fun checkPermissionToTakePhoto(): Boolean {
        val checkPermissionCamera =
            ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)
        val whichPermission = "camera"
        return if (checkPermissionCamera == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    (mContext as Activity),
                    Manifest.permission.CAMERA
                )
            ) {
                // User can get the permission via this dialog
                showInfoDialog(whichPermission)
            } else {
                // User cannot send an email anymore unless getting the permission manually in "App setting"
                ActivityCompat.requestPermissions(
                    (mContext as Activity),
                    arrayOf(Manifest.permission.CAMERA),
                    PERMISSION_REQUEST_CODE
                )
            }
            false
        }
    }

    private fun checkPermissionToReadStorage(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE)
        val whichPermission = "read"
        return if (result == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    (mContext as Activity),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                // User can get the permission via this dialog
                showInfoDialog(whichPermission)
            } else {
                // User cannot send an email anymore unless getting the permission manually in "App setting"
                ActivityCompat.requestPermissions(
                    (mContext as Activity),
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            }
            false
        }
    }

    fun showOptionMediaDialog() {
        MaterialAlertDialogBuilder(mContext)
            .setItems(
                mediaOptions
            ) { dialogInterface: DialogInterface?, whichOption: Int ->
                when (whichOption) {
                    SELECT_IMAGES -> if (checkPermissionToReadStorage()) {
                        uploadPhotos()
                    }
                    SELECT_A_VIDEO -> if (checkPermissionToReadStorage()) {
                        uploadVideo()
                    }
                    TAKE_A_PHOTO -> if (checkPermissionToTakePhoto()) {
                        takeOnePhoto()
                    }
                    CAPTURE_A_VIDEO -> if (checkPermissionToTakePhoto()) {
                        captureOneVideo()
                    }
                }
            }
            .show()
    }

    private fun uploadVideo() {
        val videoPickerIntent = Intent()
        videoPickerIntent.type = "video/*"
        //        videoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
        videoPickerIntent.action = Intent.ACTION_PICK
        (mContext as Activity).startActivityForResult(
            Intent.createChooser(
                videoPickerIntent,
                "Select Video"
            ), RESULT_LOAD_VIDEO
        )
    }

    private fun uploadPhotos() {
        val photoPickerIntent = Intent()
        photoPickerIntent.type = "image/*"
        photoPickerIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
        val mimeTypes = arrayOf("image/jpeg", "image/png")
        photoPickerIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)

        //photoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
        photoPickerIntent.action = Intent.ACTION_PICK
        (mContext as Activity).startActivityForResult(
            Intent.createChooser(
                photoPickerIntent,
                "Select Image"
            ), RESULT_LOAD_IMAGE
        )
    }

    private fun takeOnePhoto() {
        // Determine a folder to save the captured image
        val cFolder = mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) //DIRECTORY_DCIM
        val fileDate = SimpleDateFormat(
            "ddHHmmss", Locale.getDefault()
        ).format(Date())
        val fileName = String.format("CameraIntent_%s.jpg", fileDate)
        cameraFile = File(cFolder, fileName)

        // This is not very useful so far...
        mCapturedImageURI = FileProvider.getUriForFile(
            (mContext as Activity),
            mContext.packageName + ".fileprovider",
            cameraFile!!
        )
        val takeOnePhoto = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takeOnePhoto.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI)
        if (takeOnePhoto.resolveActivity(mContext.packageManager) != null) {
            (mContext as Activity).startActivityForResult(takeOnePhoto, REQUEST_TAKE_PHOTO)
        }
    }

    private fun captureOneVideo() {
        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if (takeVideoIntent.resolveActivity(mContext.packageManager) != null) {
            (mContext as Activity).startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
        }
    }
}