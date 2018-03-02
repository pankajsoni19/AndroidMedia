package com.serenegiant.enums;

import android.support.annotation.IntDef;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.serenegiant.enums.ScaleType.SCALE_CROP_CENTER;
import static com.serenegiant.enums.ScaleType.SCALE_KEEP_ASPECT;
import static com.serenegiant.enums.ScaleType.SCALE_KEEP_ASPECT_VIEWPORT;
import static com.serenegiant.enums.ScaleType.SCALE_SQUARE;
import static com.serenegiant.enums.ScaleType.SCALE_STRETCH_FIT;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 01/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef({SCALE_STRETCH_FIT, SCALE_KEEP_ASPECT_VIEWPORT, SCALE_KEEP_ASPECT,
        SCALE_CROP_CENTER, SCALE_SQUARE})
public @interface ScaleType {
    int SCALE_STRETCH_FIT = 0;
    int SCALE_KEEP_ASPECT_VIEWPORT = 1;
    int SCALE_KEEP_ASPECT = 2;
    int SCALE_CROP_CENTER = 3;
    int SCALE_SQUARE = 4;
}
