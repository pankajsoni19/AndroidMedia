package com.pankajsoni19.media.image;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.pankajsoni19.media.enums.ImageEffect;
import com.pankajsoni19.media.glutils.GLDrawer2D;
import com.pankajsoni19.media.multitouch.MultiTouchListener;
import com.pankajsoni19.media.picker.MediaPickerOpts;
import com.pankajsoni19.media.tasks.LoadImageTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.pankajsoni19.media.enums.ImageEffect.BLACKWHITE;
import static com.pankajsoni19.media.enums.ImageEffect.CROSSPROCESS;
import static com.pankajsoni19.media.enums.ImageEffect.DUOTONEBW;
import static com.pankajsoni19.media.enums.ImageEffect.DUOTONEPY;
import static com.pankajsoni19.media.enums.ImageEffect.FILLIGHT;
import static com.pankajsoni19.media.enums.ImageEffect.LOMOISH;
import static com.pankajsoni19.media.enums.ImageEffect.NEGATIVE;
import static com.pankajsoni19.media.enums.ImageEffect.SEPIA;
import static com.pankajsoni19.media.enums.ImageEffect.NONE;

@SuppressWarnings("WeakerAccess")
public final class EffectGLView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private static final String TAG = "EffectGLView";

    private static String[] EFFECTS = {
            NONE, SEPIA, CROSSPROCESS, DUOTONEBW, DUOTONEPY,
            NEGATIVE, BLACKWHITE, LOMOISH, FILLIGHT
    };
   // GRAYSCALE,POSTERIZE,  DOCUMENTARY, VIGNETTE
    private static final int FILTERED_PREVIEW_SIZE = 96;
    private static final int FILTER_PREVIEWS_PER_ROW = 3;

    private boolean mHasSurface;

    private boolean mInitialised;
    private boolean filtersEnabled;
    private volatile boolean filtersPreviewEnabled;

    private int[] mTextures = new int[2];
    private EffectContext mEffectContext;
    private EffectRenderer mEffectRenderer;
    private int mImageWidth;
    private int mImageHeight;
    private Bitmap origBitmap;

    private List<EffectRenderer> effects = new ArrayList<>();

    private final Queue<Runnable> mRunOnDraw = new LinkedBlockingQueue<>();

    private @ImageEffect
    volatile String mCurrentEffect = ImageEffect.NONE;

    private String origImagePath;

    private MultiTouchListener touchListener;
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];

    private float scale = 1.0f;
    private float rotation = 0f;
    private float factorX = 0f;
    private float factorY = 0f;
    private float pivotX = 0;
    private float pivotY = 0;

    private final int heightPixels;
    private final int widthPixels;
    private final float translationFactor;
    private final int previewSize;

    public EffectGLView(final Context context) {
        this(context, null, 0);
    }

    public EffectGLView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EffectGLView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs);

        //setZOrderOnTop(true);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8,8,8,8,16,0);
        getHolder().setFormat(PixelFormat.RGBA_8888);

        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        touchListener = new MultiTouchListener(getContext());

        setOnTouchListener(touchListener);

        Arrays.fill(mTextures, 0);

        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();

        widthPixels = displayMetrics.widthPixels;
        heightPixels = Math.min(widthPixels, displayMetrics.heightPixels);
        translationFactor = displayMetrics.density;

        previewSize = (int) (FILTERED_PREVIEW_SIZE * displayMetrics.density);
    }

    @Override
    public void setPivotX(float pivot) {
        Log.d(TAG, "pivotX: " + pivot);
        pivotY = pivot;
    }

    @Override
    public float getPivotX() {
        return pivotX;
    }

    @Override
    public void setPivotY(float pivot) {
        Log.d(TAG, "pivotY: " + pivot);
        pivotX = pivot;
    }

    @Override
    public float getPivotY() {
        return pivotY;
    }

    @Override
    public void setTranslationX(float translation) {
        factorX = factorX + translation;
        buildTransform();
    }

    @Override
    public float getTranslationX() {
        return factorX;
    }

    @Override
    public void setTranslationY(float translation) {
        factorY = factorY + translation;
        buildTransform();
    }

    @Override
    public float getTranslationY() {
        return factorY;
    }

    @Override
    public void setScaleX(float totalScale) {
        scale = totalScale;
        buildTransform();
    }

    @Override
    public void setScaleY(float scaleY) {
        //no-op
    }

    @Override
    public void setRotation(float deltaRotation) {
        rotation = deltaRotation + rotation;
        buildTransform();
    }

    @Override
    public float getRotation() {
        return rotation;
    }

    private void buildTransform() {
        if (!mInitialised || !mHasSurface) return;
        Log.d(TAG, "buildTransform");
        updateMainTransform();
        mEffectRenderer.setMatrix(mMVPMatrix);
        requestRender();
    }

    private void updateMainTransform() {
        Matrix.setRotateM(mMVPMatrix, 0, rotation, 0, 0, -1.0f);
        Matrix.scaleM(mMVPMatrix, 0, scale, scale, 0);
        Matrix.translateM(mMVPMatrix, 0, -factorX * translationFactor/widthPixels, factorY * translationFactor/heightPixels, 0);
    }

    public void init(MediaPickerOpts opts) {
        filtersEnabled = opts.filtersEnabled;

        if (!filtersEnabled) {
            for (EffectRenderer renderer: effects) {
                if (!ImageEffect.NONE.equals(renderer.name())) {
                    renderer.release();
                }
            }

            effects.clear();
        }

        if (mHasSurface && getWidth() > 0 && getHeight() > 0) {
            runOnDraw(this::loadImage);
        }
    }

    public void onImageLoaded(String imagePath, Bitmap bitmap) {
        origImagePath = imagePath;
        origBitmap = bitmap;
        runOnDraw(this::loadImage);
    }

    private void loadImage() {
        if (origImagePath == null || !mHasSurface) return;

        final int width = getWidth();
        final int height = getHeight();

        if (origBitmap == null) {
            new LoadImageTask(origImagePath, this).execute();
            return;
        }

        Log.d(TAG, "loadImage: origImagePath: " + origImagePath + " mInitialised: " + mInitialised);

        mImageWidth = origBitmap.getWidth();
        mImageHeight = origBitmap.getHeight();

        if (!mInitialised) {

            float ratio = (float) width / height;

            Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
            Matrix.setRotateM(mMVPMatrix, 0, rotation, 0, 0, -1.0f);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, origBitmap, 0);

            createEffects();
        }

        mInitialised = true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (!filtersPreviewEnabled) return super.dispatchTouchEvent(event);
        Log.d(TAG, "dispatchTouchEvent");

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                final int x = (int) event.getX();
                final int y = (int) event.getY();
                Log.d(TAG, "onTouched: x: " + x + " y: " + y);
                queueEvent(() -> onTouched(x, y));
                break;
            case MotionEvent.ACTION_UP:
                break;
        }

        return true;
    }

    private void updateMainEffect() {
        final int width = getWidth();
        final int height = getHeight();
        GLES20.glViewport(0, 0, width, height);

        mEffectRenderer.makeEffectCurrent(mTextures[0], mImageWidth, mImageHeight, mTextures[1]);

        mEffectRenderer.updateViewSize(width, height);
        mEffectRenderer.updateTextureSize(mImageWidth, mImageHeight);

        updateMainTransform();

        mEffectRenderer.setMatrix(mMVPMatrix);

        GLES20.glViewport(0, 0, width, height);
        GLToolbox.checkGlError("glViewport");
    }

    public void toggleEffectPreviews() {
        runOnDraw(() -> {
            Log.d(TAG, "toggleEffectPreviews");
            //filtersPreviewEnabled = !filtersPreviewEnabled;
            GLDrawer2D.deleteTex(mTextures[1]);
            GLES20.glGenTextures(1, mTextures, 1);
        });

       // return !filtersPreviewEnabled;
    }

    public void touched1 (String name){
        for (EffectRenderer renderer: effects) {
        if (name.equals(renderer.name())) {

            queueEvent(() -> {
                mCurrentEffect = renderer.name();
                mEffectRenderer = renderer;
            });
            toggleEffectPreviews();
            break;
        }
    }}

    private void onTouched(int x, int y) {
        for (EffectRenderer renderer: effects) {
            final int filterTop = (heightPixels - renderer.bottomY - previewSize);
            Rect openGLRect = new Rect(renderer.startX, filterTop,
                    renderer.startX + previewSize, filterTop + previewSize);

            if (openGLRect.contains(x, y)) {

                Log.d(TAG, "onTouched rect: " + openGLRect + " selected: " + renderer.name());

                queueEvent(() -> {
                    Log.d(TAG, "onTouched: queueEvent");
                    mCurrentEffect = renderer.name();
                    mEffectRenderer = renderer;
                });
                toggleEffectPreviews();
                break;
            }
        }
    }

    private void init() {
        if (!mHasSurface) {
            mEffectContext = EffectContext.createWithCurrentGlContext();

            GLES20.glGenTextures(2, mTextures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);

            GLToolbox.initTexParams();

            GLES20.glClearColor(0,0,0,0);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        }

        mHasSurface = true;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated: width: " + getWidth() + " height: " + getHeight());
        init();

        if (getWidth() > 0 && getHeight() > 0) {
            runOnDraw(this::loadImage);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged: width: " + width + " height: " + height);

        init();

        if (getWidth() > 0 && getHeight() > 0) {
            runOnDraw(this::loadImage);
        }
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed:");
        mHasSurface = false;
        mInitialised = false;
        queueEvent(this::onSurfaceDestroyed);
        super.surfaceDestroyed(holder);
    }

    private void onSurfaceDestroyed() {
        if (mEffectRenderer != null) {
            mEffectRenderer.release();
        }

        for (EffectRenderer renderer : effects) {
            renderer.release();
        }

        effects.clear();

        for (int mTexture : mTextures) {
            if (mTexture > 0) {
                GLDrawer2D.deleteTex(mTexture);
            }
        }

        if (mEffectContext != null) {
            mEffectContext.release();
        }

        mEffectContext = null;

        if (origBitmap != null) {
            origBitmap.recycle();
        }

        origBitmap = null;
    }

    protected void runOnDraw(final Runnable runnable) {
        mRunOnDraw.add(runnable);
        requestRender();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Log.d(TAG, "onDrawFrame mHasSurface: " + mHasSurface + " mInitialised: " + mInitialised);

        if (!mHasSurface) return;

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.poll().run();
        }

        if (!mInitialised) return;

       /* if (filtersPreviewEnabled) {

                 updateAllEffects();
            for (EffectRenderer renderer: effects) {
                renderer.makeEffectCurrent(mTextures[0], renderer.mViewWidth, renderer.mViewHeight, mTextures[1]);

                GLES20.glViewport(renderer.startX, renderer.bottomY, renderer.mViewWidth, renderer.mViewHeight);
                GLToolbox.checkGlError("glViewport");

                renderer.renderTexture();
            }
       } else {
       */      updateMainEffect();
            mEffectRenderer.renderTexture();
      //  }
    }

    private void createEffects() {
    //    if (filtersEnabled) {
            EffectFactory effectFactory = mEffectContext.getFactory();

            for (@ImageEffect String effectType: EFFECTS) {

                EffectRenderer renderer = new EffectRenderer(effectFactory, effectType);

                if (mCurrentEffect.equals(effectType)) {
                    mEffectRenderer = renderer;
                }

                effects.add(renderer);
            }
       // } else {
       //     mEffectRenderer = new EffectRenderer(null, ImageEffect.NONE);
       // }
    }

    private void updateAllEffects() {

        int margin = (widthPixels - (previewSize * FILTER_PREVIEWS_PER_ROW)) / (FILTER_PREVIEWS_PER_ROW + 1);

        int startX = margin;
        int bottomY = heightPixels - previewSize - margin;

        float[] matrix = new float[16];

        Matrix.setIdentityM(matrix, 0);

        for (EffectRenderer renderer: effects) {
            Log.d(TAG, "updateAllEffects: startX: " + startX + " bottomY: " + bottomY + " name: " + renderer.name());

            renderer.setStartXY(startX, bottomY);
            renderer.updateTextureSize(mImageWidth, mImageHeight);
            renderer.updateViewSize(previewSize, previewSize);

            startX = startX + previewSize + margin;

            if (widthPixels < (startX + previewSize + margin)) {
                startX = margin;
                bottomY = bottomY - previewSize - margin;   //move down
            }

            renderer.setMatrix(matrix);
        }
    }
}