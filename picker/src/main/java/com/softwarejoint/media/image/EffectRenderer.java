package com.softwarejoint.media.image;

import android.media.effect.Effect;
import android.media.effect.EffectFactory;
import android.opengl.Matrix;
import android.support.annotation.Nullable;

import com.softwarejoint.media.enums.ImageEffectType;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 16/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
class EffectRenderer extends TextureRenderer {

    public final @ImageEffectType String TAG;
    private Effect mEffect;
    public int startX;
    public int bottomY;
    private int texId;

    EffectRenderer(@Nullable EffectFactory effectFactory, @ImageEffectType String effectType) {
        super();
        TAG = effectType;

        init();
        initEffect(effectFactory, effectType);
    }

    void makeEffectCurrent(int inputTexId, int width, int height, int outputTexId) {
        if (ImageEffectType.NONE.equals(TAG)) {
            texId = inputTexId;
        } else {
            mEffect.apply(inputTexId, width, height, outputTexId);
            texId = outputTexId;
        }
    }

    void renderTexture() {
        super.renderTexture(texId);
    }

    private void initEffect(EffectFactory effectFactory, @ImageEffectType String effectType) {
        switch (effectType) {
            case ImageEffectType.NONE:
                return;
            case ImageEffectType.EFFECT_GRAYSCALE:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_GRAYSCALE);
                break;
            case ImageEffectType.EFFECT_POSTERIZE:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_POSTERIZE);
                break;
            case ImageEffectType.EFFECT_SEPIA:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_SEPIA);
                break;
            case ImageEffectType.EFFECT_NEGATIVE:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_NEGATIVE);
                break;
            case ImageEffectType.EFFECT_BLACKWHITE:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_BLACKWHITE);
                mEffect.setParameter("black", .1f);
                mEffect.setParameter("white", .7f);
                break;
            case ImageEffectType.EFFECT_DOCUMENTARY:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_DOCUMENTARY);
                break;
            case ImageEffectType.EFFECT_LOMOISH:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_LOMOISH);
                break;
            case ImageEffectType.EFFECT_VIGNETTE:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_VIGNETTE);
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