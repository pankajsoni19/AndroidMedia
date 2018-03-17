package com.softwarejoint.media.picker;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.softwarejoint.media.enums.MediaType;
import com.softwarejoint.media.enums.ScaleType;
import com.softwarejoint.media.fileio.FileHandler;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 03/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
@SuppressWarnings("WeakerAccess")
public class MediaPickerOpts implements Parcelable {

    private static final int REQUEST_CODE = 1003;

    public static final String INTENT_OPTS = "com.softwarejoint.media.opts";
    public static final String INTENT_RES = "com.softwarejoint.media.result";

    public final @MediaType int mediaType;
    public @ScaleType int scaleType;
    public final boolean galleryEnabled;
    public final boolean flashEnabled;
    public final boolean filtersEnabled;
    public final boolean cropEnabled;

    public final boolean scaleTypeChangeable;

    public final int imgSize;
    public final int maxSelection;
    public String mediaDir;

    public MediaPickerOpts(int mediaType, int scaleType, boolean galleryEnabled, boolean flashEnabled,
                           boolean filtersEnabled, boolean cropEnabled, int size,
                           boolean scaleTypeChangeable, int maxSelection, String mediaDir) {
        this.mediaType = mediaType;
        this.scaleType = scaleType;
        this.galleryEnabled = galleryEnabled;
        this.flashEnabled = flashEnabled;
        this.filtersEnabled = filtersEnabled;
        this.cropEnabled = cropEnabled;
        this.scaleTypeChangeable = scaleTypeChangeable;
        this.imgSize = size;
        this.maxSelection = maxSelection;
        this.mediaDir = mediaDir;
    }

    protected MediaPickerOpts(Parcel in) {
        mediaType = in.readInt();
        scaleType = in.readInt();
        galleryEnabled = in.readByte() != 0;
        flashEnabled = in.readByte() != 0;
        filtersEnabled = in.readByte() != 0;
        cropEnabled = in.readByte() != 0;
        scaleTypeChangeable = in.readByte() != 0;
        imgSize = in.readInt();
        maxSelection = in.readInt();
        mediaDir = in.readString();
    }

    public static final Creator<MediaPickerOpts> CREATOR = new Creator<MediaPickerOpts>() {
        @Override
        public MediaPickerOpts createFromParcel(Parcel in) {
            return new MediaPickerOpts(in);
        }

        @Override
        public MediaPickerOpts[] newArray(int size) {
            return new MediaPickerOpts[size];
        }
    };

    public boolean showFilters() {
        return filtersEnabled && (mediaType == MediaType.VIDEO || !cropEnabled);
    }

    public static @Nullable Result onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE || resultCode != Activity.RESULT_OK) return null;
        return new Result(data.getStringArrayListExtra(INTENT_RES));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mediaType);
        dest.writeInt(scaleType);
        dest.writeByte((byte) (galleryEnabled ? 1 : 0));
        dest.writeByte((byte) (flashEnabled ? 1 : 0));
        dest.writeByte((byte) (filtersEnabled ? 1 : 0));
        dest.writeByte((byte) (cropEnabled ? 1 : 0));
        dest.writeByte((byte) (scaleTypeChangeable ? 1 : 0));
        dest.writeInt(imgSize);
        dest.writeInt(maxSelection);
        dest.writeString(mediaDir);
    }

    public void startActivity(Fragment fragment) {
        startActivity(fragment.getActivity());
    }

    public void startActivity(Activity activity) {
        if (mediaDir == null) {
            mediaDir = FileHandler.getApplicationName(activity);
        }

        Intent newIntent = new Intent(activity, PickerActivity.class);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        newIntent.putExtra(INTENT_OPTS, this);

        //noinspection unchecked
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity);
        ActivityCompat.startActivityForResult(activity, newIntent, REQUEST_CODE, options.toBundle());
    }

    @SuppressWarnings("unused")
    public static final class Builder {

        private static final int DEF_MAX_SELECTION = 1;

        private @MediaType int mediaType = MediaType.VIDEO;
        private @ScaleType int scaleType = ScaleType.SCALE_CROP_CENTER;

        private boolean galleryEnabled = true;
        private boolean flashEnabled = true;
        private boolean filtersEnabled = true;
        private boolean scaleTypeChangeable = true;
        private boolean cropEnabled = false;
        private int imgSize = 0;

        private int maxSelection = DEF_MAX_SELECTION;
        private String mediaDir;

        public MediaPickerOpts build() {
            if (mediaType == MediaType.VIDEO) {
                imgSize = 0;
                cropEnabled = false;
            }

            if (imgSize > 0) {
                cropEnabled = false;
                maxSelection = DEF_MAX_SELECTION;
            }

            if (cropEnabled) {
                maxSelection = DEF_MAX_SELECTION;
            }

            return new MediaPickerOpts(mediaType, scaleType, galleryEnabled, flashEnabled,
                    filtersEnabled, cropEnabled, imgSize, scaleTypeChangeable, maxSelection, mediaDir);
        }

        public Builder setMediaType(@MediaType int mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public Builder saveInDir(String dir) {
            this.mediaDir = dir;
            return this;
        }

        public Builder withGallery(boolean enabled) {
            galleryEnabled = enabled;
            return this;
        }

        public Builder withFilters(boolean enabled) {
            filtersEnabled = enabled;
            return this;
        }

        public Builder withCameraType(@ScaleType int type) {
            scaleType = type;
            return this;
        }

        public Builder withFlash(boolean enabled) {
            flashEnabled = true;
            return this;
        }

        public Builder canChangeScaleType(boolean enabled) {
            scaleTypeChangeable = enabled;
            return this;
        }

        public Builder withCropEnabled(boolean enabled) {
            cropEnabled = enabled;
            return this;
        }

        public Builder withImgSize(int size) {
            imgSize = (int) (size * Resources.getSystem().getDisplayMetrics().density);
            return this;
        }

        public Builder withMaxSelection(int count) {
            maxSelection = count;
            return this;
        }
    }
}