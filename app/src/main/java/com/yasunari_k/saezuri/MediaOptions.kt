package com.yasunari_k.saezuri

import android.net.Uri
import java.io.File

object MediaOptions {
    const val SELECT_IMAGES = 0
    const val SELECT_A_VIDEO = 1
    const val TAKE_A_PHOTO = 2
    const val CAPTURE_A_VIDEO = 3
    @JvmField
    val mediaOptions =
        arrayOf("Select image(s)", "Select a video", "Capture a photo", "Capture a video")
    @JvmField
    var mCapturedImageURI: Uri? = null
    @JvmField
    var cameraFile: File? = null
}