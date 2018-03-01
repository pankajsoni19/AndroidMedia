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
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.serenegiant.encoder.MediaVideoEncoder;
import com.serenegiant.glutils.GLDrawer2D;
import com.serenegiant.mediaaudiotest.BuildConfig;
import com.serenegiant.utils.CameraHelper;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Sub class of GLSurfaceView to display camera preview and write video frame to capturing surface
 */
@SuppressWarnings("WeakerAccess")
public final class CameraGLView extends GLSurfaceView {

    private static final String TAG = "CameraGLView";

    private static final int PREFERRED_PREVIEW_WIDTH = 640;
    private static final int PREFERRED_PREVIEW_HEIGHT = 480;

    private static final int SCALE_STRETCH_FIT = 0;
    private static final int SCALE_KEEP_ASPECT_VIEWPORT = 1;
    private static final int SCALE_KEEP_ASPECT = 2;
    private static final int SCALE_CROP_CENTER = 3;

    private int previewWidth = PREFERRED_PREVIEW_WIDTH;
    private int previewHeight = PREFERRED_PREVIEW_HEIGHT;
    private int cameraId = CameraHelper.getBackCameraID();

    private final CameraSurfaceRenderer mRenderer;
    private boolean mHasSurface;
    private CameraHandler mCameraHandler = null;
    private int mVideoWidth, mVideoHeight;
    private int mRotation;
    private int mScaleMode = SCALE_STRETCH_FIT;
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
        if (mHasSurface) {
            if (mCameraHandler == null) {
                Log.d(TAG, "surface already exist");
                startPreview(getWidth(), getHeight());
            }
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause:");
        if (mCameraHandler != null) {
            // just request stop previewing
            mCameraHandler.stopPreview(false);
        }
        super.onPause();
    }

    public int getScaleMode() {
        return mScaleMode;
    }

    public void setScaleMode(final int mode) {
        if (mScaleMode != mode) {
            mScaleMode = mode;
            queueEvent(mRenderer::updateViewport);
        }
    }

    public void setVideoSize(final int width, final int height) {
        if ((mRotation % 180) == 0) {
            mVideoWidth = width;
            mVideoHeight = height;
        } else {
            mVideoWidth = height;
            mVideoHeight = width;
        }

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
                if (old != null) {
                    old.release(mProgramId);
                }
                mProgramId = mDrawer.init();
                GLES20.glUseProgram(mProgramId);
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
            // create texture ID
            mGLTextureId = GLDrawer2D.initTex();
            // create SurfaceTexture with texture ID.
            mSTexture = new SurfaceTexture(mGLTextureId);
            mSTexture.setOnFrameAvailableListener(this);
            // clear screen with yellow color so that you can see rendering rectangle
            GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);
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
            if ((width == 0) || (height == 0)) return;
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
                mDrawer.release(mProgramId);
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

            Log.i(TAG,
                    String.format("view(%d,%d)%f,video(%1.0f,%1.0f)", view_width, view_height, view_aspect,
                            video_width, video_height));

            switch (parent.mScaleMode) {
                case SCALE_STRETCH_FIT:
                    break;
                case SCALE_KEEP_ASPECT_VIEWPORT: {
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
                case SCALE_KEEP_ASPECT:
                case SCALE_CROP_CENTER: {
                    final double scale_x = view_width / video_width;
                    final double scale_y = view_height / video_height;
                    final double scale =
                            (parent.mScaleMode == SCALE_CROP_CENTER ? Math.max(scale_x, scale_y)
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
                        mVideoEncoder.frameAvailableSoon(mStMatrix);
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
    private static final class CameraHandler extends Handler {
        private static final int MSG_PREVIEW_START = 1;
        private static final int MSG_PREVIEW_STOP = 2;
        private CameraThread mThread;

        public CameraHandler(final CameraThread thread) {
            mThread = thread;
        }

        public void startPreview(final int width, final int height) {
            sendMessage(obtainMessage(MSG_PREVIEW_START, width, height));
        }

        /**
         * request to stop camera preview
         *
         * @param needWait need to wait for stopping camera preview
         */
        public void stopPreview(final boolean needWait) {
            synchronized (this) {
                sendEmptyMessage(MSG_PREVIEW_STOP);
                if (needWait && mThread.mIsRunning) {
                    try {
                        Log.d(TAG, "wait for terminating of camera thread");
                        wait();
                    } catch (final InterruptedException ignore) {
                    }
                }
            }
        }

        /**
         * message handler for camera thread
         */
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_PREVIEW_START:
                    mThread.startPreview(msg.arg1, msg.arg2);
                    break;
                case MSG_PREVIEW_STOP:
                    mThread.stopPreview();
                    synchronized (this) {
                        notifyAll();
                    }
                    Looper looper = Looper.myLooper();
                    if (looper != null) looper.quit();
                    mThread = null;
                    break;
                default:
                    throw new RuntimeException("unknown message:what=" + msg.what);
            }
        }
    }

    /**
     * Thread for asynchronous operation of camera preview
     */
    private static final class CameraThread extends Thread {
        private final Object mReadyFence = new Object();
        private final WeakReference<CameraGLView> mWeakParent;
        private CameraHandler mHandler;
        private volatile boolean mIsRunning = false;
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

        /**
         * start camera preview
         */
        private void startPreview(final int width, final int height) {
            Log.d(TAG, "startPreview:");

            final CameraGLView parent = mWeakParent.get();

            if ((parent == null) || (mCamera != null)) {
                return;
            }

            // This is a sample project so just use 0 as camera ID.
            // it is better to selecting camera is available
            try {
                mCamera = Camera.open(parent.cameraId);
                final Camera.Parameters params = mCamera.getParameters();

                final List<String> focusModes = params.getSupportedFocusModes();

                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }

                // let's try fastest frame rate. You will get near 60fps, but your device become hot.
                final List<int[]> supportedFpsRange = params.getSupportedPreviewFpsRange();

                if (supportedFpsRange != null) {
                    //TODO: get 30fps rate
                    final int n = supportedFpsRange.size();

                    for (int i = 0; i < n; i++) {
                        int range[] = supportedFpsRange.get(i);
                        Log.d(TAG, String.format("supportedFpsRange(%d)=(%d,%d)", i, range[0], range[1]));
                    }

                    final int[] max_fps = supportedFpsRange.get(supportedFpsRange.size() - 1);
                    Log.d(TAG, String.format("fps:%d-%d", max_fps[0], max_fps[1]));
                    params.setPreviewFpsRange(max_fps[0], max_fps[1]);
                }

                params.setRecordingHint(true);
                // request preview size
                // this is a sample project and just use fixed value
                // if you want to use other size, you also need to change the recording size.
                List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
                Camera.Size previewSize = CameraHelper.getOptimalSize(previewSizes,
                        parent.previewWidth, parent.previewHeight);

                Log.i(TAG, String.format("previewSize(%d, %d)", previewSize.width, previewSize.height));

                // rotate camera preview according to the device orientation
                /**
                 * rotate preview screen according to the device orientation
                 */

                final int degrees = CameraHelper.getDisplayOrientation(parent.getContext(), parent.cameraId);
                mCamera.setDisplayOrientation(degrees);

                // apply rotation setting
                parent.mRotation = degrees;

                mCamera.setParameters(params);

                // adjust view size with keeping the aspect ration of camera preview.
                // here is not a UI thread and we should request parent view to execute.
                parent.post(new Runnable() {
                    @Override
                    public void run() {
                        parent.setVideoSize(previewSize.width, previewSize.height);
                    }
                });

                final SurfaceTexture st = parent.getSurfaceTexture();
                //noinspection ConstantConditions
                st.setDefaultBufferSize(previewSize.width, previewSize.height);
                mCamera.setPreviewTexture(st);
            } catch (final IOException | RuntimeException e) {
                Log.e(TAG, "startPreview:", e);
                if (mCamera != null) {
                    mCamera.release();
                    mCamera = null;
                }
            }

            if (mCamera != null) {
                // start camera preview display
                mCamera.startPreview();
            }
        }

        /**
         * stop camera preview
         */
        private void stopPreview() {
            Log.d(TAG, "stopPreview:");

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
}