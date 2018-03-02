package com.serenegiant.fileio;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Environment;
import android.support.annotation.StringRes;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 28/02/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
@SuppressWarnings("WeakerAccess")
public class FileHandler {

    private static final String TAG = "FileHandler";

    private static String getApplicationName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        @StringRes int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
    }

    public static File getTempFile(Context context) {
        final String appName = getApplicationName(context);
        return getTempFile(appName);
    }

    public static File getTempFile(String albumName) {
        File dir = getPublicAlbumStorageDir(albumName);
        final String filename = "VID_" + getDateTimeString() + ".mp4";
        return new File(dir, filename);
    }

    private static File getPublicAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), albumName);
        //noinspection ResultOfMethodCallIgnored
        if (!file.exists() && !file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }

    private static String getDateTimeString() {
        final SimpleDateFormat mDateTimeFormat =
                new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
        return mDateTimeFormat.format(new Date());
    }
}
