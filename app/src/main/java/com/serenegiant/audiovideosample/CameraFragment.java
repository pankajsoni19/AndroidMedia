package com.serenegiant.audiovideosample;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: CameraFragment.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.serenegiant.encoder.MediaAudioEncoder;
import com.serenegiant.encoder.MediaEncoder;
import com.serenegiant.encoder.MediaMuxerWrapper;
import com.serenegiant.encoder.MediaVideoEncoder;
import com.serenegiant.enums.ScaleType;
import com.serenegiant.fileio.FileHandler;
import com.serenegiant.glutils.GL1977Filter;
import com.serenegiant.glutils.GLArtFilter;
import com.serenegiant.glutils.GLBloomFilter;
import com.serenegiant.glutils.GLColorInvertFilter;
import com.serenegiant.glutils.GLGrayscaleFilter;
import com.serenegiant.glutils.GLPosterizeFilter;
import com.serenegiant.mediaaudiotest.R;

import java.io.IOException;

public class CameraFragment extends Fragment {

    public final String TAG = "CameraFragment";

    private static final int SIZE = 480;

    private static final String INTENT_FILEPATH = "com.wafer.picker.image.file";

    public static CameraFragment newInstance() {
        return newInstance(null);
    }

    @SuppressWarnings("SameParameterValue")
    public static CameraFragment newInstance(@Nullable String mediaPath) {
        Bundle args = new Bundle();
        args.putString(INTENT_FILEPATH, mediaPath);
        CameraFragment fragment = new CameraFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * for camera preview display
     */
    private CameraGLView mCameraView;
    private ImageView mFlashView;
    private ImageView mCameraSwitcher;
    private String mediaPath;
    private RecyclerView recyclerView;
    private ImageView ivGallery;
    private GalleryAdapter adapter;

    /**
     * callback methods from encoder
     */
    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener =
            new MediaEncoder.MediaEncoderListener() {
                @Override
                public void onPrepared(final MediaEncoder encoder) {
                    Log.d(TAG, "onPrepared:encoder=" + encoder);
                    if (encoder instanceof MediaVideoEncoder) {
                        mCameraView.setVideoEncoder((MediaVideoEncoder) encoder);
                    }
                }

                @Override
                public void onStopped(final MediaEncoder encoder) {
                    Log.d(TAG, "onStopped:encoder=" + encoder);
                    if (encoder instanceof MediaVideoEncoder) mCameraView.setVideoEncoder(null);
                }
            };

    /**
     * button for start/stop recording
     */
    private ImageView mRecordButton;
    /**
     * muxer for audio/video recording
     */
    private MediaMuxerWrapper mMuxer;
    /**
     * method when touch record button
     */
    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View view) {
            switch (view.getId()) {
                case R.id.iv_flash:
                    mCameraView.toggleFlash();
                    break;
                case R.id.iv_switch_camera:
                    mCameraView.toggleCamera();
                    break;
                case R.id.record_button:
                    if (mMuxer == null) {
                        startRecording();
                    } else {
                        stopRecording();
                    }
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        //noinspection ConstantConditions
        mediaPath = getArguments().getString(INTENT_FILEPATH);
        if (mediaPath == null) {
            mediaPath = FileHandler.getTempFile(getContext()).getPath();
        }
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mCameraView = rootView.findViewById(R.id.cameraView);
        mFlashView = rootView.findViewById(R.id.iv_flash);
        mCameraSwitcher = rootView.findViewById(R.id.iv_switch_camera);
        recyclerView = rootView.findViewById(R.id.gallery_previews);
        ivGallery = rootView.findViewById(R.id.iv_gallery);

        mCameraView.setOnClickListener(mOnClickListener);
        mRecordButton = rootView.findViewById(R.id.record_button);
        mRecordButton.setOnClickListener(mOnClickListener);
        mFlashView.setOnClickListener(mOnClickListener);
        mCameraSwitcher.setOnClickListener(mOnClickListener);

        mCameraView.setVideoSize();

        loadAdapter();
        return rootView;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isRemoving()) {
            adapter.changeCursor(null);
        }
    }

    public void loadAdapter() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);

        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.MediaColumns.DATA,
                MediaStore.Video.Thumbnails.DATA
        };

        final String orderBy = MediaStore.Images.Media.DATE_TAKEN;
        //noinspection ConstantConditions
        ContentResolver contentResolver = getContext().getContentResolver();
        Cursor cursor = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, orderBy + " DESC");

        if (cursor != null && cursor.moveToFirst()) {

            if (adapter == null) {
                adapter = new GalleryAdapter(cursor);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.changeCursor(cursor);
            }
        }
    }

    private void onFilterSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_posterize:
                mCameraView.setDrawer(new GLPosterizeFilter());
                break;
            case R.id.action_grayscale:
                mCameraView.setDrawer(new GLGrayscaleFilter());
                break;
            case R.id.action_art:
                mCameraView.setDrawer(new GLArtFilter());
                break;
            case R.id.action_1977:
                mCameraView.setDrawer(new GL1977Filter());
                break;
            case R.id.action_bloom:
                mCameraView.setDrawer(new GLBloomFilter());
                break;
            case R.id.action_invert:
                mCameraView.setDrawer(new GLColorInvertFilter());
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume:");
        mCameraView.onResume();
        mCameraView.setFlashImageView(mFlashView);
        mCameraView.setCameraSwitcher(mCameraSwitcher);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause:");
        stopRecording();
        mCameraView.onPause();
        super.onPause();
    }

    /**
     * start resorcing
     * This is a sample project and call this on UI thread to avoid being complicated
     * but basically this should be called on private thread because prepareing
     * of encoder is heavy work
     */
    private void startRecording() {
        Log.d(TAG, "startRecording:");

        try {
            mRecordButton.setColorFilter(0xffff0000);  // turn red
            mMuxer = new MediaMuxerWrapper(mediaPath);  // if you record audio only, ".m4a" is also OK.

            int outputVideoWidth;
            int outputVideoHeight;

            if (mCameraView.getScaleMode() == ScaleType.SCALE_SQUARE) {
                outputVideoWidth = SIZE;
                outputVideoHeight = SIZE;
            } else {
                outputVideoWidth = mCameraView.getVideoWidth();
                outputVideoHeight = mCameraView.getVideoHeight();
            }

            Log.d(TAG, "output: width: " + outputVideoWidth + " height: " + outputVideoWidth);
            // for video capturing
            new MediaVideoEncoder(mMuxer, mMediaEncoderListener, outputVideoWidth,
                    outputVideoHeight, mCameraView.getDrawer());

            // for audio capturing
            new MediaAudioEncoder(mMuxer, mMediaEncoderListener);

            mMuxer.prepare();
            mMuxer.startRecording();
        } catch (final IOException e) {
            mRecordButton.setColorFilter(0);
            Log.e(TAG, "startCapture:", e);
            return;
        }

        mCameraView.onRecordingStart();
    }

    /**
     * request stop recording
     */
    private void stopRecording() {
        Log.d(TAG, "stopRecording:mMuxer=" + mMuxer);

        mRecordButton.setColorFilter(0);  // return to default color

        mCameraView.onRecordingStop();

        if (mMuxer != null) {
            mMuxer.stopRecording();
            mMuxer = null;
            // you should not wait here
        }
    }
}