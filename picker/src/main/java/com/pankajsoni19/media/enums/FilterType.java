package com.pankajsoni19.media.enums;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.pankajsoni19.media.enums.FilterType.ART;
import static com.pankajsoni19.media.enums.FilterType.BLOOM;
import static com.pankajsoni19.media.enums.FilterType.COLOR_INVERT;
import static com.pankajsoni19.media.enums.FilterType.F1977;
import static com.pankajsoni19.media.enums.FilterType.GRAY_SCALE;
import static com.pankajsoni19.media.enums.FilterType.NONE;
import static com.pankajsoni19.media.enums.FilterType.POSTERIZE;

/**
 * Created by Pankaj Soni <pankajsoni19@live.com> on 02/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
@Retention(RetentionPolicy.SOURCE)
@StringDef({NONE, F1977, ART, COLOR_INVERT, GRAY_SCALE, POSTERIZE, BLOOM})
public @interface FilterType {
    String NONE = "none";
    String F1977 = "F1977";
    String ART = "art";
    String COLOR_INVERT = "colorInvert";
    String GRAY_SCALE = "grayscale";
    String POSTERIZE = "posterize";
    String BLOOM = "bloom";
}