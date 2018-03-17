package com.softwarejoint.media.tasks;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.softwarejoint.media.camera.CameraFragment;
import com.softwarejoint.media.picker.MediaPickerOpts;
import com.softwarejoint.media.utils.BitmapUtils;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 17/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class SaveGLImageTask extends AsyncTask<Void, Void, String> {

    private final int w;
    private final int h;
    private final int[] bitmapBuffer;
    private final CameraFragment fragment;
    private MediaPickerOpts opts;

    public SaveGLImageTask(int w, int h, int[] bitmapBuffer, CameraFragment fragment, MediaPickerOpts opts) {
        this.w = w;
        this.h = h;
        this.bitmapBuffer = bitmapBuffer;
        this.fragment = fragment;
        this.opts = opts;
    }

    @Override
    protected String doInBackground(Void... voids) {
        Bitmap bitmap = BitmapUtils.createBitmapFromGLBuffer(w, h, bitmapBuffer);
        return BitmapUtils.saveBitmap(bitmap, opts, false);
    }

    @Override
    protected void onPostExecute(String imagePath) {
        if (fragment != null && !fragment.isRemoving()) {
            fragment.onPictureSaved(imagePath);
        }
    }
}
