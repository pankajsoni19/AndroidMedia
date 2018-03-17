/*
 * Created By Pankaj Soni <pankajsoni@softwarejoint.com>
 * Copyright Wafer Inc. (c) 2017. All rights reserved
 *
 * Last Modified: 22/11/17 7:00 AM By Pankaj Soni <pankajsoni@softwarejoint.com>
 */

package com.softwarejoint.media.anim;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.transition.Fade;

import android.transition.Slide;
import android.transition.TransitionSet;
import android.view.animation.AccelerateDecelerateInterpolator;

public class AnimationHelper {

    public static Integer fadeIn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return Fade.IN;
        }

        return null;
    }

    public static Integer fadeOut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return Fade.OUT;
        }

        return null;
    }

    static int getAnimationDuration() {
        return Resources.getSystem().getInteger(android.R.integer.config_mediumAnimTime);
    }

    public static long getShortDuration() {
        return Resources.getSystem().getInteger(android.R.integer.config_shortAnimTime);
    }

    private static long getLongDuration() {
        return Resources.getSystem().getInteger(android.R.integer.config_longAnimTime);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static TransitionSet getTransition(@Nullable Integer gravity, @Nullable Integer fade) {
        TransitionSet transitionSet = new TransitionSet();

        if (gravity != null) {
            final int layoutDirection = Resources.getSystem().getConfiguration().getLayoutDirection();
            final int absGravity = GravityCompat.getAbsoluteGravity(gravity, layoutDirection);
            transitionSet.addTransition(AnimationHelper.getSlideTransition(absGravity));
        }

        if (fade != null) {
            transitionSet.addTransition(AnimationHelper.getFadeTransition(fade));
        }

        transitionSet.setDuration(getAnimationDuration())
                .setOrdering(TransitionSet.ORDERING_TOGETHER);

        return transitionSet;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static Fade getFadeTransition(int fadingMode) {
        Fade fade = new Fade(fadingMode);
        fade.setInterpolator(new AccelerateDecelerateInterpolator());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fade.excludeTarget(android.R.id.statusBarBackground, true);
            fade.excludeTarget(android.R.id.navigationBarBackground, true);
        }
        return fade;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Slide getSlideTransition(Integer gravity) {
        Slide slide = new Slide(gravity);
        slide.excludeTarget(android.R.id.statusBarBackground, true);
        slide.excludeTarget(android.R.id.navigationBarBackground, true);
        return slide;
    }

    /** DND:
     * Note: value animator
     public static void setFadeAnimation(final View view, float startAlpha, float endAlpha, int duration) {
     if (startAlpha == endAlpha) {
     return;
     }

     ValueAnimator anim = new ValueAnimator();
     anim.setFloatValues(startAlpha, endAlpha);
     anim.setEvaluator(new FloatEvaluator());
     anim.setDuration(duration);

     anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
    @Override public void onAnimationUpdate(ValueAnimator valueAnimator) {
    view.setAlpha((float) valueAnimator.getAnimatedValue());
    }
    });

     anim.start();
     }


     */

    static void onFinished(final AnimationFinishedListener listener) {
        if (listener != null) {
            listener.onAnimationFinished();
        }
    }
}
