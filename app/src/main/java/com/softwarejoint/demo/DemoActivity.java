package com.softwarejoint.demo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.softwarejoint.media.anim.BaseActivity;
import com.softwarejoint.media.enums.MediaType;
import com.softwarejoint.media.enums.ScaleType;
import com.softwarejoint.media.picker.MediaPickerOpts;
import com.softwarejoint.media.picker.Result;

import java.io.File;

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
        getWindow().setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);

        setTransition(Gravity.END, GravityCompat.END, GravityCompat.END, GravityCompat.END);

        setContentView(R.layout.demo_activity);
        txt_files = findViewById(R.id.txt_files);
        findViewById(R.id.video).setOnClickListener(this);
        findViewById(R.id.image).setOnClickListener(this);
    }

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
            txt_files.setText("Selection Empty");
        }
    }

    private void startVideoPicker() {
        new MediaPickerOpts.Builder()
                .setMediaType(MediaType.VIDEO)
                .canChangeScaleType(Boolean.valueOf("false"))
                .withGallery(Boolean.valueOf("true"))
                .withCameraType(ScaleType.SCALE_SQUARE)
                .withFlash(Boolean.valueOf("true"))
                .withMaxSelection(Integer.parseInt("2"))
                .withFilters(Boolean.valueOf("true"))
                .startActivity(this);
    }

    private void startImagePicker() {
        new MediaPickerOpts.Builder()
                .setMediaType(MediaType.IMAGE)
                .canChangeScaleType(Boolean.valueOf("false"))
                .withGallery(Boolean.valueOf("true"))
                .withCameraType(ScaleType.SCALE_SQUARE)
                .withFlash(Boolean.valueOf("true"))
                .withMaxSelection(Integer.parseInt("2"))
                .withFilters(Boolean.valueOf("true"))
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
