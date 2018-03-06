package com.softwarejoint.media.camera;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.view.Gravity;
import android.view.Window;

import com.softwarejoint.media.R;
import com.softwarejoint.media.anim.BaseActivity;
import com.softwarejoint.media.fileio.MemoryCache;
import com.softwarejoint.media.permission.PermissionCallBack;
import com.softwarejoint.media.permission.PermissionManager;
import com.softwarejoint.media.permission.PermissionRequest;
import com.softwarejoint.media.picker.MediaPickerOpts;

import java.util.List;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

public class PickerActivity extends BaseActivity implements PermissionCallBack, FragmentManager.OnBackStackChangedListener {

    private Handler uiThreadHandler;
    private MemoryCache memoryCache;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);

        setTransition(Gravity.END, GravityCompat.END, GravityCompat.END, GravityCompat.END);

        setContentView(R.layout.activity_main);
        uiThreadHandler = new Handler();

        memoryCache = MemoryCache.getInstance();
        PermissionManager.videoPermission(this, this);
    }

    private void launchCameraFragment() {
        FragmentManager manager = getSupportFragmentManager();

        if (manager.getBackStackEntryCount() == 0) {
            MediaPickerOpts opts = getIntent().getParcelableExtra(MediaPickerOpts.INTENT_OPTS);
            if (opts == null) {
                uiThreadHandler.postDelayed(this::supportFinishAfterTransition, 500L);
                return;
            }

            CameraFragment fragment = CameraFragment.newInstance(opts);
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.container, fragment, fragment.TAG);
            transaction.addToBackStack(fragment.TAG);
            transaction.commit();
        }
    }

    @Override
    public void onBackPressed() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments.size() > 0) {
            Fragment topFrag = fragments.get(fragments.size() - 1);
            if (!topFrag.isRemoving() && topFrag instanceof CameraFragment) {
                if (((CameraFragment) topFrag).onBackPressed()) return;
            }
        }

        super.onBackPressed();
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
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            memoryCache.clear();
        }
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

        uiThreadHandler.postDelayed(this::launchCameraFragment, 500L);
    }

    @Override
    public void onBackStackChanged() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            supportFinishAfterTransition();
        }
    }
}