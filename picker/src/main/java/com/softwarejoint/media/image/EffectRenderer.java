package com.softwarejoint.media.image;

import android.graphics.Color;
import android.media.effect.Effect;
import android.media.effect.EffectFactory;
import android.support.annotation.Nullable;

import com.softwarejoint.media.enums.ImageEffect;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 16/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
class EffectRenderer extends TextureRenderer {

    public final @ImageEffect
    String TAG;
    private Effect mEffect;
    public int startX;
    public int bottomY;
    private int texId;

    EffectRenderer(@Nullable EffectFactory effectFactory, @ImageEffect String effectType) {
        super();
        TAG = effectType;

        init();
        initEffect(effectFactory, effectType);
    }

    void makeEffectCurrent(int inputTexId, int width, int height, int outputTexId) {
        if (ImageEffect.NONE.equals(TAG)) {
            texId = inputTexId;
        } else {
            mEffect.apply(inputTexId, width, height, outputTexId);
            texId = outputTexId;
        }
    }

    void renderTexture() {
        super.renderTexture(texId);
    }

    private void initEffect(EffectFactory effectFactory, @ImageEffect String effectType) {
        switch (effectType) {
            case ImageEffect.NONE:
                return;
          /*  case ImageEffect.GRAYSCALE:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_GRAYSCALE);
                break;
            case ImageEffect.POSTERIZE:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_POSTERIZE);
                break;
                */
            case ImageEffect.SEPIA:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_SEPIA);
                break;
            case ImageEffect.NEGATIVE:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_NEGATIVE);
                break;
            case ImageEffect.BLACKWHITE:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_GRAYSCALE);
                break;
           // case ImageEffect.DOCUMENTARY:
           //     mEffect = effectFactory.createEffect(EffectFactory.EFFECT_DOCUMENTARY);
           //     break;
            case ImageEffect.LOMOISH:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_LOMOISH);
                break;
            case ImageEffect.CROSSPROCESS:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_CROSSPROCESS);
                break;
            case ImageEffect.FILLIGHT:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_FILLLIGHT);
                mEffect.setParameter("strength",0.5f);
                break;
           // case ImageEffect.VIGNETTE:
           //     mEffect = effectFactory.createEffect(EffectFactory.EFFECT_VIGNETTE);
            case ImageEffect.DUOTONEPY:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_DUOTONE);
                mEffect.setParameter("first_color", Color.rgb(128, 0, 255));
                mEffect.setParameter("second_color", Color.rgb(255, 207, 30));
                break;
            case ImageEffect.DUOTONEBW:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_DUOTONE);
                mEffect.setParameter("first_color", Color.rgb(0, 114, 255));
                mEffect.setParameter("second_color", Color.rgb(255, 255, 255));
                break;
        }
    }

    void setStartXY(int startX, int bottomY) {
        this.startX = startX;
        this.bottomY = bottomY;
    }

    public void release() {
        if (mEffect != null) {
            mEffect.release();
        }

        mEffect = null;
        super.release();
    }

    public String name() {
        return TAG;
    }
}