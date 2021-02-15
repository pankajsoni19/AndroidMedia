package com.pankajsoni19.media.utils;

/**
 * https://gist.githubusercontent.com/9re/1990019/raw/95414cbd6c2bb75d27a564e1b677caded07134f4/ExifUtil.java
 */

import android.graphics.Bitmap;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;

class ExifUtil {

    static void saveExif(String oldFile, String newFile) {
        if (!oldFile.endsWith("jpeg") || !oldFile.endsWith("jpg")) return;
        if (!newFile.endsWith("jpeg") || !newFile.endsWith("jpg")) return;

        try {
            int orientation = getExifOrientation(oldFile);
            ExifInterface newInterface = new ExifInterface(newFile);
            newInterface.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(orientation));
            newInterface.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static Bitmap rotateBitmap(String src, Bitmap bitmap) {
        if (!src.endsWith("jpeg") || !src.endsWith("jpg")) {
            return bitmap;
        }

        int orientation;

        try {
            orientation = getExifOrientation(src);
        } catch (IOException e) {
            e.printStackTrace();
            return bitmap;
        }

        Matrix matrix = new Matrix();

        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            case ExifInterface.ORIENTATION_NORMAL:
            default:
                return bitmap;
        }

        try {
            Bitmap oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return oriented;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    private static int getExifOrientation(String src) throws IOException {
        ExifInterface exifInterface = new ExifInterface(src);
        return exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
    }
}