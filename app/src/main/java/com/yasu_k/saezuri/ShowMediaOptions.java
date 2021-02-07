package com.yasu_k.saezuri;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.yasu_k.saezuri.Code.PERMISSION_REQUEST_CODE;
import static com.yasu_k.saezuri.Code.REQUEST_TAKE_PHOTO;
import static com.yasu_k.saezuri.Code.REQUEST_VIDEO_CAPTURE;
import static com.yasu_k.saezuri.Code.RESULT_LOAD_IMAGE;
import static com.yasu_k.saezuri.Code.RESULT_LOAD_VIDEO;
import static com.yasu_k.saezuri.MediaOptions.CAPTURE_A_VIDEO;
import static com.yasu_k.saezuri.MediaOptions.SELECT_A_VIDEO;
import static com.yasu_k.saezuri.MediaOptions.SELECT_IMAGES;
import static com.yasu_k.saezuri.MediaOptions.TAKE_A_PHOTO;
import static com.yasu_k.saezuri.MediaOptions.cameraFile;
import static com.yasu_k.saezuri.MediaOptions.mCapturedImageURI;
import static com.yasu_k.saezuri.MediaOptions.mediaOptions;

public class ShowMediaOptions {
    Context mContext;

    public ShowMediaOptions(Context context){
        this.mContext = context;
    }

    // After clicking "Yes" on the dialog, you can have a permission request
    public void showInfoDialog(String whichPermission){
        if (whichPermission.equals("camera")){
            new AlertDialog.Builder(mContext)
                    .setMessage("To attach images or video on your tweet, press \"OK\" to get the permissions.")
                    .setPositiveButton(
                            "OK",
                            (dialog, which) -> ActivityCompat.requestPermissions((Activity)mContext, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE)
                    )
                    .setNegativeButton(
                            "NO",
                            (DialogInterface.OnClickListener) (dialogInterface, i) -> Toast.makeText((Activity)mContext, mContext.getString(R.string.warning_no_permission),
                                    Toast.LENGTH_LONG).show()
                    )
                    .show();
        }
        else if (whichPermission.equals("read"))
        {
            new AlertDialog.Builder(mContext)
                    .setMessage("To attach images or video on your tweet, press \"OK\" to get the permissions.")
                    .setPositiveButton(
                            "OK",
                            (dialog, which) -> ActivityCompat.requestPermissions((Activity)mContext, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE)
                    )
                    .setNegativeButton(
                            "NO",
                            (DialogInterface.OnClickListener) (dialogInterface, i) -> Toast.makeText((Activity)mContext, mContext.getString(R.string.warning_no_permission),
                                    Toast.LENGTH_LONG).show()
                    )
                    .show();
        }

    }

    // Runtime Permission check
    private boolean checkPermissionToTakePhoto(){

        int checkPermissionCamera = ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);
        String whichPermission = "camera";

        if (checkPermissionCamera == PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        // Permission denied
        else
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity)mContext, Manifest.permission.CAMERA)){
                // User can get the permission via this dialog
                showInfoDialog(whichPermission);
            }else{
                // User cannot send an email anymore unless getting the permission manually in "App setting"
                ActivityCompat.requestPermissions((Activity)mContext, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
            }
            return false;
        }
    }

    private boolean checkPermissionToReadStorage() {

        int result = ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE);
        String whichPermission = "read";

        if (result == PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        // Permission denied
        else
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity)mContext, Manifest.permission.READ_EXTERNAL_STORAGE)){
                // User can get the permission via this dialog
                showInfoDialog(whichPermission);
            }else{
                // User cannot send an email anymore unless getting the permission manually in "App setting"
                ActivityCompat.requestPermissions((Activity)mContext, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
            return false;
        }
    }

    public void showOptionMediaDialog(){
        new MaterialAlertDialogBuilder(mContext)
                .setItems(mediaOptions, (dialogInterface, whichOption) -> {
                    switch (whichOption){
                        case SELECT_IMAGES:
                            if (checkPermissionToReadStorage()){
                                uploadPhotos();
                            }
                            break;
                        case SELECT_A_VIDEO:
                            if (checkPermissionToReadStorage()){
                                uploadVideo();
                            }
                            break;
                        case TAKE_A_PHOTO:
                            if (checkPermissionToTakePhoto()){
                                takeOnePhoto();
                            }
                            break;
                        case CAPTURE_A_VIDEO:
                            if (checkPermissionToTakePhoto()){
                                captureOneVideo();
                            }
                            break;
                    }
                })
                .show();
    }

    private void uploadVideo(){
        Intent videoPickerIntent = new Intent();
        videoPickerIntent.setType("video/*");
//        videoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
        videoPickerIntent.setAction(Intent.ACTION_PICK);
        ((Activity) mContext).startActivityForResult(Intent.createChooser(videoPickerIntent, "Select Video"), RESULT_LOAD_VIDEO);
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
        ((Activity) mContext).startActivityForResult(Intent.createChooser(photoPickerIntent, "Select Image"), RESULT_LOAD_IMAGE);
    }

    private void takeOnePhoto(){
        // Determine a folder to save the captured image
        File cFolder = mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);//DIRECTORY_DCIM

        String fileDate = new SimpleDateFormat(
                "ddHHmmss", Locale.getDefault()).format(new Date());

        String fileName = String.format("CameraIntent_%s.jpg", fileDate);

        cameraFile = new File(cFolder, fileName);

        // This is not very useful so far...
        mCapturedImageURI = FileProvider.getUriForFile(
                (Activity) mContext,
                mContext.getPackageName() + ".fileprovider",
                cameraFile);

        Intent takeOnePhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takeOnePhoto.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

        if (takeOnePhoto.resolveActivity(mContext.getPackageManager()) != null){
            ((Activity) mContext).startActivityForResult(takeOnePhoto, REQUEST_TAKE_PHOTO);
        }
    }

    private void captureOneVideo(){
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        if (takeVideoIntent.resolveActivity(mContext.getPackageManager()) != null) {
            ((Activity) mContext).startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }
}