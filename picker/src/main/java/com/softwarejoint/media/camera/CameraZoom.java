package com.softwarejoint.media.camera;

import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 01/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class CameraZoom implements View.OnTouchListener {

    private static final String TAG = "CameraZoom";

    private static final int FOCUS_SQR_SIZE = 100;
    private static final int FOCUS_MAX_BOUND = 1000;
    private static final int FOCUS_MIN_BOUND = -FOCUS_MAX_BOUND;

    private static final double ASPECT_RATIO = 3.0 / 4.0;
    private Camera mCamera;

    private float mLastTouchX;
    private float mLastTouchY;

    // For scaling
    private int mMaxZoom;
    //private int mScaleFactor = 1;
    private ScaleGestureDetector mScaleDetector;

    // For focus
    private boolean mIsFocus;
    private volatile boolean mIsFocusReady = false;
    private Camera.Area mFocusArea;
    private ArrayList<Camera.Area> mFocusAreas;
    private CameraHandler mHandler;

    CameraZoom(CameraGLView glView) {
        mScaleDetector = new ScaleGestureDetector(glView.getContext(), new ScaleListener());
        mFocusArea = new Camera.Area(new Rect(), 1000);
        mFocusAreas = new ArrayList<>();
        mFocusAreas.add(mFocusArea);
        glView.setOnTouchListener(this);
    }

    void setCameraHandler(CameraHandler handler) {
        mHandler = handler;
    }

    public void setCamera(Camera camera) {
        mCamera = camera;

        if (camera == null) {
            mIsFocusReady = false;
            return;
        }

        mIsFocusReady = true;

        Camera.Parameters params = camera.getParameters();
        boolean mIsZoomSupported = params.isZoomSupported();

        if (mIsZoomSupported) {
            mMaxZoom = params.getMaxZoom();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!mIsFocusReady) return false;

        //if (isRecording) return true;
        mScaleDetector.onTouchEvent(event);
        final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mIsFocus = true;
                mLastTouchX = event.getX();
                mLastTouchY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                if (mIsFocus) {
                    mHandler.post(this::handleFocus);
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mHandler.post(this::cancelAutoFocus);
                mIsFocus = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }

        return true;
    }

    private void cancelAutoFocus() {
        if (mCamera == null) { return; }
        try { mCamera.cancelAutoFocus(); } catch (Exception ignore) { }
    }

    private void handleFocus() {
        if (mCamera == null) { return; }

        Camera.Parameters params = mCamera.getParameters();

        float x = mLastTouchX;
        float y = mLastTouchY;

        if (!setFocusBound(x, y)) return;

        List<String> supportedFocusModes = params.getSupportedFocusModes();

        if (supportedFocusModes != null &&
                supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            Log.d(TAG, mFocusAreas.size() + "");
            params.setFocusAreas(mFocusAreas);
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            // List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            // Camera.Size cs = sizes.get(0);
            // params.setPreviewSize(cs.width, cs.height);

            mCamera.setParameters(params);
        }
    }

    private boolean setFocusBound(float x, float y) {
        int left = (int) (x - FOCUS_SQR_SIZE / 2);
        int right = (int) (x + FOCUS_SQR_SIZE / 2);
        int top = (int) (y - FOCUS_SQR_SIZE / 2);
        int bottom = (int) (y + FOCUS_SQR_SIZE / 2);

        if (FOCUS_MIN_BOUND > left || left > FOCUS_MAX_BOUND) return false;
        if (FOCUS_MIN_BOUND > right || right > FOCUS_MAX_BOUND) return false;
        if (FOCUS_MIN_BOUND > top || top > FOCUS_MAX_BOUND) return false;
        if (FOCUS_MIN_BOUND > bottom || bottom > FOCUS_MAX_BOUND) return false;

        mFocusArea.rect.set(left, top, right, bottom);

        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        private static final int ZOOM_OUT = 0;
        private static final int ZOOM_IN = 1;
        private static final int ZOOM_DELTA = 1;

        volatile int scaleFactor;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor = (int) detector.getScaleFactor();

            if (mCamera == null) { return false; }

            Camera.Parameters params = mCamera.getParameters();

            int zoom = params.getZoom();
            if (scaleFactor == ZOOM_IN) {
                if (zoom < mMaxZoom) zoom += ZOOM_DELTA;
            } else if (scaleFactor == ZOOM_OUT) {
                if (zoom > 0) zoom -= ZOOM_DELTA;
            }

            params.setZoom(zoom);
            mCamera.setParameters(params);

            return true;
        }
    }
}
