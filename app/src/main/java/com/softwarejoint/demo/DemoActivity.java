package com.softwarejoint.demo;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.softwarejoint.media.base.BaseActivity;
import com.softwarejoint.media.enums.MediaType;
import com.softwarejoint.media.enums.ScaleType;
import com.softwarejoint.media.picker.MediaPickerOpts;
import com.softwarejoint.media.picker.Result;

import java.io.File;
import java.io.IOException;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 03/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class DemoActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "DemoActivity";

    private TextView txt_files;

    @Override
    protected void onCreate(@org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);

        setTransition(Gravity.END, GravityCompat.END, GravityCompat.END, GravityCompat.END);

        setContentView(R.layout.demo_activity);

        txt_files = findViewById(R.id.txt_files);
        findViewById(R.id.video).setOnClickListener(this);
        findViewById(R.id.image).setOnClickListener(this);
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        new EncoderTest().testAACEncoders();
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Result result = MediaPickerOpts.onActivityResult(requestCode, resultCode, data);
        if (result != null) {
            StringBuilder builder = new StringBuilder();
            for (String file: result.files) {
                builder.append(new File(file).getName()).append("\r\n");
                Log.d(TAG, "file: picked: " + file);
            }

            txt_files.setText(builder.toString());
        } else {
            txt_files.setText(R.string.empty_selection);
        }
    }

    private void startVideoPicker() {
        new MediaPickerOpts.Builder()
                .setMediaType(MediaType.VIDEO)
                .withCameraType(ScaleType.SCALE_SQUARE)
                .withGallery(Boolean.valueOf("true"))
                .withFlash(Boolean.valueOf("true"))
                .withFilters(Boolean.valueOf("true"))
                .withCropEnabled(Boolean.valueOf("false"))
                .canChangeScaleType(Boolean.valueOf("true"))
                .withMaxSelection(Integer.parseInt("2"))
                .build()
                .startActivity(this);
    }

    private void startImagePicker() {
        new MediaPickerOpts.Builder()
                .setMediaType(MediaType.IMAGE)
                .withCameraType(ScaleType.SCALE_SQUARE)
                .withGallery(Boolean.valueOf("true"))
                .withFlash(Boolean.valueOf("true"))
                .withFilters(Boolean.valueOf("true"))
                .withCropEnabled(Boolean.valueOf("true"))
                .canChangeScaleType(Boolean.valueOf("true"))
                .withImgSize(Integer.valueOf("96"))
                .withMaxSelection(Integer.parseInt("2"))
                .build()
                .startActivity(this);
    }

    @Override
    public void onClick(View v) {
        txt_files.setText("");

        switch (v.getId()) {
            case R.id.video:
                startVideoPicker();
                break;
            case R.id.image:
                startImagePicker();
                break;
        }
    }
}