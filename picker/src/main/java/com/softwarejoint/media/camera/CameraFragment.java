package com.softwarejoint.media.camera;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaActionSound;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.softwarejoint.media.R;
import com.softwarejoint.media.adapter.GalleryAdapter;
import com.softwarejoint.media.adapter.SelectedAdapter;
import com.softwarejoint.media.anim.AnimFadeReveal;
import com.softwarejoint.media.encoder.MediaAudioEncoder;
import com.softwarejoint.media.encoder.MediaEncoder;
import com.softwarejoint.media.encoder.MediaMuxerWrapper;
import com.softwarejoint.media.encoder.MediaVideoEncoder;
import com.softwarejoint.media.enums.MediaType;
import com.softwarejoint.media.enums.ScaleType;
import com.softwarejoint.media.fileio.FileHandler;
import com.softwarejoint.media.fileio.FilePathUtil;
import com.softwarejoint.media.glutils.GLDrawer2D;
import com.softwarejoint.media.image.ImageEffectFragment;
import com.softwarejoint.media.picker.MediaPickerOpts;
import com.softwarejoint.media.base.PickerFragment;
import com.softwarejoint.media.tasks.LoadGLImageTask;
import com.softwarejoint.media.tasks.LoadImageTask;
import com.softwarejoint.media.tasks.SaveGLImageTask;
import com.softwarejoint.media.utils.BitmapUtils;
import com.softwarejoint.media.utils.CameraHelper;
import com.softwarejoint.media.utils.TimeParseUtils;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

import static android.app.Activity.RESULT_OK;

public class CameraFragment extends PickerFragment implements OnClickListener {

    public final String TAG = "CameraFragment";

    private static final int REQUEST_GET_CONTENT = 1001;

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
    private TextView tv_gallery_effects;
    private AppCompatImageView iv_none_c, iv_duo_py_c, iv_cross_c, iv_negative_c, iv_duo_bw_c, iv_lomo_c, iv_fillight_c, iv_bw_c, iv_sepia_c;

    private RecyclerView recyclerView;
    private GalleryAdapter galleryAdapter;
    private SelectedAdapter selectedAdapter;

    private HorizontalScrollView hsv_effects;
    private ImageView iv_back;
    private ImageView iv_filter;
    private View txt_gallery;
    private ImageView iv_gallery;
    private ImageView iv_vid_crop;
    private TextView txtVideoDur;
    private View txt_done;

    private Timer timer;
    private Handler uiThreadHandler;

    /**
     * button for start/stop recording
     */
    private ImageView mRecordButton;
    /**
     * muxer for audio/video recording
     */
    private MediaMuxerWrapper mMuxer;
    private MediaPickerOpts opts;
    private MediaActionSound mediaActionSound;

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
        if (opts == null) {
            //noinspection ConstantConditions
            opts = getArguments().getParcelable(MediaPickerOpts.INTENT_OPTS);
        }
        uiThreadHandler = new Handler();
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_capture, container, false);

        mCameraView = rootView.findViewById(R.id.cameraView);
        iv_flash = rootView.findViewById(R.id.iv_flash);
        mCameraSwitcher = rootView.findViewById(R.id.iv_switch_camera);
        recyclerView = rootView.findViewById(R.id.gallery_previews);
        hsv_effects = rootView.findViewById(R.id.hsv_effects_capture);

        iv_filter = rootView.findViewById(R.id.iv_filter);

        iv_gallery = rootView.findViewById(R.id.iv_gallery);
        txt_gallery = rootView.findViewById(R.id.txt_gallery);
        iv_vid_crop = rootView.findViewById(R.id.iv_vid_crop);
        tv_gallery_effects = rootView.findViewById(R.id.tv_gallery_effects);

        iv_none_c = rootView.findViewById(R.id.iv_none_c);
        iv_duo_py_c = rootView.findViewById(R.id.iv_duotone_py_c);
        iv_cross_c = rootView.findViewById(R.id.iv_cross_c);
        iv_negative_c = rootView.findViewById(R.id.iv_negative_c);
        iv_duo_bw_c = rootView.findViewById(R.id.iv_duotone_bw_c);
        iv_lomo_c = rootView.findViewById(R.id.iv_lomo_c);
        iv_fillight_c = rootView.findViewById(R.id.iv_fillight_C);
        iv_bw_c = rootView.findViewById(R.id.iv_bw_c);
        iv_sepia_c = rootView.findViewById(R.id.iv_sepia_c);

        txtVideoDur = rootView.findViewById(R.id.video_dur);
        mRecordButton = rootView.findViewById(R.id.record_button);

        iv_back = rootView.findViewById(R.id.iv_back);
        txt_done = rootView.findViewById(R.id.txt_done);

        iv_none_c.setOnClickListener(this);
        iv_duo_py_c.setOnClickListener(this);
        iv_cross_c.setOnClickListener(this);
        iv_negative_c.setOnClickListener(this);
        iv_duo_bw_c.setOnClickListener(this);
        iv_lomo_c.setOnClickListener(this);
        iv_fillight_c.setOnClickListener(this);
        iv_bw_c.setOnClickListener(this);
        iv_sepia_c.setOnClickListener(this);

        txt_done.setOnClickListener(this);
        iv_back.setOnClickListener(this);

        mRecordButton.setOnClickListener(this);
        mCameraSwitcher.setOnClickListener(this);

        tv_gallery_effects.setText("Gallery");

        handleIntent();

        AnimFadeReveal.fadeIn(rootView);

        return rootView;
    }

    @SuppressWarnings("ConstantConditions")
    private void handleIntent() {
        mCameraView.init(opts.scaleType, opts.mediaType);
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

            if (opts.mediaType == MediaType.IMAGE) {
                iv_gallery.setImageResource(R.drawable.photo_library_white);
            } else {
                iv_gallery.setImageResource(R.drawable.video_library_white);
            }

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

        if (opts.showFilters()) {
            if (opts.mediaType == MediaType.IMAGE) {
                iv_filter.setImageResource(R.drawable.photo_filter);
            } else {
                iv_filter.setImageResource(R.drawable.movie_filter);
            }

            iv_filter.setVisibility(View.VISIBLE);
            iv_filter.setOnClickListener(this);
        } else {
            iv_filter.setVisibility(View.INVISIBLE);
            iv_filter.setOnClickListener(null);
        }

        mCameraView.setPreviewEnabled(opts.showFilters());
    }

    public boolean onBackPressed() {
        if (mCameraView.isFiltersPreviewVisible()) {
            mCameraView.toggleShowFilters();
            return true;
        }
        return false;
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
        mCameraView.setFrag(this);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause:");
        stopRecording();
        mCameraView.onPause();
        cancelTimer();

        if (mediaActionSound != null) {
            mediaActionSound.release();
            mediaActionSound = null;
        }

        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        txtVideoDur.setVisibility(View.INVISIBLE);
        if (isRemoving() && galleryAdapter != null) {
            galleryAdapter.changeCursor(null);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || requestCode != REQUEST_GET_CONTENT || data.getData() == null) {
            return;
        }

        Uri fileUri = data.getData();
        String filePath = FilePathUtil.getRealPath(getContext(), fileUri, opts);

        if (filePath == null) return;

        Log.d(TAG, "selectedPath: " + filePath);

        if (galleryAdapter != null) {
            galleryAdapter.addSelected(filePath);
        } else if (selectedAdapter != null) {
            selectedAdapter.addSelected(filePath);
        }

        checkIfMediaSelectionCompleted();
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
            if (opts.mediaType == MediaType.IMAGE) {
                playSound(MediaActionSound.SHUTTER_CLICK);
                takePicture();
            } else if (mMuxer == null) {
                playSound(MediaActionSound.START_VIDEO_RECORDING);
                startRecording();
            } else {
                stopRecording();
                uiThreadHandler.postDelayed(() -> playSound(MediaActionSound.STOP_VIDEO_RECORDING), 500L);
            }
        } else if (id == R.id.txt_done) {
            onClickDone();
        } else if (id == R.id.iv_back) {
            //noinspection ConstantConditions
            getActivity().setResult(Activity.RESULT_CANCELED);
            getActivity().supportFinishAfterTransition();
        } else if (id == R.id.iv_none_c){
            mCameraView.touched("none");
        }else if (id == R.id.iv_duotone_py_c){
        }else if (id == R.id.iv_cross_c){
        }else if (id == R.id.iv_negative_c){
            mCameraView.touched("invert");
        }else if (id == R.id.iv_duotone_bw_c){
        }else if (id == R.id.iv_lomo_c){
        }else if (id == R.id.iv_fillight_C){
        }else if (id == R.id.iv_bw_c){
            mCameraView.touched("bw");
        }else if (id == R.id.iv_sepia_c){
        }
    }

    public void toggleShowFilters() {
      //  if (mCameraView.toggleShowFilters())
        if (tv_gallery_effects.getText().equals("Gallery")){
           // iv_gallery.setVisibility(View.GONE);
            txt_gallery.setVisibility(View.GONE);
            recyclerView.setVisibility(View.INVISIBLE);
            hsv_effects.setVisibility(View.VISIBLE);
            tv_gallery_effects.setText("Effects");
           // mRecordButton.setVisibility(View.INVISIBLE);
          //  mRecordButton.setOnClickListener(null);
            if (opts.mediaType == MediaType.IMAGE){
                iv_filter.setImageResource(R.drawable.photo_library_white);
                } else {
                iv_filter.setImageResource(R.drawable.video_library_white);
                }
            } else {
            tv_gallery_effects.setText("Gallery");
            mRecordButton.setVisibility(View.VISIBLE);
            mRecordButton.setOnClickListener(this);

            if (opts.mediaType == MediaType.IMAGE){
                iv_filter.setImageResource(R.drawable.photo_filter);
            } else {
                iv_filter.setImageResource(R.drawable.movie_filter);
            }
            if (opts.galleryEnabled) {
                iv_gallery.setVisibility(View.VISIBLE);
                if (getListItemCount() == 0) {
                    txt_gallery.setVisibility(View.VISIBLE);
                }
            }
            hsv_effects.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
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
            iv_vid_crop.setImageResource(R.drawable.crop_portrait);
        } else {
            iv_vid_crop.setImageResource(R.drawable.crop_square);
        }
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);

        if (opts.mediaType == MediaType.IMAGE) {
            intent.setType("image/*");
            intent = Intent.createChooser(intent, "Select Image");
        } else {
            intent.setType("video/*");
            intent = Intent.createChooser(intent, "Select Video");
        }

        startActivityForResult(intent, REQUEST_GET_CONTENT);
    }

    private void takePicture() {
        mRecordButton.setVisibility(View.INVISIBLE);
        mRecordButton.setOnClickListener(null);
        mCameraView.queueEvent(() -> {
            GLDrawer2D drawer = mCameraView.getDrawer();
            int x = drawer.getStartX();
            int y = drawer.getStartY();
            int w = drawer.width();
            int h = drawer.height();

            int bitmapBuffer[] = BitmapUtils.readEGLBuffer(x, y, w, h);

            if (bitmapBuffer == null) return;

            if (opts.mediaType == MediaType.IMAGE && opts.cropEnabled) {
                ImageEffectFragment fragment = ImageEffectFragment.newInstance(opts);
                uiThreadHandler.post(() -> showFragment(fragment));
                new LoadGLImageTask(w, h, bitmapBuffer, fragment, opts).execute();
            } else {
                new SaveGLImageTask(w, h, bitmapBuffer, this, opts).execute();
            }
        });
    }

    public void onPictureSaved(String imagePath) {
        if (imagePath == null || !FileHandler.exists(imagePath)) {
            mRecordButton.setVisibility(View.VISIBLE);
            mRecordButton.setOnClickListener(this);
            return;
        }

        if (galleryAdapter != null) {
            galleryAdapter.addSelected(imagePath);
        } else if (selectedAdapter != null) {
            selectedAdapter.addSelected(imagePath);
        }

        MediaScannerConnection.MediaScannerConnectionClient callBack = null;

        if (!checkIfMediaSelectionCompleted()) {
            mRecordButton.setVisibility(View.VISIBLE);
            mRecordButton.setOnClickListener(this);

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
        scanFile(imagePath, "image/jpg", callBack);
    }

    /**
     * start recording
     * This is a sample project and call this on UI thread to avoid being complicated
     * but basically this should be called on private thread because prepareing
     * of encoder is heavy work
     */
    private void startRecording() {
        Log.d(TAG, "startRecording:");

        try {
            File mediaPath = FileHandler.getTempFile(opts);

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
            //delay is added as camera record sound gets recorded
            uiThreadHandler.postDelayed(() -> mMuxer.startRecording(), 500L);

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

        if (mediaPath == null || !FileHandler.exists(mediaPath)) return;

        Log.d(TAG, "recordedPath: " + mediaPath);

        mRecordButton.setImageResource(R.drawable.circle_done);

        if (galleryAdapter != null) {
            galleryAdapter.addSelected(mediaPath);
        } else if (selectedAdapter != null) {
            selectedAdapter.addSelected(mediaPath);
        }

        MediaScannerConnection.MediaScannerConnectionClient callBack = null;

        if (!checkIfMediaSelectionCompleted()) {
            if (opts.showFilters()) {
                iv_filter.setVisibility(View.VISIBLE);
            } else {
                iv_filter.setVisibility(View.INVISIBLE);
            }

            if (opts.galleryEnabled) {
                iv_gallery.setVisibility(View.VISIBLE);
                if (getListItemCount() == 0) {
                    txt_gallery.setVisibility(View.VISIBLE);
                }
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

        scanFile(mediaPath, "video/mp4", callBack);
    }

    private int getListItemCount() {
        RecyclerView.Adapter adapter = recyclerView.getAdapter();
        return adapter != null ? adapter.getItemCount() : 0;
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

        String[] projection;
        final String orderBy;
        Uri contentURI;

        if (opts.mediaType == MediaType.VIDEO) {
            projection = new String[]{
                    MediaStore.Video.Media._ID,
                    MediaStore.MediaColumns.DATA,
            };

            orderBy = MediaStore.Video.Media.DATE_TAKEN;
            contentURI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        } else {
            projection = new String[]{
                    MediaStore.Images.Media._ID,
                    MediaStore.MediaColumns.DATA,
            };

            orderBy = MediaStore.Images.Media.DATE_TAKEN;
            contentURI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        //noinspection ConstantConditions
        ContentResolver contentResolver = getContext().getContentResolver();

        Cursor cursor = contentResolver.query(contentURI,
                projection, null, null, orderBy + " DESC");

        if (cursor != null && cursor.moveToFirst()) {

            Log.d(TAG, "mediaCount: " + cursor.getCount());

            if (cursor.getCount() > 0) {
                txt_gallery.setVisibility(View.GONE);
            }

            if (galleryAdapter == null) {
                galleryAdapter = new GalleryAdapter(cursor, opts.maxSelection, opts.mediaType, this);
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

    public boolean checkIfMediaSelectionCompleted() {
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

    @SuppressWarnings("ConstantConditions")
    private void onClickDone() {
        ArrayList<String> items = new ArrayList<>();

        if (galleryAdapter != null) {
            galleryAdapter.fill(items);
            galleryAdapter.clearSelection();
        } else if (selectedAdapter != null) {
            selectedAdapter.fill(items);
            selectedAdapter.clearSelection();
        }

        //if (opts.mediaType == MediaType.IMAGE && opts.cropEnabled) { opts.cropEnabled not working
            if (opts.mediaType == MediaType.IMAGE) {
            ImageEffectFragment fragment = ImageEffectFragment.newInstance(opts);
                showFragment(fragment);
                new LoadImageTask(items.remove(0), fragment).execute();

                return;
        }

        if (opts.mediaType == MediaType.IMAGE && opts.imgSize > 0) {
            String imagePath = items.remove(0);
            String newPath = BitmapUtils.createCroppedBitmap(imagePath, opts);
            if (!imagePath.equals(newPath)) {
                scanFile(newPath, "image/jpg", null);
            }
            items.add(0, newPath);
        }

        Intent resultIntent = new Intent();
        resultIntent.putStringArrayListExtra(MediaPickerOpts.INTENT_RES, items);
        FragmentActivity activity = getActivity();
        activity.setResult(RESULT_OK, resultIntent);
        activity.supportFinishAfterTransition();
    }

    @SuppressWarnings("ConstantConditions")
    private void scanFile(String mediaPath, String mimeType, OnScanCompletedListener callback) {
        MediaScannerConnection.scanFile(getContext().getApplicationContext(), new String[]{
                mediaPath
        }, new String[]{
                mimeType
        }, callback);
    }

    private void playSound(int soundId) {
        if (mediaActionSound == null) {
            mediaActionSound = new MediaActionSound();
        }

        mediaActionSound.play(soundId);
    }
}