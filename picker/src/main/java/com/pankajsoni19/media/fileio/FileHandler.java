package com.pankajsoni19.media.fileio;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Environment;
import androidx.annotation.StringRes;
import android.util.Log;

import com.pankajsoni19.media.enums.MediaType;
import com.pankajsoni19.media.picker.MediaPickerOpts;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Pankaj Soni <pankajsoni19@live.com> on 28/02/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
@SuppressWarnings("WeakerAccess")
public class FileHandler {

    private static final String TAG = "FileHandler";

    public static String getApplicationName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        @StringRes int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
    }

    public static File getTempFile(MediaPickerOpts opts) {
        File dir = getPublicAlbumStorageDir(opts.mediaDir, opts.mediaType);
        if (opts.mediaType == MediaType.VIDEO) {
            return new File(dir, "VID_" + getDateTimeString() + ".mp4");
        } else if (opts.cropEnabled) {
            return new File(dir, "IMG_" + getDateTimeString() + ".png");
        } else {
            return new File(dir, "IMG_" + getDateTimeString() + ".jpg");
        }
    }

    private static File getPublicAlbumStorageDir(String albumName, @MediaType int mediaType) {
        String type = mediaType == MediaType.VIDEO ? Environment.DIRECTORY_MOVIES : Environment.DIRECTORY_PICTURES;

        File file = new File(Environment.getExternalStoragePublicDirectory(type), albumName);
        //noinspection ResultOfMethodCallIgnored
        if (!file.exists() && !file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }

    private static String getDateTimeString() {
        final SimpleDateFormat mDateTimeFormat =
                new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US);
        return mDateTimeFormat.format(new Date());
    }

    public static boolean exists(String mediaPath) {
        File file = new File(mediaPath);
        return file.exists();
    }
}