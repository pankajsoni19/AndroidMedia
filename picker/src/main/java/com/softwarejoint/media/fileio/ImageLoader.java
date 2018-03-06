package com.softwarejoint.media.fileio;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.widget.ImageView;

import com.softwarejoint.media.R;

import java.lang.ref.WeakReference;

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

    private ImageLoader(String mediaPath) {
        this.mediaPath = mediaPath;
    }

    private ImageLoader(long id) {
        this.id = id;
    }

    public static ImageLoader with(long id) {
        return new ImageLoader(id);
    }

    public static ImageLoader with(String mediaPath) {
        return new ImageLoader(mediaPath);
    }

    private String getKey() {
        return id == null ? mediaPath : String.valueOf(id);
    }

    /**
     * Loads blank thumbnail if the file is corrupt.
     * @param imageView
     */
    public void loadInto(ImageView imageView) {
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

            int imgHeight = imageView.getHeight();
            int imgWidth = imageView.getWidth();

            imgHeight =  imgHeight > 0 ? imgHeight : imageSize;
            imgWidth =  imgWidth > 0 ? imgWidth : imageSize;

            final int imgSize = Math.min(imgHeight, imgWidth);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.outHeight = 96;
            options.outWidth = 96;
            options.inSampleSize = calculateInSampleSize(options, imgSize, imgSize);

            bitmap = MediaStore.Video.Thumbnails.getThumbnail(crThumb, id, MICRO_KIND, options);
        } else if (mediaPath != null) {
            bitmap = ThumbnailUtils.createVideoThumbnail(mediaPath, MICRO_KIND);
        }

        MemoryCache.getInstance().put(getKey(), bitmap);

        return bitmap;
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
