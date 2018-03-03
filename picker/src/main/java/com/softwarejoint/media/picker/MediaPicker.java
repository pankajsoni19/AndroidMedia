package com.softwarejoint.media.picker;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;

import com.softwarejoint.media.camera.PickerActivity;
import com.softwarejoint.media.enums.MediaType;
import com.softwarejoint.media.enums.ScaleType;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 03/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class MediaPicker implements Parcelable {

    private static final int REQUEST_CODE = 1003;

    private static final String INTENT_OPTS = "com.softwarejoint.media.opts";
    private static final String INTENT_RES = "com.softwarejoint.media.result";

    private final int mediaType;
    private final boolean galleryEnabled;
    private final boolean cameraEnabled;
    private final int scaleType;
    private final boolean frontCamera;
    private final boolean flashEnabled;
    private int maxPicks;

    private MediaPicker(@MediaType int mediaType, boolean galleryEnabled, boolean cameraEnabled,
                        @ScaleType int scaleType, boolean frontCamera, boolean flashEnabled, int maxPicks) {

        this.mediaType = mediaType;
        this.galleryEnabled = galleryEnabled;
        this.cameraEnabled = cameraEnabled;
        this.scaleType = scaleType;
        this.frontCamera = frontCamera;
        this.flashEnabled = flashEnabled;
        this.maxPicks = maxPicks;
    }

    private MediaPicker(Parcel in) {
        mediaType = in.readInt();
        galleryEnabled = in.readByte() != 0;
        cameraEnabled = in.readByte() != 0;
        scaleType = in.readInt();
        frontCamera = in.readByte() != 0;
        flashEnabled = in.readByte() != 0;
        maxPicks = in.readInt();
    }

    public static final Creator<MediaPicker> CREATOR = new Creator<MediaPicker>() {
        @Override
        public MediaPicker createFromParcel(Parcel in) {
            return new MediaPicker(in);
        }

        @Override
        public MediaPicker[] newArray(int size) {
            return new MediaPicker[size];
        }
    };

    public static @Nullable
    Result onActivityResult(int requestCode, int resultCode, Intent data) {
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
        dest.writeByte((byte) (galleryEnabled ? 1 : 0));
        dest.writeByte((byte) (cameraEnabled ? 1 : 0));
        dest.writeInt(scaleType);
        dest.writeByte((byte) (frontCamera ? 1 : 0));
        dest.writeByte((byte) (flashEnabled ? 1 : 0));
        dest.writeInt(maxPicks);
    }

    @SuppressWarnings("unused")
    public static final class Builder {

        private @MediaType
        int mediaType;
        private boolean galleryEnabled = true;
        private boolean cameraEnabled = true;
        private @ScaleType
        int scaleType = ScaleType.SCALE_CROP_CENTER;
        private boolean frontCamera = true;
        private boolean flashEnabled = true;
        private int maxPicks;

        public void startActivity(Activity activity) {
            MediaPicker opts =
                    new MediaPicker(mediaType, galleryEnabled, cameraEnabled, scaleType,
                            frontCamera, flashEnabled, maxPicks);

            Intent newIntent = new Intent(activity, PickerActivity.class);
            newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            newIntent.putExtra(INTENT_OPTS, opts);

            //noinspection unchecked
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity);
            ActivityCompat.startActivityForResult(activity, newIntent, REQUEST_CODE, options.toBundle());
        }

        public void startActivity(Fragment fragment) {
            startActivity(fragment.getActivity());
        }

        public Builder setMediaType(@MediaType int mediaType) {
            this.mediaType = mediaType;
            return this;
        }


        public Builder withGallery(boolean enabled) {
            galleryEnabled = enabled;
            return this;
        }

        public Builder withCamera(boolean enabled) {
            cameraEnabled = enabled;
            return this;
        }

        public Builder withCameraType(@ScaleType int type) {
            scaleType = type;
            cameraEnabled = true;
            return this;
        }

        public Builder withCameraFront(boolean enabled) {
            cameraEnabled = true;
            frontCamera = enabled;
            return this;
        }

        public Builder withFlash(Boolean enabled) {
            flashEnabled = true;
            cameraEnabled = true;
            return this;
        }

        public Builder withMaxPick(int maxPicks) {
            this.maxPicks = maxPicks;
            return this;
        }
    }
}