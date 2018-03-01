package com.serenegiant.enums;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.serenegiant.enums.FlashMode.AUTO;
import static com.serenegiant.enums.FlashMode.OFF;
import static com.serenegiant.enums.FlashMode.ON;
import static com.serenegiant.enums.FlashMode.TORCH;
import static com.serenegiant.enums.FlashMode.UNAVAILABLE;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 01/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef({UNAVAILABLE, OFF, TORCH, ON, AUTO})
public @interface FlashMode {
    int UNAVAILABLE = 0;
    int OFF = 1;
    int TORCH = 2;
    int ON = 3;
    int AUTO = 4;
}
