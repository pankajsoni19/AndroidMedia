package com.softwarejoint.media.camera;
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
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.TextView;

import com.softwarejoint.media.R;
import com.softwarejoint.media.encoder.MediaAudioEncoder;
import com.softwarejoint.media.encoder.MediaEncoder;
import com.softwarejoint.media.encoder.MediaMuxerWrapper;
import com.softwarejoint.media.encoder.MediaVideoEncoder;
import com.softwarejoint.media.enums.ScaleType;
import com.softwarejoint.media.fileio.FileHandler;
import com.softwarejoint.media.fileio.FilePathUtil;
import com.softwarejoint.media.glutils.GLDrawer2D;
import com.softwarejoint.media.utils.TimeParseUtils;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static android.app.Activity.RESULT_OK;

public class CameraFragment extends Fragment implements OnClickListener {

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
    private ImageView iv_vid_crop;
    private TextView txtVideoDur;
    private Timer timer;
    private @ScaleType
    int scaleType = ScaleType.SCALE_SQUARE;
    private Handler uiThreadHandler;

    /**
     * button for start/stop recording
     */
    private ImageView mRecordButton;
    /**
     * muxer for audio/video recording
     */
    private MediaMuxerWrapper mMuxer;

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


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        //noinspection ConstantConditions
        mediaPath = getArguments().getString(INTENT_FILEPATH);
        if (mediaPath == null) {
            mediaPath = FileHandler.getTempFile(getContext()).getPath();
        }

        uiThreadHandler = new Handler();
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
        iv_vid_crop = rootView.findViewById(R.id.iv_vid_crop);

        txtVideoDur = rootView.findViewById(R.id.video_dur);
        mRecordButton = rootView.findViewById(R.id.record_button);

        iv_filter.setOnClickListener(this);
        iv_gallery.setOnClickListener(this);

        rootView.findViewById(R.id.cancel).setOnClickListener(this);

        iv_vid_crop.setOnClickListener(this);
        mRecordButton.setOnClickListener(this);
        mFlashView.setOnClickListener(this);
        mCameraSwitcher.setOnClickListener(this);

        mCameraView.setScaleType(scaleType);
        mCameraView.setVideoSize();
        updateScaleUI();

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
        cancelTimer();
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        txtVideoDur.setVisibility(View.INVISIBLE);
        setColorFilter(mRecordButton, android.R.color.white);
        if (isRemoving() && adapter != null) {
            adapter.changeCursor(null);
        }
    }

    @Override
    public void onClick(final View view) {
        final int i = view.getId();

        if (i == R.id.iv_vid_crop) {
            toggleScaleType();
        } else if (i == R.id.iv_gallery) {
            openGallery();
        } else if (i == R.id.iv_filter) {
            toggleShowFilters();
        } else if (i == R.id.iv_flash) {
            mCameraView.toggleFlash();
        } else if (i == R.id.iv_switch_camera) {
            mCameraView.toggleCamera();
        } else if (i == R.id.record_button) {
            if (mMuxer == null) {
                startRecording();
            } else {
                stopRecording();
            }
        } else if (i == R.id.cancel) {
            //noinspection ConstantConditions
            getActivity().supportFinishAfterTransition();
        }
    }

    private void startTimer() {
        cancelTimer();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            private long startTime = System.currentTimeMillis();

            @Override
            public void run() {
                long delta = (System.currentTimeMillis() - startTime);
                String parsed = TimeParseUtils.getFormattedTimeHHMMSS(delta);
                uiThreadHandler.post(() -> txtVideoDur.setText(parsed));
            }
        }, 1000L, 1000L);
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private void setColorFilter(ImageView view, @ColorRes int colorRes) {
        @ColorInt int colorInt = ContextCompat.getColor(view.getContext(), colorRes);
        view.setColorFilter(colorInt);
    }

    private void toggleScaleType() {
        if (mCameraView.getScaleType() == ScaleType.SCALE_SQUARE) {
            mCameraView.setScaleType(ScaleType.SCALE_CROP_CENTER);
        } else {
            mCameraView.setScaleType(ScaleType.SCALE_SQUARE);
        }

        updateScaleUI();
    }

    private void updateScaleUI() {
        if (mCameraView.getScaleType() == ScaleType.SCALE_SQUARE) {
            iv_vid_crop.setImageResource(R.drawable.crop_square);
        } else {
            iv_vid_crop.setImageResource(R.drawable.crop_free);
        }
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
        startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_TAKE_GALLERY_VIDEO);
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

            if (ScaleType.SCALE_SQUARE == mCameraView.getScaleType()) {
                outputVideoWidth = DEF_VID_SIZE;
                outputVideoHeight = DEF_VID_SIZE;
            } else {
                outputVideoWidth = mCameraView.getVideoWidth();
                outputVideoHeight = mCameraView.getVideoHeight();
            }

            Log.d(TAG, "output: width: " + outputVideoWidth + " height: " + outputVideoHeight);
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

        iv_filter.setVisibility(View.GONE);
        grid_filters.setVisibility(View.GONE);
        iv_gallery.setVisibility(View.GONE);
        txt_gallery.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        mCameraSwitcher.setVisibility(View.GONE);
        setColorFilter(mRecordButton, android.R.color.holo_red_dark);
        txtVideoDur.setVisibility(View.VISIBLE);
        startTimer();

        mCameraView.onRecordingStart();
    }

    /**
     * request stop recording
     */
    private void stopRecording() {
        Log.d(TAG, "stopRecording:mMuxer=" + mMuxer);

        txtVideoDur.setVisibility(View.INVISIBLE);
        setColorFilter(mRecordButton, android.R.color.white);
        cancelTimer();

        mRecordButton.setColorFilter(0);  // return to default color

        mCameraView.onRecordingStop();

        if (mMuxer != null) {
            mMuxer.stopRecording();
            mMuxer = null;
            // you should not wait here
        }

        if (new File(mediaPath).exists()) {
            //noinspection ConstantConditions
            MediaScannerConnection.scanFile(getContext().getApplicationContext(), new String[]{
                    mediaPath
            }, new String[]{
                    "video/mp4"
            }, null);
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