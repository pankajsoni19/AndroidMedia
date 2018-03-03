package com.softwarejoint.media.enums;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.softwarejoint.media.enums.MediaType.IMAGE;
import static com.softwarejoint.media.enums.MediaType.VIDEO;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 03/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
@Retention(RetentionPolicy.CLASS)
@IntDef({VIDEO, IMAGE})
public @interface MediaType {
    int VIDEO = 0;
    int IMAGE = 1;
}