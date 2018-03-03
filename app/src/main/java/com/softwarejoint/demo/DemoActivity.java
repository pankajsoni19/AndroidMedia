package com.softwarejoint.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;

import com.softwarejoint.media.anim.BaseActivity;
import com.softwarejoint.media.enums.MediaType;
import com.softwarejoint.media.enums.ScaleType;
import com.softwarejoint.media.picker.MediaPicker;
import com.softwarejoint.media.picker.Result;

import java.io.File;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 03/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class DemoActivity extends BaseActivity {

    private static final String TAG = "DemoActivity";

    private Handler handler = new Handler();

    private TextView txt_files;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launcher);
        txt_files = findViewById(R.id.txt_files);
        handler = new Handler();
        handler.postDelayed(this::startPicker, 500L);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Result result = MediaPicker.onActivityResult(requestCode, resultCode, data);
        if (result != null) {
            StringBuilder builder = new StringBuilder();
            for (String file: result.files) {
                builder.append(new File(file).getName()).append("\r\n");
                Log.d(TAG, "file: picked: " + file);
            }

            txt_files.setText(builder.toString());
        }
    }

    private void startPicker() {
        txt_files.setText("");

        new MediaPicker.Builder()
                .setMediaType(MediaType.VIDEO)
                .withGallery(Boolean.valueOf("true"))
                .withCamera(Boolean.valueOf("true"))
                .withCameraType(ScaleType.SCALE_CROP_CENTER)
                .withCameraFront(Boolean.valueOf("true"))
                .withFlash(Boolean.valueOf("true"))
                .withMaxPick(Integer.parseInt("2"))
                .startActivity(this);
    }
}
