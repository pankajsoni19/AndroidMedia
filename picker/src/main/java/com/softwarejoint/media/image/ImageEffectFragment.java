package com.softwarejoint.media.image;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.softwarejoint.media.R;
import com.softwarejoint.media.anim.AnimationHelper;
import com.softwarejoint.media.enums.CropType;
import com.softwarejoint.media.picker.MediaPickerOpts;
import com.softwarejoint.media.picker.PickerFragment;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 15/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class ImageEffectFragment extends PickerFragment implements View.OnClickListener {

    private static final String IMAGE_PATH = "com.softwarejoint.image";

    public final String TAG = "ImageEffectFragment";

    public static ImageEffectFragment newInstance(MediaPickerOpts opts, String imagePath) {
        Bundle args = new Bundle();
        args.putParcelable(MediaPickerOpts.INTENT_OPTS, opts);
        args.putString(ImageEffectFragment.IMAGE_PATH, imagePath);

        ImageEffectFragment fragment = new ImageEffectFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private MediaPickerOpts opts;

    private EffectGLView effectView;
    private ImageView iv_crop, iv_crop_circle, iv_crop_star, iv_crop_flower, iv_crop_hand;
    private ImageView iv_crop_mask;
    private PathCropView pathCropView;

    private View iv_filter;

    private Handler uiThreadHandler;

    private @CropType
    int cropType = CropType.NONE;
    private String originalImagePath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (opts == null) {
            Bundle args = getArguments();
            //noinspection ConstantConditions
            opts = args.getParcelable(MediaPickerOpts.INTENT_OPTS);
            originalImagePath = args.getString(ImageEffectFragment.IMAGE_PATH);
        }

        uiThreadHandler = new Handler();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_effect, container, false);

        rootView.findViewById(R.id.iv_back).setOnClickListener(this);
        rootView.findViewById(R.id.iv_done).setOnClickListener(this);

        effectView = rootView.findViewById(R.id.effectView);

        iv_crop = rootView.findViewById(R.id.iv_crop);
        iv_crop_circle = rootView.findViewById(R.id.iv_crop_circle);
        iv_crop_star = rootView.findViewById(R.id.iv_crop_star);
        iv_crop_flower = rootView.findViewById(R.id.iv_crop_flower);
        iv_crop_hand = rootView.findViewById(R.id.iv_crop_hand);

        iv_crop_mask = rootView.findViewById(R.id.iv_crop_mask);
        pathCropView = rootView.findViewById(R.id.pathCropView);

        iv_filter = rootView.findViewById(R.id.iv_filter);

        if (opts.cropEnabled) {
            iv_crop.setOnClickListener(this);
            iv_crop_circle.setOnClickListener(this);
            iv_crop_star.setOnClickListener(this);
            iv_crop_flower.setOnClickListener(this);
            iv_crop_hand.setOnClickListener(this);
        } else {
            iv_crop.setVisibility(View.GONE);
        }

        if (opts.filtersEnabled) {
            iv_filter.setOnClickListener(this);
        } else {
            iv_filter.setVisibility(View.GONE);
        }

        effectView.init(originalImagePath, opts);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        effectView.onResume();
    }

    private void onClickDone() {
        Log.d(TAG, "onClickDone");
    }

    @Override
    public void onClick(final View view) {
        final int id = view.getId();
        if (R.id.iv_back == id) {
            //noinspection ConstantConditions
            getActivity().setResult(Activity.RESULT_CANCELED);
            getActivity().supportFinishAfterTransition();
        } else if (R.id.iv_crop == id) {
            toggleCrop();
        } else if (R.id.iv_crop_circle == id) {
            onCropSelected(CropType.CIRCLE);
        } else if (R.id.iv_crop_star == id) {
            onCropSelected(CropType.STAR);
        } else if (R.id.iv_crop_flower == id) {
            onCropSelected(CropType.FLOWER);
        } else if (R.id.iv_crop_hand == id) {
            onCropSelected(CropType.PATH);
        } else if (R.id.iv_filter == id) {

        } else if (R.id.iv_done == id) {
            onClickDone();
        }
    }

    private void onCropSelected(@CropType int type) {
        cropType = type;
        switch (cropType) {
            case CropType.NONE:
                iv_crop_mask.setVisibility(View.GONE);
                break;
            case CropType.CIRCLE:
                iv_crop_mask.setImageResource(R.drawable.circle_mask);
                iv_crop_mask.setVisibility(View.VISIBLE);
                break;
            case CropType.STAR:
                iv_crop_mask.setImageResource(R.drawable.star_mask);
                iv_crop_mask.setVisibility(View.VISIBLE);
                break;
            case CropType.FLOWER:
                iv_crop_mask.setImageResource(R.drawable.flower_mask);
                iv_crop_mask.setVisibility(View.VISIBLE);
                break;
            case CropType.PATH:
                iv_crop_mask.setVisibility(View.GONE);
                pathCropView.setVisibility(View.VISIBLE);
                break;
        }
    }

    private boolean isCropVisible() {
        return iv_crop_circle.getVisibility() == View.VISIBLE;
    }

    private void toggleCrop() {
        final long duration = AnimationHelper.getShortDuration(iv_crop.getContext());

        iv_crop.setOnClickListener(null);

        if (isCropVisible()) {
            iv_crop_mask.setVisibility(View.GONE);
            pathCropView.setVisibility(View.GONE);

            final int translationX = (int) (Resources.getSystem().getDisplayMetrics().density * 48);

            iv_crop_circle.animate().translationX(translationX)
                    .alpha(0).setDuration(duration).start();

            iv_crop_star.animate().translationX(translationX * 2)
                    .alpha(0).setDuration(duration * 2).start();

            iv_crop_flower.animate().translationX(translationX * 3)
                    .alpha(0).setDuration(duration * 3).start();

            iv_crop_hand.animate().translationX(translationX * 4)
                    .alpha(0).setDuration(duration * 4)
                    .setListener(new AnimatorListenerAdapter() {

                        @Override
                        public void onAnimationStart(Animator animation) {
                            iv_crop.setImageResource(R.drawable.crop_option_white);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            iv_crop_circle.setVisibility(View.GONE);
                            iv_crop_star.setVisibility(View.GONE);
                            iv_crop_flower.setVisibility(View.GONE);
                            iv_crop_hand.setVisibility(View.GONE);
                            iv_crop.setOnClickListener(ImageEffectFragment.this);
                        }
                    }).start();
        } else {
            iv_crop_circle.setVisibility(View.VISIBLE);
            iv_crop_star.setVisibility(View.VISIBLE);
            iv_crop_flower.setVisibility(View.VISIBLE);
            iv_crop_hand.setVisibility(View.VISIBLE);

            iv_crop_circle.animate().translationX(0)
                    .alpha(1).setDuration(duration).start();

            iv_crop_star.animate().translationX(0)
                    .alpha(1).setDuration(duration * 2).start();

            iv_crop_flower.animate().translationX(0)
                    .alpha(1).setDuration(duration * 3).start();

            iv_crop_hand.animate().translationX(0)
                    .alpha(1).setDuration(duration * 4)
                    .setListener(new AnimatorListenerAdapter() {

                        @Override
                        public void onAnimationStart(Animator animation) {
                            onCropSelected(cropType);
                            iv_crop.setImageResource(R.drawable.clear_white);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            iv_crop.setOnClickListener(ImageEffectFragment.this);
                        }
                    }).start();
        }
    }
}