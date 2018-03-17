package com.softwarejoint.media.picker;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AppCompatDelegate;
import android.view.Gravity;
import android.view.Window;

import com.softwarejoint.media.R;
import com.softwarejoint.media.base.BaseActivity;
import com.softwarejoint.media.base.PickerFragment;
import com.softwarejoint.media.camera.CameraFragment;
import com.softwarejoint.media.enums.MediaType;
import com.softwarejoint.media.fileio.MemoryCache;
import com.softwarejoint.media.image.ImageEffectFragment;
import com.softwarejoint.media.permission.PermissionCallBack;
import com.softwarejoint.media.permission.PermissionManager;

import java.util.List;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

public class PickerActivity extends BaseActivity implements PermissionCallBack, FragmentManager.OnBackStackChangedListener {

    private Handler uiThreadHandler;
    private MemoryCache memoryCache;
    private MediaPickerOpts opts;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);

        setTransition(Gravity.END, GravityCompat.END, GravityCompat.END, GravityCompat.END);

        setContentView(R.layout.activity_main);
        uiThreadHandler = new Handler();

        memoryCache = MemoryCache.getInstance();
        opts = getIntent().getParcelableExtra(MediaPickerOpts.INTENT_OPTS);

        if (opts.mediaType == MediaType.IMAGE) {
            PermissionManager.photoPermission(this, this);
        } else {
            PermissionManager.videoPermission(this, this);
        }

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private void launchCameraFragment() {
        FragmentManager manager = getSupportFragmentManager();

        if (manager.getBackStackEntryCount() == 0) {

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

//    private void testEffectFragment() {
//        FragmentManager manager = getSupportFragmentManager();
//
//        if (manager.getBackStackEntryCount() == 0) {
//            MediaPickerOpts opts = getIntent().getParcelableExtra(MediaPickerOpts.INTENT_OPTS);
//            if (opts == null) {
//                uiThreadHandler.postDelayed(this::supportFinishAfterTransition, 500L);
//                return;
//            }
//
//            // /storage/emulated/0/WhatsApp/Media/WhatsApp Images/IMG-20180312-WA0001.jpg
//            ImageEffectFragment fragment = ImageEffectFragment.newInstance(opts);, "/storage/emulated/0/WhatsApp/Media/WhatsApp Images/IMG-20180312-WA0001.jpg");
//            FragmentTransaction transaction = manager.beginTransaction();
//            transaction.replace(R.id.container, fragment, fragment.TAG);
//            transaction.addToBackStack(fragment.TAG);
//            transaction.commit();
//        }
//    }

    @Override
    public void onBackPressed() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments.size() > 0) {
            Fragment topFrag = fragments.get(fragments.size() - 1);
            if (!topFrag.isRemoving() && topFrag instanceof PickerFragment) {
                if (((PickerFragment) topFrag).onBackPressed()) return;
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
        if (permissionGranted) {
            uiThreadHandler.postDelayed(this::launchCameraFragment, 500L);
        } else {
            uiThreadHandler.postDelayed(this::supportFinishAfterTransition, 500L);
        }
    }

    @Override
    public void onBackStackChanged() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            supportFinishAfterTransition();
        }
    }
}