package com.pankajsoni19.media.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.pankajsoni19.media.enums.MediaType.IMAGE;
import static com.pankajsoni19.media.enums.MediaType.VIDEO;

/**
 * Created by Pankaj Soni <pankajsoni19@live.com> on 03/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
@Retention(RetentionPolicy.CLASS)
@IntDef({VIDEO, IMAGE})
public @interface MediaType {
    int VIDEO = 1;
    int IMAGE = 2;
}