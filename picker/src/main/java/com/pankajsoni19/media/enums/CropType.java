package com.pankajsoni19.media.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.pankajsoni19.media.enums.CropType.CIRCLE;
import static com.pankajsoni19.media.enums.CropType.FLOWER;
import static com.pankajsoni19.media.enums.CropType.PATH;
import static com.pankajsoni19.media.enums.CropType.NONE;
import static com.pankajsoni19.media.enums.CropType.STAR;

/**
 * Created by Pankaj Soni <pankajsoni19@live.com> on 15/03/18.
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