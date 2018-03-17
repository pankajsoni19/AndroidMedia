package com.softwarejoint.media.enums;

import android.media.effect.EffectFactory;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.softwarejoint.media.enums.ImageEffect.BLACKWHITE;
import static com.softwarejoint.media.enums.ImageEffect.DOCUMENTARY;
import static com.softwarejoint.media.enums.ImageEffect.GRAYSCALE;
import static com.softwarejoint.media.enums.ImageEffect.LOMOISH;
import static com.softwarejoint.media.enums.ImageEffect.NEGATIVE;
import static com.softwarejoint.media.enums.ImageEffect.POSTERIZE;
import static com.softwarejoint.media.enums.ImageEffect.SEPIA;
import static com.softwarejoint.media.enums.ImageEffect.VIGNETTE;
import static com.softwarejoint.media.enums.ImageEffect.NONE;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 15/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
@Retention(RetentionPolicy.SOURCE)
@StringDef({NONE, SEPIA, GRAYSCALE, POSTERIZE, NEGATIVE,
        BLACKWHITE, LOMOISH, DOCUMENTARY, VIGNETTE})
public @interface ImageEffect {
    String NONE = "none";
    String SEPIA = EffectFactory.EFFECT_SEPIA;
    String GRAYSCALE = EffectFactory.EFFECT_GRAYSCALE;
    String POSTERIZE = EffectFactory.EFFECT_POSTERIZE;
    String NEGATIVE = EffectFactory.EFFECT_NEGATIVE;
    String BLACKWHITE = EffectFactory.EFFECT_BLACKWHITE;
    String DOCUMENTARY = EffectFactory.EFFECT_DOCUMENTARY;
    String LOMOISH = EffectFactory.EFFECT_LOMOISH;
    String VIGNETTE = EffectFactory.EFFECT_VIGNETTE;
}
