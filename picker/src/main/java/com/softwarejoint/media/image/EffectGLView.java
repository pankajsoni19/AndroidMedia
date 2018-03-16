package com.softwarejoint.media.image;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.media.effect.Effect;
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

import com.softwarejoint.media.enums.ImageEffectType;
import com.softwarejoint.media.glutils.GLDrawer2D;
import com.softwarejoint.media.multitouch.MultiTouchListener;
import com.softwarejoint.media.picker.MediaPickerOpts;
import com.softwarejoint.media.utils.BitmapUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.softwarejoint.media.enums.ImageEffectType.EFFECT_BLACKWHITE;
import static com.softwarejoint.media.enums.ImageEffectType.EFFECT_DOCUMENTARY;
import static com.softwarejoint.media.enums.ImageEffectType.EFFECT_GRAYSCALE;
import static com.softwarejoint.media.enums.ImageEffectType.EFFECT_LOMOISH;
import static com.softwarejoint.media.enums.ImageEffectType.EFFECT_NEGATIVE;
import static com.softwarejoint.media.enums.ImageEffectType.EFFECT_POSTERIZE;
import static com.softwarejoint.media.enums.ImageEffectType.EFFECT_SEPIA;
import static com.softwarejoint.media.enums.ImageEffectType.EFFECT_VIGNETTE;
import static com.softwarejoint.media.enums.ImageEffectType.NONE;

@SuppressWarnings("WeakerAccess")
public final class EffectGLView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private static final String TAG = "EffectGLView";

    private static String[] EFFECTS = {
            //NONE, EFFECT_SEPIA,
            EFFECT_GRAYSCALE,
            //EFFECT_POSTERIZE, EFFECT_NEGATIVE, EFFECT_BLACKWHITE, EFFECT_LOMOISH, EFFECT_DOCUMENTARY, EFFECT_VIGNETTE
    };

    private static final int FILTERED_PREVIEW_SIZE = 96;
    private static final int FILTER_PREVIEWS_PER_ROW = 3;

    private boolean mHasSurface;

    private boolean mInitialised;
    private boolean filtersEnabled;
    private volatile boolean filtersPreviewEnabled;
    //private boolean imageLoaded = false;

    private int[] mTextures = new int[2];
    private EffectContext mEffectContext;
    private TextureRenderer mTexRenderer = new TextureRenderer();
    private int mImageWidth;
    private int mImageHeight;
    private Bitmap origBitmap;

    private List<EffectRenderer> effects = new ArrayList<>();

    private final Queue<Runnable> mRunOnDraw = new LinkedList<>();

    private @ImageEffectType
    volatile String mCurrentEffect = ImageEffectType.NONE;

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
        heightPixels = displayMetrics.heightPixels;
        translationFactor = displayMetrics.density;
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
        buildMainTransform();
        mTexRenderer.setMatrix(mMVPMatrix);
        requestRender();
    }

    private void buildMainTransform() {
        Matrix.setRotateM(mMVPMatrix, 0, rotation, 0, 0, -1.0f);
        Matrix.scaleM(mMVPMatrix, 0, scale, scale, 0);
        Matrix.translateM(mMVPMatrix, 0, -factorX * translationFactor/widthPixels, factorY * translationFactor/heightPixels, 0);
    }

    public void init(String imagePath, MediaPickerOpts opts) {
        origImagePath = imagePath;
        filtersEnabled = opts.filtersEnabled;
        if (mHasSurface && getWidth() > 0 && getHeight() > 0) {
            runOnDraw(this::loadImage);
        }
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
                // queueEvent(() -> mRenderer.onTouched(x, y));
                break;
            case MotionEvent.ACTION_UP:
                break;
        }

        return true;
    }

    private void loadImage() {
        Log.d(TAG, "loadImage: mHasSurface: " + mHasSurface + " origImagePath: " + origImagePath);
        final int width = getWidth();
        final int height = getHeight();

        if (origBitmap == null) {
            final int imgSize = Math.min(width, height);
            origBitmap = BitmapUtils.decodeBitmapFromFile(origImagePath, imgSize);
        }

        if (origBitmap != null && mHasSurface) {
            mImageWidth = origBitmap.getWidth();
            mImageHeight = origBitmap.getHeight();

            if (!mInitialised) {

                mTexRenderer.updateViewSize(width, height);
                mTexRenderer.updateTextureSize(mImageWidth, mImageHeight);

                float ratio = (float) width / height;

                Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
                Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
                Matrix.setRotateM(mMVPMatrix, 0, rotation, 0, 0, -1.0f);

                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, origBitmap, 0);

                mTexRenderer.setMatrix(mMVPMatrix);

                GLES20.glViewport(0, 0, width, height);

                createEffects();
            }

            mInitialised = true;
        }
    }

    public void toggleEffectPreviews() {
        runOnDraw(() -> {
            filtersPreviewEnabled = !filtersPreviewEnabled;
            if (!filtersPreviewEnabled) {
                GLES20.glViewport(0, 0, getWidth(), getHeight());
            }
        });
    }

    private void init() {
        if (!mHasSurface) {
            mEffectContext = EffectContext.createWithCurrentGlContext();

            GLES20.glGenTextures(2, mTextures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);

            GLToolbox.initTexParams();

            mTexRenderer.init();

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
        if (mTexRenderer != null) {
            mTexRenderer.release();
        }

        for (EffectRenderer renderer : effects) {
            if (renderer != null) renderer.release();
        }

        for (int mTexture : mTextures) {
            if (mTexture > 0) {
                GLDrawer2D.deleteTex(mTexture);
            }
        }

        if (mEffectContext != null) {
            mEffectContext.release();
        }

        mEffectContext = null;
    }

    public void onEffectSelected(@ImageEffectType String effectType) {
        runOnDraw(() -> {
            mCurrentEffect = effectType;
            if (!ImageEffectType.NONE.equals(mCurrentEffect)) {
                //makeEffectCurrent(effects.get(effectType));
            }
        });
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.add(runnable);
        }

        requestRender();
    }

    private void runAll(Queue<Runnable> queue) {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                queue.poll().run();
            }
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (!mHasSurface) return;

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        runAll(mRunOnDraw);

        int texId = ImageEffectType.NONE.equals(mCurrentEffect)  ? 0 : 1;

        if (filtersPreviewEnabled) {
            mTexRenderer.renderTexture(mTextures[texId]);

            for (EffectRenderer renderer: effects) {


                Log.d(TAG, "renderer: " + renderer.name() + " startX: " + renderer.startX
                        + " startY: " + renderer.startY
                        + " width: " + renderer.mViewWidth
                        + " height: " + renderer.mViewHeight);

                renderer.computeOutputVertices();
                renderer.makeEffectCurrent(mTextures[0], renderer.mViewWidth, renderer.mViewHeight, mTextures[1]);

                GLES20.glViewport(renderer.startX, renderer.startY, renderer.mViewWidth, renderer.mViewHeight);
                GLToolbox.checkGlError("glViewport");

                renderer.renderTexture();
            }
            GLES20.glViewport(0, 0, mTexRenderer.mViewWidth, mTexRenderer.mViewHeight);
        } else {
            mTexRenderer.renderTexture(mTextures[texId]);
        }
    }

    private void createEffects() {
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;

        int filterPreviewSize = (int) (FILTERED_PREVIEW_SIZE * displayMetrics.density);

        int margin = (screenWidth - (filterPreviewSize * FILTER_PREVIEWS_PER_ROW)) / (FILTER_PREVIEWS_PER_ROW + 1);

        int startX = margin;
        int startY = (int) (0.50 * displayMetrics.heightPixels);

        EffectFactory effectFactory = mEffectContext.getFactory();

        int i = 0;

        for (@ImageEffectType String effectType: EFFECTS) {
            EffectRenderer renderer = new EffectRenderer(effectFactory, effectType);
            renderer.updateTextureSize(mImageWidth, mImageHeight);
            renderer.updateViewSize(filterPreviewSize, filterPreviewSize);
            renderer.setStartXY(startX, startY);

            if (i % FILTER_PREVIEWS_PER_ROW == 0) {
                startX = margin;
                startY = startY + margin;
            } else {
                startX = startX + margin;
            }

            i++;

            Log.d(TAG, "createEffects: " + effectType + " startX: " + startX + " startY: " + startY);
            effects.add(renderer);
        }
    }
}