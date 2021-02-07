package com.yasu_k.saezuri;

import android.net.Uri;
import java.io.File;

public final class MediaOptions {
    private MediaOptions(){}

    static public final int SELECT_IMAGES = 0;
    static public final int SELECT_A_VIDEO = 1;
    static public final int TAKE_A_PHOTO = 2;
    static public final int CAPTURE_A_VIDEO = 3;
    static public final String[] mediaOptions
            = {"Select image(s)", "Select a video", "Capture a photo", "Capture a video"};

    static public Uri mCapturedImageURI;
    static public File cameraFile;
}
