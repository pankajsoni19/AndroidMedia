package com.pankajsoni19.media.utils;

import android.util.Log;

import com.pankajsoni19.media.BuildConfig;

/**
 * Created by Pankaj Soni <pankajsoni19@live.com> on 01/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
@SuppressWarnings("WeakerAccess")
public class Logger {

    public static boolean logEnabled = BuildConfig.DEBUG;

    public static void debug(String TAG, String message) {
        if (BuildConfig.DEBUG && logEnabled) {
            Log.d(TAG, message);
        }
    }
}
