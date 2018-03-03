/*
 * Created By Pankaj Soni <pankajsoni@softwarejoint.com>
 * Copyright Wafer Inc. (c) 2017. All rights reserved
 *
 * Last Modified: 22/11/17 8:49 AM By Pankaj Soni <pankajsoni@softwarejoint.com>
 */

package com.softwarejoint.media.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;

public class AnimFadeReveal extends AnimationHelper {

    private static final String TAG = "AnimFadeReveal";

    private static final float ALPHA_VISIBLE = 1.0f;
    private static final float ALPHA_GONE = 0f;

    public static void fadeIn(View view) {
        view.setAlpha(ALPHA_GONE);

        view.animate().alpha(ALPHA_VISIBLE)
                .setDuration(getAnimationDuration(view.getContext()))
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        Log.d(TAG, "onAnimationEnd: " + view.getHeight() + " w: " + view.getWidth() + " alpha: " + view.getAlpha());
                    }
                })
                .start();
    }

    public static void fadeOut(View view) {
        fadeOut(view, null);
    }

    public static void fadeOut(View view, AnimationFinishedListener listener) {
        ViewPropertyAnimator animation =
                view.animate().alpha(ALPHA_GONE)
                        .setDuration(getAnimationDuration(view.getContext()))
                        .setInterpolator(new AccelerateDecelerateInterpolator());

        if (listener != null) {
            animation.setListener(new MyAnimatorListener(listener));
        }

        animation.start();
    }

    static class MyAnimatorListener extends AnimatorListenerAdapter {

        private AnimationFinishedListener listener;

        MyAnimatorListener(AnimationFinishedListener listener) {
            this.listener = listener;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            super.onAnimationCancel(animation);
            onFinished(listener);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            onFinished(listener);
        }
    }
}