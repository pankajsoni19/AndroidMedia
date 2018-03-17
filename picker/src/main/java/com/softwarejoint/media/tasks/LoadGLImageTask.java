package com.softwarejoint.media.tasks;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.softwarejoint.media.image.ImageEffectFragment;
import com.softwarejoint.media.picker.MediaPickerOpts;
import com.softwarejoint.media.utils.BitmapUtils;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 17/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class LoadGLImageTask extends AsyncTask<Void, Void, Bitmap> {

    private final int w;
    private final int h;
    private final int[] bitmapBuffer;
    private final ImageEffectFragment fragment;
    private MediaPickerOpts opts;
    private String imagePath;

    public LoadGLImageTask(int w, int h, int[] bitmapBuffer, ImageEffectFragment fragment, MediaPickerOpts opts) {
        this.w = w;
        this.h = h;
        this.bitmapBuffer = bitmapBuffer;
        this.fragment = fragment;
        this.opts = opts;
    }

    @Override
    protected Bitmap doInBackground(Void... voids) {
        Bitmap bitmap = BitmapUtils.createBitmapFromGLBuffer(w, h, bitmapBuffer);
        imagePath = BitmapUtils.saveBitmap(bitmap, opts);
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (fragment != null && !fragment.isRemoving()) {
            fragment.onImageLoaded(imagePath, bitmap);
        }
    }
}
