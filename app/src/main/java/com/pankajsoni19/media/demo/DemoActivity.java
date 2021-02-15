package com.pankajsoni19.media.demo;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.core.view.GravityCompat;

import com.pankajsoni19.media.base.BaseActivity;
import com.pankajsoni19.media.enums.MediaType;
import com.pankajsoni19.media.enums.ScaleType;
import com.pankajsoni19.media.picker.MediaPickerOpts;
import com.pankajsoni19.media.picker.Result;

import java.io.File;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

/**
 * Created by Pankaj Soni <pankajsoni19@live.com> on 03/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class DemoActivity extends BaseActivity {

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
        findViewById(R.id.video).setOnClickListener(v -> startVideoPicker());
        findViewById(R.id.image).setOnClickListener(v -> startImagePicker());
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
            for (String file : result.files) {
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
                .withGallery(Boolean.parseBoolean("true"))
                .withFlash(Boolean.parseBoolean("true"))
                .withFilters(Boolean.parseBoolean("true"))
                .withCropEnabled(Boolean.parseBoolean("false"))
                .canChangeScaleType(Boolean.parseBoolean("true"))
                .withMaxSelection(Integer.parseInt("2"))
                .build()
                .startActivity(this);
    }

    private void startImagePicker() {
        new MediaPickerOpts.Builder()
                .setMediaType(MediaType.IMAGE)
                .withCameraType(ScaleType.SCALE_SQUARE)
                .withGallery(Boolean.parseBoolean("true"))
                .withFlash(Boolean.parseBoolean("true"))
                .withFilters(Boolean.parseBoolean("true"))
                .withCropEnabled(Boolean.parseBoolean("true"))
                .canChangeScaleType(Boolean.parseBoolean("true"))
                .withImgSize(Integer.parseInt("96"))
                .withMaxSelection(Integer.parseInt("2"))
                .build()
                .startActivity(this);
    }
}