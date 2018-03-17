package com.softwarejoint.media.permission;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.softwarejoint.media.anim.AnimationHelper;

import java.util.ArrayList;

public class PermissionManager {

    private static final String PERMISSION_REQUEST = "PERMISSION_REQUEST";

    public static void photoPermission(Activity activity, PermissionCallBack permissionCallBack) {
        checkPermission(activity, permissionCallBack, PermissionRequest.REQUEST_CODE_PHOTO);
    }

    public static void videoPermission(Activity activity, PermissionCallBack permissionCallBack) {
        checkPermission(activity, permissionCallBack, PermissionRequest.REQUEST_CODE_VIDEO);
    }

    @TargetApi(Build.VERSION_CODES.M)
    static void requestPermission(Activity activity) {
        @PermissionRequest int requestCode =
                activity.getIntent().getIntExtra(PERMISSION_REQUEST, PermissionRequest.REQUEST_CODE_PHOTO);

        String[] permissionsNeeded = getPermissionsForFeature(requestCode);
        activity.requestPermissions(permissionsNeeded, requestCode);
    }

    static boolean onRequestPermissionResult(int[] grantResults) {
        boolean granted = false;

        for (int grantResult : grantResults) {
            granted = (grantResult == PackageManager.PERMISSION_GRANTED);
            if (!granted) break;
        }

        return granted;
    }

    private static String[] getPermissionsForFeature(@PermissionRequest int requestCode) {
        switch (requestCode) {
            case PermissionRequest.REQUEST_CODE_VIDEO:
                return new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS
                };
            case PermissionRequest.REQUEST_CODE_PHOTO:
            default:
                return new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                };
        }
    }

    private static void checkPermission(Activity activity, PermissionCallBack permissionCallBack, @PermissionRequest int requestCode) {
        String[] permissionsNeeded = getPermissionsForFeature(requestCode);
        checkPermission(activity, permissionCallBack, permissionsNeeded, requestCode);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static void checkPermission(Activity activity, PermissionCallBack permissionCallBack,
                                          String[] permissions, @PermissionRequest int requestCode) {

        String[] permissionsNeeded = checkPermissionsNeeded(activity, permissions);

        if (permissionsNeeded == null || permissionsNeeded.length == 0) {
            if (permissionCallBack != null) {
                permissionCallBack.onPermissionGranted();
            }
        } else {
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(activity, PermissionActivity.class);
                activity.startActivityForResult(intent, requestCode);
            }, AnimationHelper.getAnimationDuration());
        }
    }

    private static String[] checkPermissionsNeeded(@NonNull Context context, String[] permissions) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null;

        ArrayList<String> permissionsNeeded = new ArrayList<>();

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        return permissionsNeeded.toArray(new String[permissionsNeeded.size()]);
    }
}
