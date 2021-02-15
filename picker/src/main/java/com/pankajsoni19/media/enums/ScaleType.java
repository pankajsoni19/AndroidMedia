package com.pankajsoni19.media.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.pankajsoni19.media.enums.ScaleType.SCALE_CROP_CENTER;
import static com.pankajsoni19.media.enums.ScaleType.SCALE_KEEP_ASPECT;
import static com.pankajsoni19.media.enums.ScaleType.SCALE_KEEP_ASPECT_VIEWPORT;
import static com.pankajsoni19.media.enums.ScaleType.SCALE_SQUARE;
import static com.pankajsoni19.media.enums.ScaleType.SCALE_STRETCH_FIT;

/**
 * Created by Pankaj Soni <pankajsoni19@live.com> on 01/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
@Retention(RetentionPolicy.CLASS)
@IntDef({SCALE_STRETCH_FIT, SCALE_KEEP_ASPECT_VIEWPORT, SCALE_KEEP_ASPECT,
        SCALE_CROP_CENTER, SCALE_SQUARE})
public @interface ScaleType {
    int SCALE_STRETCH_FIT = 0;
    int SCALE_KEEP_ASPECT_VIEWPORT = 1;
    int SCALE_KEEP_ASPECT = 2;
    int SCALE_CROP_CENTER = 3;
    int SCALE_SQUARE = 4;
}