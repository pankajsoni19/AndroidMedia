package com.pankajsoni19.media.picker;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.AppCompatDelegate;
import android.view.Gravity;
import android.view.View;
import android.view.Window;

import com.pankajsoni19.media.R;
import com.pankajsoni19.media.anim.AnimationHelper;
import com.pankajsoni19.media.base.BaseActivity;
import com.pankajsoni19.media.base.PickerFragment;
import com.pankajsoni19.media.camera.CameraFragment;
import com.pankajsoni19.media.enums.MediaType;
import com.pankajsoni19.media.fileio.MemoryCache;
import com.pankajsoni19.media.permission.PermissionCallBack;
import com.pankajsoni19.media.permission.PermissionManager;

import java.util.List;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

public class PickerActivity extends BaseActivity implements PermissionCallBack, FragmentManager.OnBackStackChangedListener {

    private static final String TAG = "PickerActivity";

    private Handler uiThreadHandler;
    private MemoryCache memoryCache;
    private MediaPickerOpts opts;
    private View container;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);

        setTransition(Gravity.END, GravityCompat.END, GravityCompat.END, GravityCompat.END);

        setContentView(R.layout.activity_main);
        container = findViewById(R.id.container);

        uiThreadHandler = new Handler();

        opts = getIntent().getParcelableExtra(MediaPickerOpts.INTENT_OPTS);

        if (opts == null) {
            uiThreadHandler.postDelayed(this::supportFinishAfterTransition, 500L);
            return;
        }
        memoryCache = null;
        memoryCache = MemoryCache.getInstance();

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        if (opts.mediaType == MediaType.IMAGE) {
            PermissionManager.photoPermission(this, this);
        } else {
            PermissionManager.videoPermission(this, this);
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
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            if (memoryCache != null) memoryCache.clear();

            uiThreadHandler.postDelayed(() -> {
                container.setVisibility(View.GONE);
            }, AnimationHelper.getShortDuration());
        }
    }

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            uiThreadHandler.postDelayed(this::launchCameraFragment, 500L);
        } else {
            uiThreadHandler.postDelayed(this::supportFinishAfterTransition, 500L);
        }
    }

    @Override
    public void onPermissionGranted() {
        uiThreadHandler.postDelayed(this::launchCameraFragment, 500L);
    }

    @Override
    public void onBackStackChanged() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            supportFinishAfterTransition();
        }
    }

    private void launchCameraFragment() {
        FragmentManager manager = getSupportFragmentManager();

        if (manager.getBackStackEntryCount() == 0) {

            CameraFragment fragment = CameraFragment.newInstance(opts);
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.container, fragment, fragment.TAG);
            transaction.addToBackStack(fragment.TAG);
            transaction.commit();
        }
    }
}