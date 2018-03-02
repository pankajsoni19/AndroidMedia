package com.serenegiant.fileio;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.serenegiant.mediaaudiotest.R;

import java.lang.ref.WeakReference;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 02/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class ImageLoader extends AsyncTask<String, Void, Bitmap> {

    private static final int SIZE = 64;

    private static int imageSize = SIZE;

    static {
        imageSize = Resources.getSystem().getDisplayMetrics().densityDpi;
    }

    private String filePath;
    private WeakReference<ImageView> reference;


    public static ImageLoader with(String path) {
        return new ImageLoader(path);
    }

    public void loadInto(ImageView imageView) {
        String loaderKey = (String) imageView.getTag(R.id.image_loader_key);
        if (filePath.equals(loaderKey) && imageView.getDrawable() != null) {
            return;
        }

        reference = new WeakReference<>(imageView);
        imageView.setTag(R.id.image_loader_key, filePath);

        execute();
    }

    private ImageLoader(String path) {
        filePath = path;
    }

    @Override
    protected Bitmap doInBackground(String... strings) {
        ImageView imageView = reference.get();
        if (imageView == null) { return null; }

        Bitmap bitmap = MemoryCache.getInstance().get(filePath);
        if (bitmap != null) { return bitmap; }

        int imgHeight = imageView.getHeight();
        int imgWidth = imageView.getWidth();

        imgHeight =  imgHeight > 0 ? imgHeight : imageSize;
        imgWidth =  imgWidth > 0 ? imgWidth : imageSize;

        int imgSize = Math.min(imgHeight, imgWidth);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        options.inSampleSize = calculateInSampleSize(options, imgSize, imgSize);
        options.inJustDecodeBounds = false;

        bitmap = BitmapFactory.decodeFile(filePath, options);
        MemoryCache.getInstance().put(filePath, bitmap);
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        ImageView imageView = reference.get();
        if (imageView != null && filePath.equals(imageView.getTag(R.id.image_loader_key))) {
            imageView.setImageBitmap(bitmap);
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
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
}
