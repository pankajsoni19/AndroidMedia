package com.softwarejoint.media.fileio;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.content.MimeTypeFilter;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import com.softwarejoint.media.R;
import com.softwarejoint.media.enums.MediaType;
import com.softwarejoint.media.utils.BitmapUtils;

import java.lang.ref.WeakReference;
import java.util.Locale;

import static android.provider.MediaStore.Video.Thumbnails.MICRO_KIND;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 02/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class ImageLoader extends AsyncTask<String, Void, Bitmap> {

    private static final int SIZE = 72;

    private static int imageSize = SIZE;

    static {
        imageSize = (int) (SIZE * Resources.getSystem().getDisplayMetrics().density);
    }

    private String mediaPath;
    private Long id;
    private WeakReference<ImageView> reference;
    private @MediaType int mediaType;

    private ImageLoader(String mediaPath) {
        this.mediaPath = mediaPath;
    }

    private ImageLoader(long id) {
        this.id = id;
    }

    public static ImageLoader load(long id) {
        return new ImageLoader(id);
    }

    public static ImageLoader load(String mediaPath) {
        return new ImageLoader(mediaPath);
    }

    private String getKey() {
        return id == null ? mediaPath : String.valueOf(id);
    }

    public ImageLoader withMediaHint(@MediaType int mediaType) {
        this.mediaType = mediaType;
        return this;
    }
    /**
     * Loads blank thumbnail if the file is corrupt.
     * @param imageView
     */
    public void into(ImageView imageView) {
        final String key = getKey();

        if (key.equals(imageView.getTag(R.id.image_loader_key))
                && imageView.getDrawable() != null) {
            return;
        }

        imageView.setTag(R.id.image_loader_key, key);
        Bitmap bitmap = MemoryCache.getInstance().get(key);

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            reference = new WeakReference<>(imageView);
            execute();
        }
    }

    @Override
    protected Bitmap doInBackground(String... strings) {
        Bitmap bitmap = null;

        if (id != null) {
            ImageView imageView = reference.get();
            if (imageView == null) { return null; }

            ContentResolver crThumb = imageView.getContext().getContentResolver();

/**
    int imgHeight = imageView.getHeight();
            int imgWidth = imageView.getWidth();

            imgHeight =  imgHeight > 0 ? imgHeight : imageSize;
            imgWidth =  imgWidth > 0 ? imgWidth : imageSize;

            final int imgSize = Math.min(imgHeight, imgWidth);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.outHeight = 96;
            options.outWidth = 96;
            options.inSampleSize = BitmapUtils.calculateInSampleSize(options, imgSize, imgSize);

           */

            if (mediaType == MediaType.VIDEO) {
                bitmap = MediaStore.Video.Thumbnails.getThumbnail(crThumb, id, MICRO_KIND, null);
            } else {
                bitmap = MediaStore.Images.Thumbnails.getThumbnail(crThumb, id, MICRO_KIND, null);
            }
        } else if (mediaPath != null) {

            if (mediaType == MediaType.VIDEO) {
                bitmap = ThumbnailUtils.createVideoThumbnail(mediaPath, MICRO_KIND);
            } else {
                bitmap = loadBitmapFromFile();
            }
        }

        MemoryCache.getInstance().put(getKey(), bitmap);

        return bitmap;
    }

    private Bitmap loadBitmapFromFile() {
        ImageView imageView = reference.get();
        if (imageView == null) { return null; }

        int imgHeight = imageView.getHeight();
        int imgWidth = imageView.getWidth();

        imgHeight =  imgHeight > 0 ? imgHeight : imageSize;
        imgWidth =  imgWidth > 0 ? imgWidth : imageSize;

        final int imgSize = Math.min(imgHeight, imgWidth);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(mediaPath, options);
        options.inSampleSize = BitmapUtils.calculateInSampleSize(options, imgSize, imgSize);
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(mediaPath, options);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        final ImageView imageView = reference.get();
        if (imageView == null) { return; }

        final String key = getKey();

        if (key.equals(imageView.getTag(R.id.image_loader_key))) {
            imageView.setImageBitmap(bitmap);
        }
    }
}
