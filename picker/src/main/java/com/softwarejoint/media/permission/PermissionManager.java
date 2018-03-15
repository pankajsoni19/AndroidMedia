package com.softwarejoint.media.permission;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;

public class PermissionManager {

    public static void photoPermission(Activity activity, PermissionCallBack permissionCallBack) {
        requestPermission(activity, permissionCallBack, PermissionRequest.REQUEST_CODE_PHOTO);
    }

    public static void videoPermission(Activity activity, PermissionCallBack permissionCallBack) {
        requestPermission(activity, permissionCallBack, PermissionRequest.REQUEST_CODE_VIDEO);
    }

    private static String[] getPermissionsForFeature(@PermissionRequest int requestCode) {
        switch (requestCode) {
            case PermissionRequest.REQUEST_CODE_PHOTO:
                return new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                };
            case PermissionRequest.REQUEST_CODE_VIDEO:
                return new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS
                };
            default:
                break;
        }

        return null;
    }

    private static void requestPermission(Activity activity, PermissionCallBack permissionCallBack, @PermissionRequest int requestCode) {
        String[] permissionsNeeded = getPermissionsForFeature(requestCode);
        requestPermission(activity, permissionCallBack, permissionsNeeded, requestCode);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static void requestPermission(Activity activity, PermissionCallBack permissionCallBack,
                                          String[] permissions, @PermissionRequest int requestCode) {

        if (activity == null || activity.isFinishing()) {
            return;
        }

        String[] permissionsNeeded = checkPermissionsNeeded(activity, permissions);

        if (permissionsNeeded == null || permissionsNeeded.length == 0) {
            if (permissionCallBack != null)
                permissionCallBack.onAccessPermission(true, requestCode);
            return;
        }

        if (permissionCallBack instanceof FragmentActivity) {
            ((FragmentActivity) permissionCallBack).requestPermissions(permissionsNeeded, requestCode);
        } else if (permissionCallBack instanceof Fragment) {
            ((Fragment) permissionCallBack).requestPermissions(permissionsNeeded, requestCode);
        } else {
            ActivityCompat.requestPermissions(activity, permissionsNeeded, requestCode);
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

    public static void onRequestPermissionResult(@PermissionRequest int requestCode, int[] grantResults,
                                                 PermissionCallBack permissionCallBack) {

        if (grantResults.length == 0) return;

        boolean granted = false;

        for (int grantResult : grantResults) {
            granted = (grantResult == PackageManager.PERMISSION_GRANTED);
            if (!granted) break;
        }

        if (permissionCallBack != null) {
            permissionCallBack.onAccessPermission(granted, requestCode);
        }
    }
}
