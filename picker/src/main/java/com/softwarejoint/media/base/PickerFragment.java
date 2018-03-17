package com.softwarejoint.media.base;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.transition.Fade;
import android.transition.TransitionSet;

import com.softwarejoint.media.R;
import com.softwarejoint.media.anim.AnimationHelper;

import java.util.List;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 15/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public abstract class PickerFragment extends Fragment {

    private String TAG = getClass().getSimpleName();

    protected Handler uiThreadHandler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiThreadHandler = new Handler();
    }

    protected void showFragment(PickerFragment fragment) {
        showFragment(fragment, getFragmentManager());
    }

    private void showFragment(PickerFragment fragment, FragmentManager fragmentManager) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        Fragment previousFragment = fragmentManager.findFragmentByTag(fragment.TAG);

        if (previousFragment != null) {
            transaction.remove(previousFragment);
            transaction.addToBackStack(null);
        }

        transaction.replace(R.id.container, fragment, fragment.TAG);

        fragment.setEnterTransition(GravityCompat.END, AnimationHelper.fadeIn());
        fragment.setExitTransition(GravityCompat.START, AnimationHelper.fadeOut());

        transaction.addToBackStack(fragment.TAG);

        transaction.commit();
    }

    /**
     * Note: all values are #Gravity.Class
     *
     * @param enter   when entering activity
     * @param reenter returning from previously started activity
     * @param exit    when starting new activity
     * @param finish  when finishing current activity and going back
     */
    public final void setTransition(@Nullable Integer enter, @Nullable Integer reenter,
                                    @Nullable Integer exit, @Nullable Integer finish) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) { return; }

        setEnterTransition(enter, Fade.IN);
        setReenterTransition(reenter, Fade.IN);
        setReturnTransition(finish, Fade.OUT);
        setExitTransition(exit, Fade.OUT);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public final void setEnterTransition(@Nullable Integer gravity, @Nullable Integer fade) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) { return; }

        TransitionSet transitionSet = AnimationHelper.getTransition(gravity, fade);
        setEnterTransition(transitionSet);
        setAllowEnterTransitionOverlap(true);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public final void setReenterTransition(@Nullable Integer gravity, @Nullable Integer fade) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) { return; }

        TransitionSet transitionSet = AnimationHelper.getTransition(gravity, fade);
        setReenterTransition(transitionSet);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public final void setReturnTransition(@Nullable Integer finish, @Nullable Integer fade) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) { return; }

        TransitionSet transitionSet = AnimationHelper.getTransition(finish, fade);
        setReturnTransition(transitionSet);
        setAllowReturnTransitionOverlap(true);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public final void setExitTransition(@Nullable Integer gravity, @Nullable Integer fade) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) { return; }

        TransitionSet transitionSet = AnimationHelper.getTransition(gravity, fade);
        setExitTransition(transitionSet);
    }

    public void dismiss() {
        //noinspection ConstantConditions
        getFragmentManager().popBackStack();
    }

    /**
     * NOTE: Returns true if handled
     * */
    public boolean onBackPressed() {
        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
            List<Fragment> fragments = getChildFragmentManager().getFragments();
            return ((PickerFragment) fragments.get(fragments.size() - 1)).onBackPressed();
        }

        dismiss();

        return true;
    }
}