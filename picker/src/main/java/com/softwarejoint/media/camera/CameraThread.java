package com.softwarejoint.media.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.softwarejoint.media.R;
import com.softwarejoint.media.enums.FlashMode;
import com.softwarejoint.media.enums.MediaType;
import com.softwarejoint.media.utils.CameraHelper;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;
import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;
import static android.hardware.Camera.Parameters.PREVIEW_FPS_MAX_INDEX;
import static android.hardware.Camera.Parameters.PREVIEW_FPS_MIN_INDEX;

/**
 * Thread for asynchronous operation of camera preview
 */
@SuppressWarnings("WeakerAccess")
public final class CameraThread extends Thread {

    private static final String TAG = "CameraThread";

    @SuppressWarnings("unused")
    private static final int MIN_FRAME_RATE = 15000;
    private static final int MAX_FRAME_RATE = 30000;

    private static final int PREFERRED_SIZE = 720;

    private final Object mReadyFence = new Object();

    private final WeakReference<CameraGLView> mWeakParent;
    private CameraHandler mHandler;
    private CameraZoom cameraZoom;

    public volatile boolean mIsRunning = false;
    private volatile @FlashMode
    int mFlashMode = FlashMode.UNAVAILABLE;
    private Camera mCamera;

    public CameraThread(final CameraGLView parent) {
        super("Camera thread");
        mWeakParent = new WeakReference<>(parent);
    }

    public CameraHandler getHandler() {
        synchronized (mReadyFence) {
            try {
                mReadyFence.wait();
            } catch (final InterruptedException ignore) {
            }
        }
        return mHandler;
    }

    /**
     * message loop
     * prepare Looper and create Handler for this thread
     */
    @Override
    public void run() {
        Log.d(TAG, "Camera thread start");

        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new CameraHandler(this);
            cameraZoom = new CameraZoom(mWeakParent.get());
            cameraZoom.setCameraHandler(mHandler);
            mIsRunning = true;
            mReadyFence.notify();
        }
        Looper.loop();

        Log.d(TAG, "Camera thread finish");

        synchronized (mReadyFence) {
            mHandler = null;
            mIsRunning = false;
        }
    }

    public void updateFlashStatus() {
        final CameraGLView parent = mWeakParent.get();
        if (parent == null || parent.flashImageView == null || mCamera == null) {
            return;
        }

        Camera.Parameters parameters = mCamera.getParameters();

        List<String> flashModes = parameters.getSupportedFlashModes();

        if (flashModes == null || flashModes.isEmpty() || !flashModes.contains(FLASH_MODE_TORCH)) {
            mFlashMode = FlashMode.UNAVAILABLE;
        } else if (mFlashMode == FlashMode.UNAVAILABLE) {
            mFlashMode = FlashMode.OFF;
        }

        updateFlashImage(parent);
    }

    public void toggleCamera() {
        final CameraGLView parent = mWeakParent.get();
        if (parent == null || parent.cameraSwitcher == null) {
            return;
        }

        switch (parent.cameraId) {
            case CAMERA_FACING_BACK:
                parent.cameraId = CAMERA_FACING_FRONT;
                break;
            case CAMERA_FACING_FRONT:
                parent.cameraId = CAMERA_FACING_BACK;
                break;
            default:
                break;
        }

        if (updateCameraIcon()) {
            parent.post(parent::restartPreview);
        }
    }

    public void toggleFlash() {
        final CameraGLView parent = mWeakParent.get();
        if (parent == null || parent.flashImageView == null || mCamera == null) {
            return;
        }

        Camera.Parameters parameters = mCamera.getParameters();

        List<String> flashModes = parameters.getSupportedFlashModes();
        if (flashModes == null || flashModes.isEmpty()) {
            return;
        }

        if (mFlashMode == FlashMode.OFF && flashModes.contains(FLASH_MODE_TORCH)) {
            mFlashMode = FlashMode.TORCH;
        } else if (mFlashMode == FlashMode.TORCH) {
            mFlashMode = FlashMode.OFF;
        }

        updateFlashImage(parent);
    }

    public void onRecordingStart() {
        torchOn();
    }

    public void onRecordingStop() {
        torchOff();
    }

    public void torchOn() {
        if (mFlashMode != FlashMode.TORCH || mCamera == null) {
            return;
        }

        Camera.Parameters params = mCamera.getParameters();
        params.setFlashMode(FLASH_MODE_TORCH);
        mCamera.setParameters(params);
    }

    public void torchOff() {
        if (mFlashMode != FlashMode.TORCH || mCamera == null) {
            return;
        }

        Camera.Parameters params = mCamera.getParameters();
        params.setFlashMode(FLASH_MODE_OFF);
        mCamera.setParameters(params);
    }

    public boolean updateCameraIcon() {
        final CameraGLView parent = mWeakParent.get();
        if (parent == null || parent.cameraSwitcher == null) {
            return false;
        }

        parent.post(() -> {
            if (parent.cameraSwitcher == null) {
                return;
            }

            switch (parent.cameraId) {
                case CAMERA_FACING_BACK:
                    parent.cameraSwitcher.setImageResource(R.drawable.camera_front_white);
                    break;
                case CAMERA_FACING_FRONT:
                    parent.cameraSwitcher.setImageResource(R.drawable.camera_rear_white);
                    break;
                default:
                    break;
            }

            parent.cameraSwitcher.setVisibility(View.VISIBLE);
            Log.d(TAG, "mCameraSwitcher: visible: " + " front: " + (parent.cameraId == CAMERA_FACING_FRONT));
        });

        return true;
    }

    public void updateFlashImage(final CameraGLView parent) {
        parent.post(() -> {
            if (parent.flashImageView == null) {
                return;
            }

            switch (mFlashMode) {
                case FlashMode.ON:
                case FlashMode.AUTO:
                case FlashMode.TORCH:
                    parent.flashImageView.setImageResource(R.drawable.flash_on_white);
                    parent.flashImageView.setVisibility(View.VISIBLE);
                    break;
                case FlashMode.OFF:
                    parent.flashImageView.setImageResource(R.drawable.flash_off_white);
                    parent.flashImageView.setVisibility(View.VISIBLE);
                    break;
                case FlashMode.UNAVAILABLE:
                    parent.flashImageView.setVisibility(View.INVISIBLE);
                    break;
            }
        });
    }

    /**
     * start camera preview
     */
    public void startPreview(int reqWidth, int reqHeight) {
        Log.d(TAG, "startPreview:");

        final CameraGLView parent = mWeakParent.get();

        if ((parent == null) || (mCamera != null)) {
            return;
        }

        // This is a sample project so just use 0 as camera ID.
        // it is better to selecting camera is available
        try {
            final boolean isVideo = parent.mMediaType == MediaType.VIDEO;

            mCamera = Camera.open(parent.cameraId);
            final Camera.Parameters params = mCamera.getParameters();

            List<String> focusModes = params.getSupportedFocusModes();

            if (focusModes != null) {
                if (isVideo && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                } else if (!isVideo && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
            }

            if (isVideo) {
                final List<int[]> supportedFpsRange = params.getSupportedPreviewFpsRange();

                if (supportedFpsRange != null) {
                    final int n = supportedFpsRange.size();

                    for (int i = 0; i < n; i++) {

                        final int range[] = supportedFpsRange.get(i);

                        final int minFPS = range[PREVIEW_FPS_MIN_INDEX];
                        final int maxFPS = range[PREVIEW_FPS_MAX_INDEX];

                        Log.d(TAG, String.format("supportedFpsRange(%d)=(%d,%d)", i, minFPS, maxFPS));

                        //if (range[0] >= MIN_FRAME_RATE && range[1] >= MAX_FRAME_RATE) {
                        if (maxFPS >= MAX_FRAME_RATE) {
                            params.setPreviewFpsRange(minFPS, maxFPS);
                            break;
                        }
                    }
                }

                params.setRecordingHint(true);
            }

            //TODO: video exposure
            if (params.isVideoStabilizationSupported()) {
                params.setVideoStabilization(true);
            }

            //exposure
            int maxExposure = params.getMaxExposureCompensation();
            int minExposure = params.getMinExposureCompensation();
            float step = params.getExposureCompensationStep();

            if ((minExposure < 0 || maxExposure > 0) && step > 0.0f) {
                int compensation = Math.max(1, maxExposure / 2);
                Log.d(TAG, "setExposureCompensation: " + compensation);
                params.setExposureCompensation(compensation);
            }

            // request preview size
            // this is a sample project and just use fixed value
            // if you want to use other size, you also need to change the recording size.
            Log.d(TAG, "requested: width: " + reqWidth + " height: " + reqHeight);

            final Camera.Size closestSize = CameraHelper.getOptimalSize(
                    params.getSupportedPreviewSizes(), reqWidth, reqHeight);

            if (closestSize != null) {
                params.setPreviewSize(closestSize.width, closestSize.height);
                Log.d(TAG, String.format("closestSize(%d, %d)", closestSize.width, closestSize.height));
            }

            final Camera.Size pictureSize = CameraHelper.getOptimalSize(
                    params.getSupportedPictureSizes(), reqWidth, reqHeight);

            if (pictureSize != null) {
                params.setPictureSize(pictureSize.width, pictureSize.height);
                Log.d(TAG, String.format("pictureSize(%d, %d)", pictureSize.width, pictureSize.height));
            }

            final int degrees = CameraHelper.getDisplayOrientation(parent.getContext(), parent.cameraId);
            mCamera.setDisplayOrientation(degrees);

            // apply rotation setting
            parent.mRotation = degrees;

            mCamera.setParameters(params);

            final Camera.Size previewSize = mCamera.getParameters().getPreviewSize();

            if (previewSize != null) {
                Log.d(TAG, String.format("previewSize(%d, %d)", previewSize.width, previewSize.height));

                // adjust view size with keeping the aspect ration of camera preview.
                // here is not a UI thread and we should request parent view to execute.
                parent.post(() -> parent.setCameraPreviewSize(previewSize.width, previewSize.height));
            }

            final SurfaceTexture st = parent.getSurfaceTexture();
            //noinspection ConstantConditions
            st.setDefaultBufferSize(previewSize.width, previewSize.height);
            mCamera.setPreviewTexture(st);
        } catch (final IOException | RuntimeException e) {
            Log.e(TAG, "startPreview:", e);
            cameraZoom.setCamera(null);

            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }
        }

        if (mCamera != null) {
            // start camera preview display
            mCamera.startPreview();
            cameraZoom.setCamera(mCamera);
        }
    }

    /**
     * stop camera preview
     */
    public void stopPreview() {
        Log.d(TAG, "stopPreview:");

        cameraZoom.setCamera(null);

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        final CameraGLView parent = mWeakParent.get();
        if (parent == null) return;
        parent.mCameraHandler = null;
    }
}