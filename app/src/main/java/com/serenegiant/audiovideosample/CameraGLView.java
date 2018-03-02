package com.serenegiant.audiovideosample;
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
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.support.v7.app.AppCompatDelegate;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageView;

import com.serenegiant.encoder.MediaVideoEncoder;
import com.serenegiant.enums.ScaleType;
import com.serenegiant.glutils.GLDrawer2D;
import com.serenegiant.utils.CameraHelper;
import com.serenegiant.utils.Constants;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
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
    protected int mRotation;

    protected boolean isFlashAvailable = false;
    protected boolean isFrontCameraAvailable = false;

    protected ImageView flashImageView, cameraSwitcher;

    protected @ScaleType int mScaleType = ScaleType.SCALE_SQUARE;
    protected volatile int cameraId = CAMERA_FACING_BACK;

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

    public GLDrawer2D getDrawer() {
        return mDrawer;
    }

    public void setDrawer(GLDrawer2D drawer) {
        mDrawer = drawer;
        mRenderer.setDrawer(drawer);
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

    public void setVideoSize() {
        setVideoSize(Constants.PREFERRED_PREVIEW_WIDTH, Constants.PREFERRED_PREVIEW_HEIGHT);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public void setVideoSize(final int width, final int height) {
        if ((mRotation % 180) == 0) {
            mVideoWidth = width;
            mVideoHeight = height;
        } else {
            mVideoWidth = height;
            mVideoHeight = width;
        }

        Log.d(TAG, "setVideoSize: width: " + width + " height: " + height);
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
        mRenderer.onSurfaceDestroyed();
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

    /**
     * GLSurfaceViewのRenderer
     */
    private static final class CameraSurfaceRenderer
            implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {  // API >= 11

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
        private int mProgramId;

        public CameraSurfaceRenderer(final CameraGLView parent, GLDrawer2D drawer) {
            Log.d(TAG, "CameraSurfaceRenderer:");
            mWeakParent = new WeakReference<>(parent);
            Matrix.setIdentityM(mMvpMatrix, 0);
            mDrawer = drawer;
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

        public void setDrawer(final GLDrawer2D drawer) {
            runOnDraw(() -> {
                GLDrawer2D old = mDrawer;
                mDrawer = drawer;
                if (old != null && mProgramId >= 0) {
                    old.release(mProgramId);
                }
                mProgramId = mDrawer.init();
                GLES20.glUseProgram(mProgramId);
                CameraGLView parent = mWeakParent.get();

                if (parent != null) {
                    parent.updateScaleType();
                }
            });
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

            mProgramId = mDrawer.init();
            mDrawer.setMatrix(mMvpMatrix, 0);
        }

        @Override
        public void onSurfaceChanged(final GL10 unused, final int width, final int height) {
            Log.d(TAG, String.format("onSurfaceChanged:(%d,%d)", width, height));
            // if at least with or height is zero, initialization of this view is still progress.
            if (width == 0 || height == 0) {
                return;
            }

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
            GLSurfaceView parent = mWeakParent.get();
            if (parent != null) {
                parent.queueEvent(this::cleanUp);
            } else {
                cleanUp();
            }
        }

        private void cleanUp() {
            if (mDrawer != null && mProgramId >= 0) {
                mDrawer.release(mProgramId);
                mProgramId = -1;
            }

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

            GLES20.glViewport(0, 0, view_width, view_height);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            final double video_width = parent.mVideoWidth;
            final double video_height = parent.mVideoHeight;

            if (video_width == 0 || video_height == 0) return;

            Matrix.setIdentityM(mMvpMatrix, 0);
            final double view_aspect = view_width / (double) view_height;

            Log.i(TAG, String.format("updateViewport view: (%d,%d) view_aspect: %f,video: (%1.0f,%1.0f)",
                    view_width, view_height, view_aspect, video_width, video_height));

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
                    Log.v(TAG, "scale square: " + scale_x + " " + scale_y);

                    GLES20.glViewport(view_x, view_y, newPreviewSize, newPreviewSize);
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                    Matrix.scaleM(mMvpMatrix, 0, scale_x, scale_y, 1.0f);
                    break;
                }
            }

            if (mDrawer != null) mDrawer.setMatrix(mMvpMatrix, 0);
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
            // draw to preview screen
            mDrawer.draw(mProgramId, mGLTextureId, mStMatrix);
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