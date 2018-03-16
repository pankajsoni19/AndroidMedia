package com.softwarejoint.media.enums;

import android.media.effect.EffectFactory;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.softwarejoint.media.enums.ImageEffectType.EFFECT_BLACKWHITE;
import static com.softwarejoint.media.enums.ImageEffectType.EFFECT_DOCUMENTARY;
import static com.softwarejoint.media.enums.ImageEffectType.EFFECT_GRAYSCALE;
import static com.softwarejoint.media.enums.ImageEffectType.EFFECT_LOMOISH;
import static com.softwarejoint.media.enums.ImageEffectType.EFFECT_NEGATIVE;
import static com.softwarejoint.media.enums.ImageEffectType.EFFECT_POSTERIZE;
import static com.softwarejoint.media.enums.ImageEffectType.EFFECT_SEPIA;
import static com.softwarejoint.media.enums.ImageEffectType.EFFECT_VIGNETTE;
import static com.softwarejoint.media.enums.ImageEffectType.NONE;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 15/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
@Retention(RetentionPolicy.SOURCE)
@StringDef({NONE, EFFECT_SEPIA, EFFECT_GRAYSCALE, EFFECT_POSTERIZE, EFFECT_NEGATIVE,
        EFFECT_BLACKWHITE, EFFECT_LOMOISH, EFFECT_DOCUMENTARY, EFFECT_VIGNETTE})
public @interface ImageEffectType {
    String NONE = "none";
    String EFFECT_SEPIA = EffectFactory.EFFECT_SEPIA;
    String EFFECT_GRAYSCALE = EffectFactory.EFFECT_GRAYSCALE;
    String EFFECT_POSTERIZE = EffectFactory.EFFECT_POSTERIZE;
    String EFFECT_NEGATIVE = EffectFactory.EFFECT_NEGATIVE;
    String EFFECT_BLACKWHITE = EffectFactory.EFFECT_BLACKWHITE;
    String EFFECT_DOCUMENTARY = EffectFactory.EFFECT_DOCUMENTARY;
    String EFFECT_LOMOISH = EffectFactory.EFFECT_LOMOISH;
    String EFFECT_VIGNETTE = EffectFactory.EFFECT_VIGNETTE;
}
