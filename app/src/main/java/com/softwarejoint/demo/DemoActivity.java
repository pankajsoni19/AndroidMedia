package com.softwarejoint.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Window;

import com.softwarejoint.media.anim.BaseActivity;
import com.softwarejoint.media.enums.MediaType;
import com.softwarejoint.media.enums.ScaleType;
import com.softwarejoint.media.picker.MediaPicker;
import com.softwarejoint.media.picker.Result;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 03/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class DemoActivity extends BaseActivity {

    private static final String TAG = "DemoActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launcher);
        startPicker();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Result result = MediaPicker.onActivityResult(requestCode, resultCode, data);
        if (result != null) {
            for (String file: result.files) {
                Log.d(TAG, "file: picked: " + file);
            }
        }
    }

    private void startPicker() {
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
