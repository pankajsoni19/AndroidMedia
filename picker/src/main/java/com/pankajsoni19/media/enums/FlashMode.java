package com.pankajsoni19.media.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.pankajsoni19.media.enums.FlashMode.AUTO;
import static com.pankajsoni19.media.enums.FlashMode.OFF;
import static com.pankajsoni19.media.enums.FlashMode.ON;
import static com.pankajsoni19.media.enums.FlashMode.TORCH;
import static com.pankajsoni19.media.enums.FlashMode.UNAVAILABLE;

/**
 * Created by Pankaj Soni <pankajsoni19@live.com> on 01/03/18.
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
