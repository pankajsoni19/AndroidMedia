package com.softwarejoint.media.image;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.softwarejoint.media.R;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 15/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class PathCropView extends View implements View.OnTouchListener {

    private static final String TAG = "PathCropView";

    private static final int INVALID = -1;
    private static final float TOUCH_TOLERANCE = 4;
    private static final float BRUSH_SIZE = 5;
    private static final int MIN_POINTS = 20;

    private Paint mPaint;
    private Path mPath;
    private float mX, mY;

    private float mSx = INVALID;
    private float mSy = INVALID;
    private int points = 0;

    public PathCropView(Context context) {
        this(context, null, 0);
    }

    public PathCropView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PathCropView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialise();
    }

    public boolean clear() {
        mSx = mSy = INVALID;
        points = 0;

        boolean isDrawn = !mPath.isEmpty();
        mPath.reset();

        mPath = new Path();
        invalidate();

        return isDrawn;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(mPath, mPaint);
    }

    private void initialise() {
        if (mPaint == null) {
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setDither(true);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
        }

        mPaint.setColor(ContextCompat.getColor(getContext(), R.color.sketch_color));
        mPaint.setStrokeWidth(BRUSH_SIZE);

        mPath = new Path();

        setOnTouchListener(this);
    }

    private void onTouchStart(float x, float y) {
        if (mSx == INVALID && mSy == INVALID) {
            mX = mSx = x;
            mY = mSy = y;
            points++;
            mPath.moveTo(x, y);
            invalidate();
        } else {
            onTouchMove(x, y);
        }
    }

    private void onTouchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        points++;

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {

            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);

            mX = x;
            mY = y;

            invalidate();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                onTouchStart(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(x, y);
                break;
            default:
                break;
        }

        return true;
    }

    public boolean pathValidForCrop() {
        return mSx != INVALID && mSy != INVALID && mSx != mX && mSy != mY && points >= MIN_POINTS;
    }

    public void completePath() {
        post(() -> {
            mPath.quadTo(mX, mY, mSx, mSy);
            mX = mSx;
            mY = mSy;

            invalidate();
        });
    }

    public Path getPath() {
        return mPath;
    }
}