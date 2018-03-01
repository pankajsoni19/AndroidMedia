package com.serenegiant.audiovideosample;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: MainActivity.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.serenegiant.mediaaudiotest.R;
import com.serenegiant.permission.PermissionCallBack;
import com.serenegiant.permission.PermissionManager;
import com.serenegiant.permission.PermissionRequest;

public class MainActivity extends FragmentActivity implements PermissionCallBack, FragmentManager.OnBackStackChangedListener {

    private Handler uiThreadHandler;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        uiThreadHandler = new Handler();

        PermissionManager.videoPermission(this, this);
    }

    private void launchCameraFragment() {
        FragmentManager manager = getSupportFragmentManager();

        if (manager.getBackStackEntryCount() == 0) {
            CameraFragment fragment = CameraFragment.newInstance();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.container, fragment, fragment.TAG);
            transaction.addToBackStack(fragment.TAG);
            transaction.commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportFragmentManager().addOnBackStackChangedListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getSupportFragmentManager().removeOnBackStackChangedListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.onRequestPermissionResult(requestCode, grantResults, this);
    }

    @Override
    public void onAccessPermission(boolean permissionGranted, int permission) {
        if (!permissionGranted || permission != PermissionRequest.REQUEST_CODE_VIDEO) {
            return;
        }

        uiThreadHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                launchCameraFragment();
            }
        }, 500L);
    }

    @Override
    public void onBackStackChanged() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            finish();
        }
    }
}