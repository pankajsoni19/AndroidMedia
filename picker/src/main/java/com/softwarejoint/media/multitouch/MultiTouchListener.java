package com.softwarejoint.media.multitouch;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * Copied from https://github.com/thuytrinh/android-collage-views
 */

public class MultiTouchListener implements OnTouchListener {

    private static final String TAG = "MultiTouchListener";

    private static final int INVALID_POINTER_ID = -1;

    private final ScaleGestureDetector mScaleGestureDetector;
    private boolean isRotateEnabled;
    private boolean isTranslateEnabled;
    private boolean isTranslateXEnabled;
    private boolean isScaleEnabled;
    private int mActivePointerId = INVALID_POINTER_ID;
    private float mPrevX;
    private float mPrevY;
    private boolean isViewTouched;

    public MultiTouchListener() {
        mScaleGestureDetector = new ScaleGestureDetector(new ScaleGestureListener());
    }

    private static float adjustAngle(float degrees) {
        if (degrees > 180.0f) {
            degrees -= 360.0f;
        } else if (degrees < -180.0f) {
            degrees += 360.0f;
        }

        return degrees;
    }

    private static void computeRenderOffset(View view, float pivotX, float pivotY) {
        if (view.getPivotX() == pivotX && view.getPivotY() == pivotY) {
            return;
        }

        float[] prevPoint = {0.0f, 0.0f};
        view.getMatrix().mapPoints(prevPoint);

        view.setPivotX(pivotX);
        view.setPivotY(pivotY);

        float[] currPoint = {0.0f, 0.0f};
        view.getMatrix().mapPoints(currPoint);

        float offsetX = currPoint[0] - prevPoint[0];
        float offsetY = currPoint[1] - prevPoint[1];

        view.setTranslationX(-offsetX);
        view.setTranslationY(-offsetY);
    }

    private void move(View view, TransformInfo info) {
        computeRenderOffset(view, info.pivotX, info.pivotY);
        adjustTranslation(view, info.deltaX, info.deltaY);

        // Assume that scaling still maintains aspect ratio.

        float scale = view.getScaleX() * info.deltaScale;
        scale = Math.max(info.minimumScale, Math.min(info.maximumScale, scale));
        view.setScaleX(scale);
        view.setScaleY(scale);

        Log.d(TAG, "deltaScale: " + info.deltaScale + " viewrot: " + view.getScaleX());

        float rotation = adjustAngle(info.deltaAngle);
        view.setRotation(rotation);
    }

    private void adjustTranslation(View view, float deltaX, float deltaY) {
        float[] deltaVector = {deltaX, deltaY};
        view.getMatrix().mapVectors(deltaVector);
        if (isScaleEnabled || isTranslateXEnabled) {
            view.setTranslationX(deltaVector[0]);
        }
        view.setTranslationY(deltaVector[1]);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        boolean handled = mScaleGestureDetector.onTouchEvent(view, event);

        isViewTouched = isViewTouched || handled;

        if (!isTranslateEnabled) { return handled; }

        isViewTouched = true;

        int action = event.getAction();
        switch (action & event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {

                mPrevX = view.getX() - event.getRawX();
                mPrevY = view.getY() - event.getRawY();
                mPrevX = event.getX();
                mPrevY = event.getY();

                // Save the ID of this pointer.
                mActivePointerId = event.getPointerId(0);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                // Find the index of the active pointer and fetch its position.
                int pointerIndex = event.findPointerIndex(mActivePointerId);
                if (pointerIndex != -1) {
                    float currX = event.getX(pointerIndex);
                    float currY = event.getY(pointerIndex);

                    // Only move if the ScaleGestureDetector isn't processing a
                    // gesture.
                    if (!mScaleGestureDetector.isInProgress()) {
                        adjustTranslation(view, currX - mPrevX, currY - mPrevY);
                    }
                }

                break;
            }

            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = INVALID_POINTER_ID;
                break;

            case MotionEvent.ACTION_UP:
                mActivePointerId = INVALID_POINTER_ID;
                break;

            case MotionEvent.ACTION_POINTER_UP: {
                // Extract the index of the pointer that left the touch sensor.
                int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mPrevX = event.getX(newPointerIndex);
                    mPrevY = event.getY(newPointerIndex);
                    mActivePointerId = event.getPointerId(newPointerIndex);
                }

                break;
            }
        }

        return true;
    }

    public void setIsRotateEnabled(boolean enabled) {
        isRotateEnabled = enabled;
    }

    public void setIsTranslateEnabled(boolean enabled) {
        isTranslateEnabled = enabled;
    }

    public void setIsScaleEnabled(boolean enabled) {
        isScaleEnabled = enabled;
    }

    public void setIsTranslationXEnabled(boolean enabled) {
        isTranslateXEnabled = enabled;
    }

    private class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        private final Vector2D mPrevSpanVector = new Vector2D();
        private float mPivotX;
        private float mPivotY;
        private float initRotation;

        @Override
        public boolean onScaleBegin(View view, ScaleGestureDetector detector) {
            mPivotX = detector.getFocusX();
            mPivotY = detector.getFocusY();
            mPrevSpanVector.set(detector.getCurrentSpanVector());
            initRotation = view.getRotation();
            return true;
        }

        @Override
        public boolean onScale(View view, ScaleGestureDetector detector) {
            TransformInfo info = new TransformInfo();

            if (isScaleEnabled) {
                info.deltaScale = detector.getScaleFactor();
            }

            if (isRotateEnabled) {
                info.deltaAngle = Vector2D.getAngle(mPrevSpanVector, detector.getCurrentSpanVector()) + initRotation;
            }

            info.deltaX = isTranslateEnabled ? detector.getFocusX() - mPivotX : 0.0f;
            info.deltaY = isTranslateEnabled ? detector.getFocusY() - mPivotY : 0.0f;
            info.pivotX = mPivotX;
            info.pivotY = mPivotY;

            move(view, info);

            return false;
        }

        @Override
        public void onScaleEnd(View view, ScaleGestureDetector detector) {
            super.onScaleEnd(view, detector);
        }
    }

    private class TransformInfo {
        float deltaX;
        float deltaY;
        float deltaScale = 1.0f;
        float deltaAngle = 0.0f;
        float pivotX;
        float pivotY;
        float minimumScale = 0.5f;
        float maximumScale = 3.0f;
    }
}