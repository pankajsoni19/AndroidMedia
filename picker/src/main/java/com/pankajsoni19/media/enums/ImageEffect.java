package com.pankajsoni19.media.enums;

import android.media.effect.EffectFactory;
import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.pankajsoni19.media.enums.ImageEffect.BLACKWHITE;
import static com.pankajsoni19.media.enums.ImageEffect.CROSSPROCESS;
import static com.pankajsoni19.media.enums.ImageEffect.DUOTONEBW;
import static com.pankajsoni19.media.enums.ImageEffect.DUOTONEPY;
import static com.pankajsoni19.media.enums.ImageEffect.FILLIGHT;
import static com.pankajsoni19.media.enums.ImageEffect.LOMOISH;
import static com.pankajsoni19.media.enums.ImageEffect.NEGATIVE;
import static com.pankajsoni19.media.enums.ImageEffect.SEPIA;
import static com.pankajsoni19.media.enums.ImageEffect.NONE;

/**
 * Created by Pankaj Soni <pankajsoni19@live.com> on 15/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
@Retention(RetentionPolicy.SOURCE)
@StringDef({NONE, SEPIA, NEGATIVE, CROSSPROCESS,
        BLACKWHITE, LOMOISH, DUOTONEPY, DUOTONEBW, FILLIGHT})
//POSTERIZE, DOCUMENTARY, GRAYSCALE,VIGNETTE
public @interface ImageEffect {
    String NONE = "none";
    String CROSSPROCESS = EffectFactory.EFFECT_CROSSPROCESS;
    String SEPIA = EffectFactory.EFFECT_SEPIA;
   // String GRAYSCALE = EffectFactory.EFFECT_GRAYSCALE;
  //  String POSTERIZE = EffectFactory.EFFECT_POSTERIZE;
    String NEGATIVE = EffectFactory.EFFECT_NEGATIVE;
    String BLACKWHITE = EffectFactory.EFFECT_BLACKWHITE;
    //String DOCUMENTARY = EffectFactory.EFFECT_DOCUMENTARY;
    String LOMOISH = EffectFactory.EFFECT_LOMOISH;
   // String VIGNETTE = EffectFactory.EFFECT_VIGNETTE;
    String DUOTONEPY = EffectFactory.EFFECT_DUOTONE;
    String DUOTONEBW = EffectFactory.EFFECT_DUOTONE + "bw";
    String FILLIGHT = EffectFactory.EFFECT_FILLLIGHT;

}
