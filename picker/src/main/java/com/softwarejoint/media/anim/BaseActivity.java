package com.softwarejoint.media.anim;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.transition.Fade;
import android.transition.TransitionSet;
import android.view.Window;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 03/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class BaseActivity extends FragmentActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            requestWindowFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            requestWindowFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);

            getWindow().setAllowEnterTransitionOverlap(true);
            getWindow().setAllowReturnTransitionOverlap(true);
        }
    }

    /**
     * Note: all values are #Gravity.Class
     *
     * @param enter   when entering activity
     * @param reenter returning from previously started activity
     * @param exit    when starting new activity
     * @param finish  when finishing current activity and going back
     */
    @SuppressWarnings("SameParameterValue")
    protected void setTransition(@Nullable Integer enter, @Nullable Integer reenter,
                                 @Nullable Integer exit, @Nullable Integer finish) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        setEnterTransition(enter, Fade.IN);
        setReenterTransition(reenter, Fade.IN);
        setReturnTransition(finish, Fade.OUT);
        setExitTransition(exit, Fade.OUT);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setEnterTransition(@Nullable Integer gravity, @Nullable Integer fade) {
        TransitionSet transitionSet = AnimationHelper.getTransition(this, gravity, fade);
        getWindow().setEnterTransition(transitionSet);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setReenterTransition(@Nullable Integer gravity, @Nullable Integer fade) {
        TransitionSet transitionSet = AnimationHelper.getTransition(this, gravity, fade);
        getWindow().setReenterTransition(transitionSet);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setReturnTransition(@Nullable Integer finish, @Nullable Integer fade) {
        TransitionSet transitionSet = AnimationHelper.getTransition(this, finish, fade);
        getWindow().setReturnTransition(transitionSet);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setExitTransition(@Nullable Integer gravity, @Nullable Integer fade) {
        TransitionSet transitionSet = AnimationHelper.getTransition(this, gravity, fade);
        getWindow().setExitTransition(transitionSet);
    }
}