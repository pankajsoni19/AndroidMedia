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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;

import com.serenegiant.encoder.MediaAudioEncoder;
import com.serenegiant.encoder.MediaEncoder;
import com.serenegiant.encoder.MediaMuxerWrapper;
import com.serenegiant.encoder.MediaVideoEncoder;
import com.serenegiant.enums.ScaleType;
import com.serenegiant.fileio.FileHandler;
import com.serenegiant.fileio.FilePathUtil;
import com.serenegiant.glutils.GLDrawer2D;
import com.serenegiant.mediaaudiotest.R;

import java.io.IOException;

import static android.app.Activity.RESULT_OK;

public class CameraFragment extends Fragment {

    public final String TAG = "CameraFragment";

    private static final int DEF_VID_SIZE = 480;
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 1002;

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
    private GalleryAdapter adapter;
    private View txt_gallery, iv_filter;
    private GridView grid_filters;
    private View iv_gallery;
    private View separator;

    private @ScaleType int scaleType = ScaleType.SCALE_CROP_CENTER;

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
                case R.id.iv_gallery:
                    openGallery();
                    break;
                case R.id.iv_filter:
                    toggleShowFilters();
                    break;
                case R.id.iv_flash:
                    mCameraView.toggleFlash();
                    break;
                case R.id.iv_switch_camera:
                    mCameraView.toggleCamera();
                    break;
                case R.id.record_button:
                    if (mMuxer == null) {
                        iv_filter.setVisibility(View.GONE);
                        grid_filters.setVisibility(View.GONE);
                        iv_gallery.setVisibility(View.GONE);
                        setColorFilter(mRecordButton, android.R.color.holo_red_dark);
                        startRecording();
                    } else {
                        setColorFilter(mRecordButton, android.R.color.white);
                        stopRecording();
                        Log.d(TAG, "file recorded");
                    }
                    break;
                case R.id.cancel:
                    getActivity().finish();
                    break;
            }
        }
    };

    private void setColorFilter(ImageView view, @ColorRes int colorRes) {
        @ColorInt int colorInt = ContextCompat.getColor(view.getContext(), colorRes);
        view.setColorFilter(colorInt);

    }

    private void toggleShowFilters() {
        if (grid_filters.getVisibility() == View.VISIBLE) {
            grid_filters.animate().alpha(0).setDuration(300).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    grid_filters.setVisibility(View.GONE);
                }
            }).start();
        } else {
            grid_filters.setAlpha(0);
            grid_filters.setVisibility(View.VISIBLE);
            grid_filters.animate().alpha(1).setDuration(300).setListener(null).start();
        }
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Video"), REQUEST_TAKE_GALLERY_VIDEO);
    }

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
        grid_filters = rootView.findViewById(R.id.grid_filters);

        iv_filter = rootView.findViewById(R.id.iv_filter);

        iv_gallery = rootView.findViewById(R.id.iv_gallery);
        txt_gallery = rootView.findViewById(R.id.txt_gallery);
        separator = rootView.findViewById(R.id.separator);

        iv_filter.setOnClickListener(mOnClickListener);

        iv_gallery.setOnClickListener(mOnClickListener);
        rootView.findViewById(R.id.cancel).setOnClickListener(mOnClickListener);

        mRecordButton = rootView.findViewById(R.id.record_button);
        mRecordButton.setOnClickListener(mOnClickListener);
        mFlashView.setOnClickListener(mOnClickListener);
        mCameraSwitcher.setOnClickListener(mOnClickListener);

        mCameraView.setVideoSize();

        grid_filters.setAdapter(new FilterAdapter());

        grid_filters.setOnItemClickListener((parent, view, position, id) -> {
            FilterAdapter adapter = (FilterAdapter) parent.getAdapter();
            adapter.markSelected(position);
            GLDrawer2D filter = adapter.getItem(position);
            mCameraView.setDrawer(filter);
        });

        loadVideoAdapter();
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        iv_filter.setVisibility(View.VISIBLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || requestCode != REQUEST_TAKE_GALLERY_VIDEO || data.getData() == null) {
            return;
        }

        Uri fileUri = data.getData();
        String filePath = FilePathUtil.getRealPath(getContext(), fileUri);

        Log.d(TAG, "selectedPath: " + filePath);
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


    @Override
    public void onStop() {
        super.onStop();
        if (isRemoving() && adapter != null) {
            adapter.changeCursor(null);
        }
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

            if (ScaleType.SCALE_SQUARE == mCameraView.getScaleMode()) {
                outputVideoWidth = DEF_VID_SIZE;
                outputVideoHeight = DEF_VID_SIZE;
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

    public void loadVideoAdapter() {
        txt_gallery.setVisibility(View.VISIBLE);

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

            Log.d(TAG, "mediaCount: " + cursor.getCount());

            if (cursor.getCount() > 0) {
                txt_gallery.setVisibility(View.GONE);
            }

            if (adapter == null) {
                adapter = new GalleryAdapter(getContext(), cursor);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.changeCursor(cursor);
            }
        }
    }
}