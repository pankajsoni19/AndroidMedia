package com.pankajsoni19.media.permission;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by Pankaj Soni <pankajsoni19@live.com> on 18/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class PermissionActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionManager.requestPermission(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = PermissionManager.onRequestPermissionResult(grantResults);
        setResult(granted ? RESULT_OK : RESULT_CANCELED);
        finish();
    }
}