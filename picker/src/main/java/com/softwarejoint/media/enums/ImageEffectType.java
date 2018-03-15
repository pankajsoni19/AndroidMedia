package com.softwarejoint.media.enums;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.softwarejoint.media.enums.ImageEffectType.BLACK_WHITE;
import static com.softwarejoint.media.enums.ImageEffectType.NONE;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 15/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef({NONE, BLACK_WHITE})
public @interface ImageEffectType {
    int NONE = 0;
    int BLACK_WHITE = 1;
}
