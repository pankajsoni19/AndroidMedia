package com.softwarejoint.media.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.opengl.GLException;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringDef;
import android.util.Log;

import com.softwarejoint.media.fileio.FileHandler;
import com.softwarejoint.media.glutils.GLDrawer2D;
import com.softwarejoint.media.picker.MediaPickerOpts;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

import static android.graphics.Bitmap.CompressFormat.JPEG;
import static android.graphics.Bitmap.CompressFormat.PNG;
import static android.graphics.Bitmap.Config.ARGB_8888;


/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 15/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class BitmapUtils {

    private static final String TAG = "BitmapUtils";

    public static String saveBitmapFromGLSurface(GLDrawer2D drawer, MediaPickerOpts opts) {
        int x = drawer.getStartX();
        int y = drawer.getStartY();
        int w = drawer.width();
        int h = drawer.height();

        return saveBitmapFromGLSurface(x, y, w, h, opts);
    }

    private static String saveBitmapFromGLSurface(int x, int y, int w, int h, MediaPickerOpts opts) {
        Bitmap bitmap = createBitmapFromGLSurface(x, y, w, h);
        return bitmap != null ? saveBitmap(bitmap, opts) : null;
    }

    public static String saveBitmap(Bitmap bitmap, MediaPickerOpts opts) {
        File tempFile = FileHandler.getTempFile(opts);

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
            if (opts.cropEnabled) {
                bitmap.compress(PNG, 100, fileOutputStream);
            } else {
                bitmap.compress(JPEG, 90, fileOutputStream);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception ex) {
            Log.e(TAG, "saveBitmap: ", ex);
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
            return null;
        }

        return tempFile.exists() ? tempFile.getPath() : null;
    }

    public static Bitmap createBitmapFromGLSurface(int x, int y, int w, int h) {
        EGL10 egl = (EGL10) EGLContext.getEGL();
        GL10 gl = (GL10) egl.eglGetCurrentContext().getGL();

        int bitmapBuffer[] = new int[w * h];
        int bitmapSource[] = new int[w * h];

        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);

        try {
            gl.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer);
            int offset1, offset2;
            for (int i = 0; i < h; i++) {
                offset1 = i * w;
                offset2 = (h - i - 1) * w;
                for (int j = 0; j < w; j++) {
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    bitmapSource[offset2 + j] = pixel;
                }
            }
        } catch (GLException ex) {
            Log.e(TAG, "createBitmapFromGLSurface: matrix translate", ex);
            return null;
        }

        return Bitmap.createBitmap(bitmapSource, w, h, ARGB_8888);
    }

    public static Bitmap decodeResource(Resources resources, @DrawableRes int resId, Bitmap original) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, resId, options);
        options.inSampleSize = calculateInSampleSize(options, original.getWidth(), original.getHeight());
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeResource(resources, resId, options);
        bitmap = Bitmap.createScaledBitmap(bitmap, original.getWidth(), original.getHeight(), true);
        return bitmap;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeBitmapFromFile(String imagePath, int imgSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        options.inSampleSize = BitmapUtils.calculateInSampleSize(options, imgSize, imgSize);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
        return bitmap != null ? ExifUtil.rotateBitmap(imagePath, bitmap) : null;
    }

    public static String createCroppedBitmap(String origImagePath, MediaPickerOpts opts) {
        final int imgSize = opts.imgSize;

        Bitmap originalImage = decodeBitmapFromFile(origImagePath, imgSize);
        if (originalImage == null) return origImagePath;

        final int originalWidth = originalImage.getWidth();
        final int originalHeight = originalImage.getHeight();

        if (originalWidth == imgSize && originalHeight == imgSize) {
            originalImage.recycle();
            return origImagePath;
        }

        float scale;
        float xTranslation = 0.0f;
        float yTranslation = 0.0f;

        /**
         Handles this condition:
         ((originalWidth >= imgSize && originalHeight >= imgSize) ||
         (originalWidth < imgSize && originalHeight < imgSize))

         irregular sizes may not fit
         */

        Bitmap result = Bitmap.createBitmap(imgSize, imgSize, ARGB_8888);
        Canvas canvas = new Canvas(result);

        //take smaller of 2 dimensions
        if (originalWidth > originalHeight) {
            scale = (float) imgSize / originalHeight;
            xTranslation = (imgSize - (originalWidth * scale)) / 2.0f;
        } else {
            scale = (float) imgSize / originalWidth;
            yTranslation = (imgSize - (originalHeight * scale)) / 2.0f;
        }

        Matrix transformation = new Matrix();
        transformation.postTranslate(xTranslation, yTranslation);
        transformation.preScale(scale, scale);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);

        canvas.drawBitmap(originalImage, transformation, paint);

        String savedPath = saveBitmap(result, opts);

        originalImage.recycle();
        result.recycle();

        return savedPath != null ? savedPath : origImagePath;
    }

    /**
    public static String saveBitmap(Bitmap bitmap, MediaPickerOpts opts, File tempFile) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
            if (opts.cropEnabled) {
                bitmap.compress(PNG, 100, fileOutputStream);
            } else {
                bitmap.compress(JPEG, 90, fileOutputStream);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception ex) {
            Log.e(TAG, "saveBitmap: ", ex);
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
            return null;
        }

        return tempFile.exists() ? tempFile.getPath() : null;
    }

    private static double ratio(int w, int h) {
        int smallSide = Math.min(w, h);
        int largeSide = Math.max(w, h);
        return (double) largeSide / smallSide;
    }*/
}
