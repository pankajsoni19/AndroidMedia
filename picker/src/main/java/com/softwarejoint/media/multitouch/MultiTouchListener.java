package com.softwarejoint.media.multitouch;

import android.content.Context;
import android.graphics.RectF;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ScaleGestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * Copied from https://github.com/thuytrinh/android-collage-views
 */

public class MultiTouchListener implements OnTouchListener, GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener {

    private static final String TAG = "MultiTouchListener";

    private static final float BASE_SCALE = 1.0f;
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 3.0f;

    private final GestureDetectorCompat gestureDetector;
    private final ScaleGestureDetector scaleGestureDetector;

    private View view;
    private RectF mCurrentViewport;

    private float mLastAngle = 0;
    private float translationX = 0;
    private float translationY = 0;
    private float scale = BASE_SCALE;

    private boolean translateEnabled = true;
    private boolean scaleEnabled = true;
    private boolean rotateEnabled = true;

    public MultiTouchListener(Context context) {
        gestureDetector = new GestureDetectorCompat(context, this);
        gestureDetector.setIsLongpressEnabled(false);

        scaleGestureDetector = new ScaleGestureDetector(context, this);
        ScaleGestureDetectorCompat.setQuickScaleEnabled(scaleGestureDetector, false);
    }

    @SuppressWarnings("unused")
    public void setTranslateEnabled(boolean enabled) {
        translateEnabled = enabled;
    }

    @SuppressWarnings("unused")
    public void setScaleEnabled(boolean enabled) {
        scaleEnabled = enabled;
    }

    @SuppressWarnings("unused")
    public void setRotateEnabled(boolean enabled) {
        rotateEnabled = enabled;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            view = v;
            mCurrentViewport = new RectF(0, 0, view.getWidth(), view.getHeight());
        }

        scaleGestureDetector.onTouchEvent(event);

        if (!scaleGestureDetector.isInProgress()) {
            gestureDetector.onTouchEvent(event);
        }

        if (rotateEnabled && event.getPointerCount() == 2 && event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            doRotation(event);
        }

        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.d(TAG, "onSingleTapUp: ");
        view.performClick();
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (!translateEnabled) return false;

        float newTransX = translationX + (-distanceX);
        float newTransY = translationY + distanceY;

        float newX = mCurrentViewport.left + newTransX;
        float newY = mCurrentViewport.top + newTransY;

//        Log.d(TAG, "onScroll: dX: " + distanceX +
//                " transX: " + translationX +
//                " newTransX: " + newTransX +
//                " newX: " + newX);

        if (Math.abs(newX) < mCurrentViewport.width()) {
            translationX = newTransX;
            view.setTranslationX(distanceX);
        }

        if (Math.abs(newY) < mCurrentViewport.height()) {
            translationY = newTransY;
            view.setTranslationY(distanceY);
        }

        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    /**
     * Scale Gesture
     */

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (scaleEnabled) {
            float scaleFactor = detector.getScaleFactor();
            float totalScale = scale + scaleFactor - 1.0f;

            if (scale != totalScale && totalScale > MIN_SCALE && totalScale < MAX_SCALE) {
                //Log.d(TAG, "scaleFactor: " + scaleFactor + " totalScale: " + scale);
                scale = totalScale;
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
            }
        }

        return scaleEnabled;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return scaleEnabled;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
    }

    private void doRotation(MotionEvent event) {
        float deltaX = event.getX(0) - event.getX(1);
        float deltaY = event.getY(0) - event.getY(1);
        float degrees = (float) Math.toDegrees(Math.atan(deltaY / deltaX));

        float deltaRot = degrees - mLastAngle;

        if (mLastAngle == 0 && Math.abs(deltaRot) > 5) {
            deltaRot = 5;
        }

        if (deltaRot >= 160) {
            // Going CCW across the boundary
            Log.d(TAG, "mLastAngle: " + mLastAngle + " degrees: " + degrees + " deltaRot: " + deltaRot + " rot: " + (180 - deltaRot));
            view.setRotation(180 - deltaRot);
        } else if (deltaRot < -160) {
            // Going CW across the boundary
            Log.d(TAG, "mLastAngle: " + mLastAngle + " degrees: " + degrees + " deltaRot: " + deltaRot);
            view.setRotation(5);
        } else {
            view.setRotation(deltaRot);
        }

        mLastAngle = degrees;
    }
}