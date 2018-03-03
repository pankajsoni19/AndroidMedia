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

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.softwarejoint.media.R;
import com.softwarejoint.media.adapter.GalleryAdapter;
import com.softwarejoint.media.adapter.SelectedAdapter;
import com.softwarejoint.media.encoder.MediaAudioEncoder;
import com.softwarejoint.media.encoder.MediaEncoder;
import com.softwarejoint.media.encoder.MediaMuxerWrapper;
import com.softwarejoint.media.encoder.MediaVideoEncoder;
import com.softwarejoint.media.enums.ScaleType;
import com.softwarejoint.media.fileio.FileHandler;
import com.softwarejoint.media.fileio.FilePathUtil;
import com.softwarejoint.media.glutils.GLDrawer2D;
import com.softwarejoint.media.picker.MediaPickerOpts;
import com.softwarejoint.media.utils.CameraHelper;
import com.softwarejoint.media.utils.TimeParseUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static android.app.Activity.RESULT_OK;

public class CameraFragment extends Fragment implements OnClickListener {

    public final String TAG = "CameraFragment";

    private static final int REQUEST_CODE_FILTER = 10001;
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 1002;

    private static final int DEF_VID_SIZE = 480;

    public static CameraFragment newInstance(@NonNull MediaPickerOpts opts) {
        Bundle args = new Bundle();
        args.putParcelable(MediaPickerOpts.INTENT_OPTS, opts);
        CameraFragment fragment = new CameraFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * for camera preview display
     */
    private CameraGLView mCameraView;
    private ImageView iv_flash;
    private ImageView mCameraSwitcher;

    private RecyclerView recyclerView;
    private GalleryAdapter galleryAdapter;
    private SelectedAdapter selectedAdapter;

    private ImageView iv_back;
    private View txt_gallery, iv_filter;
    private View iv_gallery;
    private ImageView iv_vid_crop;
    private TextView txtVideoDur;
    private View txt_done;

    private Timer timer;
    private Handler uiThreadHandler;
    private FilterPreviewDialogFragment filterPreviewDialog;

    /**
     * button for start/stop recording
     */
    private ImageView mRecordButton;
    /**
     * muxer for audio/video recording
     */
    private MediaMuxerWrapper mMuxer;
    private MediaPickerOpts opts;

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
        opts = getArguments().getParcelable(MediaPickerOpts.INTENT_OPTS);
        uiThreadHandler = new Handler();
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mCameraView = rootView.findViewById(R.id.cameraView);
        iv_flash = rootView.findViewById(R.id.iv_flash);
        mCameraSwitcher = rootView.findViewById(R.id.iv_switch_camera);
        recyclerView = rootView.findViewById(R.id.gallery_previews);

        iv_filter = rootView.findViewById(R.id.iv_filter);

        iv_gallery = rootView.findViewById(R.id.iv_gallery);
        txt_gallery = rootView.findViewById(R.id.txt_gallery);
        iv_vid_crop = rootView.findViewById(R.id.iv_vid_crop);

        txtVideoDur = rootView.findViewById(R.id.video_dur);
        mRecordButton = rootView.findViewById(R.id.record_button);

        iv_back = rootView.findViewById(R.id.iv_back);
        txt_done = rootView.findViewById(R.id.txt_done);

        txt_done.setOnClickListener(this);
        iv_back.setOnClickListener(this);

        mRecordButton.setOnClickListener(this);
        mCameraSwitcher.setOnClickListener(this);

        handleIntent();

        return rootView;
    }

    @SuppressWarnings("ConstantConditions")
    private void handleIntent() {
        mCameraView.setScaleType(opts.scaleType);
        mCameraView.setVideoSize();
        updateScaleUI();

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);

        if (opts.scaleTypeChangeable) {
            iv_vid_crop.setOnClickListener(this);
        } else {
            iv_vid_crop.setVisibility(View.INVISIBLE);
            iv_vid_crop.setOnClickListener(null);
        }

        if (opts.galleryEnabled) {
            loadGalleryAdapter();
            iv_gallery.setOnClickListener(this);
        } else {
            iv_gallery.setVisibility(View.INVISIBLE);
            txt_gallery.setVisibility(View.INVISIBLE);
            iv_gallery.setOnClickListener(null);
            loadSelectedAdapter();
        }

        if (opts.flashEnabled) {
            iv_flash.setOnClickListener(this);
        } else {
            iv_flash.setOnClickListener(null);
        }

        if (opts.filtersEnabled) {
            iv_filter.setOnClickListener(this);
        } else {
            iv_filter.setVisibility(View.INVISIBLE);
            iv_filter.setOnClickListener(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume:");
        mCameraView.onResume();

        if (opts.flashEnabled) {
            mCameraView.setFlashImageView(iv_flash);
        }

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
        if (isRemoving() && galleryAdapter != null) {
            galleryAdapter.changeCursor(null);
        }

        if (filterPreviewDialog != null) {
            filterPreviewDialog.dismissDialog();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || requestCode != REQUEST_TAKE_GALLERY_VIDEO || data.getData() == null) {
            return;
        }

        Uri fileUri = data.getData();
        String filePath = FilePathUtil.getRealPath(getContext(), fileUri);

        if (filePath == null) {
            return;
        }

        Log.d(TAG, "selectedPath: " + filePath);

        if (galleryAdapter != null) {
            galleryAdapter.addSelected(filePath);
        } else if (selectedAdapter != null) {
            selectedAdapter.addSelected(filePath);
        }

        onMediaSelectionUpdated();
    }

    @Override
    public void onClick(final View view) {
        final int id = view.getId();

        if (id == R.id.iv_vid_crop) {
            toggleScaleType();
        } else if (id == R.id.iv_gallery) {
            openGallery();
        } else if (id == R.id.iv_filter) {
            toggleShowFilters();
        } else if (id == R.id.iv_flash) {
            mCameraView.toggleFlash();
        } else if (id == R.id.iv_switch_camera) {
            mCameraView.toggleCamera();
        } else if (id == R.id.record_button) {
            if (mMuxer == null) {
                startRecording();
            } else {
                stopRecording();
            }
        } else if (id == R.id.txt_done) {
            onClickDone();
        } else if (id == R.id.iv_back) {
            //noinspection ConstantConditions
            getActivity().supportFinishAfterTransition();
        }
    }

    private void startTimer() {
        cancelTimer();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            private long startTime = System.currentTimeMillis() - 200L;

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
        if (filterPreviewDialog == null) {
            filterPreviewDialog = new FilterPreviewDialogFragment();
            filterPreviewDialog.setTargetFragment(this, REQUEST_CODE_FILTER);
        }

        //noinspection ConstantConditions
        filterPreviewDialog.show(getFragmentManager(), filterPreviewDialog.TAG);
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
            File mediaPath = FileHandler.getTempFile(opts.mediaDir);

            mMuxer = new MediaMuxerWrapper(mediaPath.getPath());  // if you record audio only, ".m4a" is also OK.

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
            Log.e(TAG, "startCapture:", e);
            return;
        }

        iv_vid_crop.setVisibility(View.GONE);
        iv_filter.setVisibility(View.GONE);

        iv_gallery.setVisibility(View.GONE);
        txt_gallery.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        mCameraSwitcher.setVisibility(View.GONE);
        iv_back.setVisibility(View.GONE);

        if (filterPreviewDialog != null) {
            filterPreviewDialog.dismissDialog();
        }

        mRecordButton.setImageResource(R.drawable.circle_ringed_red_white);
        txtVideoDur.setText(R.string.video_start_time);
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
        iv_back.setVisibility(View.VISIBLE);

        cancelTimer();

        mCameraView.onRecordingStop();

        if (mMuxer == null) {
            return;
        }

        mMuxer.stopRecording();
        String mediaPath = mMuxer.getOutputPath();
        mMuxer = null;

        if (mediaPath == null || !FileHandler.exists(mediaPath)) {
            onMediaSelectionUpdated();
            return;
        }

        Log.d(TAG, "recordedPath: " + mediaPath);

        mRecordButton.setImageResource(R.drawable.circle_done);

        if (galleryAdapter != null) {
            galleryAdapter.addSelected(mediaPath);
        } else if (selectedAdapter != null) {
            selectedAdapter.addSelected(mediaPath);
        }

        MediaScannerConnection.MediaScannerConnectionClient callBack = null;

        if (onMediaSelectionUpdated()) {
            onClickDone();
        } else {

            if (opts.filtersEnabled) {
                iv_filter.setVisibility(View.VISIBLE);
            } else {
                iv_filter.setVisibility(View.INVISIBLE);
            }

            if (opts.galleryEnabled) {
                iv_gallery.setVisibility(View.VISIBLE);
                txt_gallery.setVisibility(View.VISIBLE);
            } else {
                iv_gallery.setVisibility(View.INVISIBLE);
            }

            if (opts.scaleTypeChangeable) {
                iv_vid_crop.setVisibility(View.VISIBLE);
            } else {
                iv_vid_crop.setVisibility(View.INVISIBLE);
            }

            recyclerView.setVisibility(View.VISIBLE);

            if (CameraHelper.isFrontCameraAvailable(getContext())) {
                mCameraSwitcher.setVisibility(View.VISIBLE);
            }

            iv_back.setVisibility(View.VISIBLE);
            txtVideoDur.setVisibility(View.GONE);

            callBack = new MediaScannerConnection.MediaScannerConnectionClient() {
                @Override
                public void onMediaScannerConnected() {

                }

                @Override
                public void onScanCompleted(String path, Uri uri) {
                    uiThreadHandler.post(() -> refreshAdapters());
                }
            };
        }

        //noinspection ConstantConditions
        MediaScannerConnection.scanFile(getContext().getApplicationContext(), new String[]{
                mediaPath
        }, new String[]{
                "video/mp4"
        }, callBack);
    }

    private void refreshAdapters() {
        if (galleryAdapter != null) {
            loadGalleryAdapter();
        } else if (selectedAdapter != null) {
            selectedAdapter.notifyDataSetChanged();
        }
    }

    public void loadGalleryAdapter() {
        txt_gallery.setVisibility(View.VISIBLE);

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

            if (galleryAdapter == null) {
                galleryAdapter = new GalleryAdapter(cursor, opts.maxSelection, this);
            } else {
                galleryAdapter.changeCursor(cursor);
            }

            recyclerView.setAdapter(galleryAdapter);
        }
    }

    private void loadSelectedAdapter() {
        if (selectedAdapter == null) {
            selectedAdapter = new SelectedAdapter(opts.maxSelection, this);
        }

        recyclerView.setAdapter(selectedAdapter);
    }

    public boolean onMediaSelectionUpdated() {
        int selectionCount = 0;

        if (galleryAdapter != null) {
            selectionCount = galleryAdapter.getSelectionCount();
        } else if (selectedAdapter != null) {
            selectionCount = selectedAdapter.getSelectionCount();
        }

        mRecordButton.setImageResource(R.drawable.ring_white);

        if (selectionCount == opts.maxSelection) {
            Log.d(TAG, "onMaxSelection");
            uiThreadHandler.post(this::onClickDone);
            return true;
        }

        if (selectionCount == 0) {
            txt_done.setVisibility(View.GONE);
        } else {
            txt_done.setVisibility(View.VISIBLE);
        }

        return false;
    }

    private void onClickDone() {
        ArrayList<String> items = new ArrayList<>();

        if (galleryAdapter != null) {
            galleryAdapter.fill(items);
        } else if (selectedAdapter != null) {
            selectedAdapter.fill(items);
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra(MediaPickerOpts.INTENT_RES, items);
        FragmentActivity activity = getActivity();
        //noinspection ConstantConditions
        activity.setResult(RESULT_OK, resultIntent);
        activity.supportFinishAfterTransition();
    }

    public void onFilterSelected(GLDrawer2D filter) {
        mCameraView.setDrawer(filter);
    }
}