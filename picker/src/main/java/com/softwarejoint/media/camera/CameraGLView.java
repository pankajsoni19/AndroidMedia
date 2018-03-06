package com.softwarejoint.media.camera;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: CameraGLView.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.support.v7.app.AppCompatDelegate;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageView;

import com.softwarejoint.media.encoder.MediaVideoEncoder;
import com.softwarejoint.media.enums.ScaleType;
import com.softwarejoint.media.glutils.GL1977Filter;
import com.softwarejoint.media.glutils.GLArtFilter;
import com.softwarejoint.media.glutils.GLColorInvertFilter;
import com.softwarejoint.media.glutils.GLDrawer2D;
import com.softwarejoint.media.glutils.GLGrayscaleFilter;
import com.softwarejoint.media.glutils.GLPosterizeFilter;
import com.softwarejoint.media.utils.CameraHelper;
import com.softwarejoint.media.utils.Constants;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;

/**
 * Sub class of GLSurfaceView to display camera preview and write video frame to capturing surface
 */
@SuppressWarnings("WeakerAccess")
public final class CameraGLView extends GLSurfaceView {

    private static final String TAG = "CameraGLView";

    protected final CameraSurfaceRenderer mRenderer;
    protected boolean mHasSurface;
    protected CameraHandler mCameraHandler = null;
    protected int mVideoWidth, mVideoHeight;
    protected volatile int mRotation;

    protected boolean isFlashAvailable = false;
    protected boolean isFrontCameraAvailable = false;

    protected ImageView flashImageView, cameraSwitcher;

    protected @ScaleType int mScaleType = ScaleType.SCALE_SQUARE;
    protected volatile int cameraId = CAMERA_FACING_BACK;
    private boolean filtersPreviewEnabled;

    private GLDrawer2D mDrawer = new GLDrawer2D();

    public CameraGLView(final Context context) {
        this(context, null, 0);
    }

    public CameraGLView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraGLView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs);
        mRenderer = new CameraSurfaceRenderer(this, mDrawer);
        setEGLContextClientVersion(2);
        setRenderer(mRenderer);
/*		// the frequency of refreshing of camera preview is at most 15 fps
    // and RENDERMODE_WHEN_DIRTY is better to reduce power consumption
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); */

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        isFlashAvailable = CameraHelper.isFlashAvailable(context);
        isFrontCameraAvailable = CameraHelper.isFrontCameraAvailable(context);

        cameraId = isFrontCameraAvailable ? CAMERA_FACING_FRONT : CAMERA_FACING_BACK;
    }

    public void onRecordingStart() {
        if (mCameraHandler != null) {
            mCameraHandler.onRecordingStart();
        }
    }

    public void onRecordingStop() {
        if (mCameraHandler != null) {
            mCameraHandler.onRecordingStop();
        }
    }

    public void setFlashImageView(ImageView imageView) {
        flashImageView = imageView;
        if (mCameraHandler != null && isFlashAvailable) {
            mCameraHandler.updateFlashStatus();
        } else {
            flashImageView.setVisibility(View.INVISIBLE);
        }
    }

    public void setCameraSwitcher(ImageView imageView) {
        cameraSwitcher = imageView;
        if (mCameraHandler != null && isFrontCameraAvailable) {
            mCameraHandler.updateCameraIcon();
        } else {
            cameraSwitcher.setVisibility(View.INVISIBLE);
            Log.d(TAG, "mCameraSwitcher: INVISIBLE: ");
        }
    }

    public void toggleFlash() {
        if (mCameraHandler != null) {
            mCameraHandler.toggleFlash();
        }
    }

    public void toggleCamera() {
        if (mCameraHandler != null) {
            mCameraHandler.toggleCamera();
        }
    }

    public boolean toggleShowFilters() {
        boolean showFilters = mRenderer.showFilters;
        queueEvent(mRenderer::toggleShowFilters);
        return !showFilters;
    }

    public boolean isFiltersPreviewVisible() {
        return mRenderer.showFilters;
    }

    public GLDrawer2D getDrawer() {
        return mDrawer;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (!filtersPreviewEnabled) return super.dispatchTouchEvent(event);

        Log.d(TAG, "dispatchTouchEvent");

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                final int x = (int) event.getX();
                final int y = (int) event.getY();
                queueEvent(() -> mRenderer.onTouched(x, y));
                break;
            case MotionEvent.ACTION_UP:
                break;
        }

        return true;
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume:");
        super.onResume();
        if (mHasSurface && mCameraHandler == null) {
            Log.d(TAG, "surface already exist");
            startPreview(getWidth(), getHeight());
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause:");
        if (mCameraHandler != null) {
            // just request stop previewing
            mCameraHandler.forceTorchOff();
            mCameraHandler.stopPreview(false);
        }

        flashImageView = null;
        cameraSwitcher = null;
        super.onPause();
    }

    public void restartPreview() {
        if (mCameraHandler != null) {
            // wait for finish previewing here
            // otherwise camera try to display on un-exist Surface and some error will occurs
            mCameraHandler.stopPreview(true);
        }
        startPreview(getWidth(), getHeight());
    }

    public @ScaleType int getScaleType() {
        return mScaleType;
    }

    public void updateScaleType() {
        queueEvent(mRenderer::updateViewport);
    }

    public void setScaleType(@ScaleType final int type) {
        if (mScaleType != type) {
            mScaleType = type;
            queueEvent(mRenderer::updateViewport);
        }
    }

    public void setCameraPreviewSize() {
        setCameraPreviewSize(Constants.PREFERRED_PREVIEW_WIDTH, Constants.PREFERRED_PREVIEW_HEIGHT);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public void setCameraPreviewSize(final int width, final int height) {
        if ((mRotation % 180) == 0) {
            mVideoWidth = width;
            mVideoHeight = height;
        } else {
            mVideoWidth = height;
            mVideoHeight = width;
        }

        Log.d(TAG, "setCameraPreviewSize: width: " + width + " height: " + height);
        queueEvent(mRenderer::updateFiltersUI);
        queueEvent(mRenderer::updateViewport);
    }

    public int getVideoWidth() {
        return mVideoWidth;
    }

    public int getVideoHeight() {
        return mVideoHeight;
    }

    public SurfaceTexture getSurfaceTexture() {
        Log.d(TAG, "getSurfaceTexture:");
        return mRenderer != null ? mRenderer.mSTexture : null;
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed:");
        if (mCameraHandler != null) {
            // wait for finish previewing here
            // otherwise camera try to display on un-exist Surface and some error will occure
            mCameraHandler.stopPreview(true);
        }
        mCameraHandler = null;
        mHasSurface = false;
        queueEvent(mRenderer::onSurfaceDestroyed);

        super.surfaceDestroyed(holder);
    }

    public void setVideoEncoder(final MediaVideoEncoder encoder) {
        Log.d(TAG, "setVideoEncoder:tex_id=" + mRenderer.mGLTextureId + ",encoder=" + encoder);
        queueEvent(() -> {
            synchronized (mRenderer) {
                if (encoder != null) {
                    encoder.setEglContext(EGL14.eglGetCurrentContext(), mRenderer.mGLTextureId);
                }
                mRenderer.mVideoEncoder = encoder;
            }
        });
    }

    //********************************************************************************
    //********************************************************************************
    private synchronized void startPreview(final int width, final int height) {
        if (width == 0 || height == 0) {
            return;
        }
        if (mCameraHandler == null) {
            final CameraThread thread = new CameraThread(this);
            thread.start();
            mCameraHandler = thread.getHandler();
        }
        mCameraHandler.startPreview(width, height);
    }

    public void setPreviewEnabled(boolean enabled) {
        filtersPreviewEnabled = enabled;
        queueEvent(() -> mRenderer.setFilterPreviewEnabled(enabled));
    }

    /**
     * GLSurfaceViewのRenderer
     * NOTE: Filter previews only support portrait mode
     * //TODO: support landscape for previews
     */
    private static final class CameraSurfaceRenderer
            implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {  // API >= 11

        private static final int FILTERED_PREVIEW_SIZE = 96;
        private static final int FILTER_PREVIEWS_PER_ROW = 3;

        private final WeakReference<CameraGLView> mWeakParent;
        private final float[] mStMatrix = new float[16];
        private final float[] mMvpMatrix = new float[16];
        private final Queue<Runnable> mRunOnDraw = new LinkedList<>();
        private SurfaceTexture mSTexture;  // API >= 11
        private int mGLTextureId;
        private GLDrawer2D mDrawer;
        private MediaVideoEncoder mVideoEncoder;
        private volatile boolean requestUpdateTex = false;
        private boolean flip = true;

        private volatile boolean showFilters = false;
        private List<GLDrawer2D> filterPreviews = new ArrayList<>();
        private int screenHeight, screenWidth;
        private int filterPreviewSize;
        private int filterStartX, filterStartY;
        private int margin;
        private boolean filterPreviewEnabled = true;

        public CameraSurfaceRenderer(final CameraGLView parent, GLDrawer2D drawer) {
            Log.d(TAG, "CameraSurfaceRenderer:");
            mWeakParent = new WeakReference<>(parent);
            Matrix.setIdentityM(mMvpMatrix, 0);
            mDrawer = drawer;

            DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();

            screenHeight = displayMetrics.heightPixels;
            screenWidth = displayMetrics.widthPixels;

            filterPreviewSize = (int) (FILTERED_PREVIEW_SIZE * displayMetrics.density);

            margin = (screenWidth - (filterPreviewSize * FILTER_PREVIEWS_PER_ROW))/(FILTER_PREVIEWS_PER_ROW + 1);

            filterStartX = margin;
            filterStartY = (int) (0.50 * screenHeight);
        }

        private void setFilterPreviewEnabled(boolean enabled) {
            if (enabled) {
                for (GLDrawer2D drawer: filterPreviews) {
                    drawer.release();
                    filterPreviews.clear();
                }
            }
        }

        public void onTouched(int x, int y) {
            Rect rect = new Rect();
            Log.d(TAG, "onTouched: rect: " + rect);

            for (GLDrawer2D filter: filterPreviews) {
                Rect openGLRect = filter.getRect();
                final int filterTop = (screenHeight - openGLRect.top - filterPreviewSize);

                Rect viewRect = new Rect(openGLRect.left, filterTop, openGLRect.left + filterPreviewSize, filterTop + filterPreviewSize);
                if (viewRect.contains(x, y)) {
                    onFilterSelected(filter);
                }
            }
        }

        private void onFilterSelected(GLDrawer2D filter) {
            Log.d(TAG, "onFilterSelected : " + filter.getClass().getSimpleName());

            runOnDraw(() -> {
                filterPreviews.add(0, mDrawer);
                filterPreviews.remove(filter);

                mDrawer = filter;

                CameraGLView parent = mWeakParent.get();

                if (parent != null) {
                    parent.updateScaleType();
                }
            });
        }

        public void createFilters() {
            mDrawer.init();
            mDrawer.setMatrix(mMvpMatrix, 0);

            if (!filterPreviewEnabled) return;
            Log.d(TAG, "createFilterPreviews");
            addFilter(new GLPosterizeFilter());
            addFilter(new GLGrayscaleFilter());
            addFilter(new GLArtFilter());
            addFilter(new GL1977Filter());
            addFilter(new GLColorInvertFilter());
        }

        public void addFilter(GLDrawer2D drawer) {
            drawer.init();
            drawer.setMatrix(mMvpMatrix, 0);
            filterPreviews.add(drawer);
        }

        protected void runOnDraw(final Runnable runnable) {
            synchronized (mRunOnDraw) {
                mRunOnDraw.add(runnable);
            }
        }

        private void runAll(Queue<Runnable> queue) {
            synchronized (queue) {
                while (!queue.isEmpty()) {
                    queue.poll().run();
                }
            }
        }

        @Override
        public void onSurfaceCreated(final GL10 unused, final EGLConfig config) {
            Log.d(TAG, "onSurfaceCreated:");
            // This renderer required OES_EGL_image_external extension
            final String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);  // API >= 8
            //			if (DEBUG) Log.i(TAG, "onSurfaceCreated:Gl extensions: " + extensions);
            if (!extensions.contains("OES_EGL_image_external")) {
                throw new RuntimeException("This system does not support OES_EGL_image_external.");
            }

            if (mGLTextureId > 0) {
                GLDrawer2D.deleteTex(mGLTextureId);
            }

            // create texture ID
            mGLTextureId = GLDrawer2D.initTex();
            // create SurfaceTexture with texture ID.
            mSTexture = new SurfaceTexture(mGLTextureId);
            mSTexture.setOnFrameAvailableListener(this);
            //TODO: clear screen with yellow color so that you can see rendering rectangle
            GLES20.glClearColor(0.00f, 0.00f, 0.00f, 1.0f);
            final CameraGLView parent = mWeakParent.get();

            if (parent != null) {
                parent.mHasSurface = true;
            }

            createFilters();
        }

        @Override
        public void onSurfaceChanged(final GL10 unused, final int width, final int height) {
            Log.d(TAG, String.format("onSurfaceChanged:(%d,%d)", width, height));
            // if at least with or height is zero, initialization of this view is still progress.
            if (width == 0 || height == 0) {
                return;
            }

            updateFiltersUI();
            updateViewport();

            final CameraGLView parent = mWeakParent.get();
            if (parent != null) {
                parent.startPreview(width, height);
            }
        }

        /**
         * when GLSurface context is soon destroyed
         */
        public void onSurfaceDestroyed() {
            Log.d(TAG, "onSurfaceDestroyed:");
            if (mDrawer != null) {
                mDrawer.release();
            }

            for (GLDrawer2D filter: filterPreviews) {
                filter.release();
            }

            filterPreviews.clear();

            if (mSTexture != null) {
                mSTexture.release();
                mSTexture = null;
            }

            GLDrawer2D.deleteTex(mGLTextureId);
        }

        private void updateViewport() {
            final CameraGLView parent = mWeakParent.get();
            if (parent == null) return;

            final int view_width = parent.getWidth();
            final int view_height = parent.getHeight();

            final double video_width = parent.mVideoWidth;
            final double video_height = parent.mVideoHeight;

            if (view_width == 0 || view_height == 0 || video_width == 0 || video_height == 0) {
                Log.e(TAG, "updateViewport: view: width: " + view_width + " height: " + view_height + " video: width: " + view_width + " height: " + view_height);
                return;
            }

            GLES20.glViewport(0, 0, view_width, view_height);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            Matrix.setIdentityM(mMvpMatrix, 0);
            final double view_aspect = view_width / (double) view_height;

            Log.i(TAG, String.format("updateViewport view: (%d,%d) view_aspect: %f,video: (%1.0f,%1.0f)",
                    view_width, view_height, view_aspect, video_width, video_height));

            mDrawer.setRect(0, 0, view_width, view_height);

            switch (parent.mScaleType) {
                case ScaleType.SCALE_STRETCH_FIT:
                    break;
                case ScaleType.SCALE_KEEP_ASPECT_VIEWPORT: {
                    final double req = video_width / video_height;
                    int x, y;
                    int width, height;
                    if (view_aspect > req) {
                        // if view is wider than camera image, calc width of drawing area based on view height
                        y = 0;
                        height = view_height;
                        width = (int) (req * view_height);
                        x = (view_width - width) / 2;
                    } else {
                        // if view is higher than camera image, calc height of drawing area based on view width
                        x = 0;
                        width = view_width;
                        height = (int) (view_width / req);
                        y = (view_height - height) / 2;
                    }
                    // set viewport to draw keeping aspect ration of camera image
                    Log.d(TAG, String.format("xy(%d,%d),size(%d,%d)", x, y, width, height));

                    GLES20.glViewport(x, y, width, height);
                    break;
                }
                case ScaleType.SCALE_KEEP_ASPECT:
                case ScaleType.SCALE_CROP_CENTER: {
                    final double scale_x = view_width / video_width;
                    final double scale_y = view_height / video_height;
                    final double scale =
                            (parent.mScaleType == ScaleType.SCALE_CROP_CENTER ? Math.max(scale_x, scale_y)
                                    : Math.min(scale_x, scale_y));
                    final double width = scale * video_width;
                    final double height = scale * video_height;

                    Log.d(TAG,
                            String.format("size(%1.0f,%1.0f),scale(%f,%f),mat(%f,%f)", width, height, scale_x,
                                    scale_y, width / view_width, height / view_height));

                    Matrix.scaleM(mMvpMatrix, 0, (float) (width / view_width),
                            (float) (height / view_height), 1.0f);
                    break;
                }
                case ScaleType.SCALE_SQUARE: {
                    int view_x = 0;
                    int view_y = 0;
                    float scale_x = 1;
                    float scale_y = 1;
                    final int newPreviewSize;

                    if (view_width >= view_height) {
                        newPreviewSize = view_height;
                        view_x = (view_width - newPreviewSize) / 2;

                    } else {
                        newPreviewSize = view_width;
                        view_y = (view_height - newPreviewSize) / 2;
                    }

                    final float video_aspect = (float) (video_width / video_height);
                    if (video_aspect >= 1) {
                        scale_x = video_aspect;
                    } else {
                        scale_y = 1 / video_aspect;
                    }
                    Log.v(TAG, "scale square: " + scale_x + " " + scale_y + " (x,y) view_x : " + view_x + " view_y : " + view_y);

                    GLES20.glViewport(view_x, view_y, newPreviewSize, newPreviewSize);
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                    mDrawer.setRect(view_x, view_y, newPreviewSize, newPreviewSize);
                    Matrix.scaleM(mMvpMatrix, 0, scale_x, scale_y, 1.0f);
                    break;
                }
            }

            if (mDrawer != null) {
                mDrawer.setMatrix(mMvpMatrix, 0);
            }
        }

        private void updateFiltersUI() {
            final CameraGLView parent = mWeakParent.get();
            if (parent == null) return;

            final double video_width = parent.mVideoWidth;
            final double video_height = parent.mVideoHeight;

            if (video_width == 0 || video_height == 0) {
                Log.e(TAG, "updateFiltersUI: " + video_width + " videoheight: " + video_height);
                return;
            }

            final double scale_x = filterPreviewSize / video_width;
            final double scale_y = filterPreviewSize / video_height;
            final double scale = Math.max(scale_x, scale_y);

            final double width = scale * video_width;
            final double height = scale * video_height;

            int startX = filterStartX;
            int startY = filterStartY;

            for (GLDrawer2D drawer: filterPreviews) {
                drawer.setRect(startX, startY, filterPreviewSize, filterPreviewSize);

                Matrix.setIdentityM(mMvpMatrix, 0);
                Matrix.scaleM(mMvpMatrix, 0,
                        (float) (width / filterPreviewSize),
                        (float) (height / filterPreviewSize),
                        1.0f);

                drawer.setMatrix(mMvpMatrix, 0);

                Log.v(TAG, "updateFiltersUI: scale: " + scale_x + " " + scale_y + " x,y: X:" + startX + " Y:" + startY + " size: " + filterPreviewSize);

                startX = startX + filterPreviewSize + margin;

                if (screenWidth < (startX + filterPreviewSize + margin)) {
                    startX = margin;
                    startY = startY - filterPreviewSize - margin;   //move down
                }
            }
        }

        private void toggleShowFilters() {
            showFilters = !showFilters;
            if (!showFilters) {
                runOnDraw(this::updateViewport);
            }
        }

        /**
         * drawing to GLSurface
         * we set renderMode to GLSurfaceView.RENDERMODE_WHEN_DIRTY,
         * this method is only called when #requestRender is called(= when texture is required to
         * update)
         * if you don't set RENDERMODE_WHEN_DIRTY, this method is called at maximum 60fps
         */
        @Override
        public void onDrawFrame(final GL10 unused) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            if (requestUpdateTex) {
                requestUpdateTex = false;
                // update texture(came from camera)
                mSTexture.updateTexImage();
                // get texture matrix
                mSTexture.getTransformMatrix(mStMatrix);
            }

            runAll(mRunOnDraw);

            if (showFilters) {

                GLES20.glViewport(mDrawer.getStartX(), mDrawer.getStartY(), mDrawer.width(), mDrawer.height());
                mDrawer.draw(mGLTextureId, mStMatrix);

                for (GLDrawer2D drawer: filterPreviews) {
                    GLES20.glViewport(drawer.getStartX(), drawer.getStartY(), filterPreviewSize, filterPreviewSize);
                    drawer.draw(mGLTextureId, mStMatrix);
                }

            } else {
                // draw to preview screen
                mDrawer.draw(mGLTextureId, mStMatrix);

                flip = !flip;
                if (flip) {  // ~30fps
                    synchronized (this) {
                        if (mVideoEncoder != null) {
                            // notify to capturing thread that the camera frame is available.
                            mVideoEncoder.frameAvailableSoon(mStMatrix, mMvpMatrix);
                        }
                    }
                }
            }
        }

        @Override
        public void onFrameAvailable(final SurfaceTexture st) {
            requestUpdateTex = true;
            //			final CameraGLView parent = mWeakParent.get();
            //			if (parent != null)
            //				parent.requestRender();
        }
    }

    /**
     * Handler class for asynchronous camera operation
     */



}