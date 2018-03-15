package com.softwarejoint.media.enums;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.softwarejoint.media.enums.CropType.CIRCLE;
import static com.softwarejoint.media.enums.CropType.FLOWER;
import static com.softwarejoint.media.enums.CropType.PATH;
import static com.softwarejoint.media.enums.CropType.NONE;
import static com.softwarejoint.media.enums.CropType.STAR;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 15/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef({NONE, CIRCLE, STAR, FLOWER, PATH})
public @interface CropType {
    int NONE = 0;
    int CIRCLE = 1;
    int STAR = 2;
    int FLOWER = 3;
    int PATH = 4;
}