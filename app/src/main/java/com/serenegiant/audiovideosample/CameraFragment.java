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

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import com.serenegiant.encoder.MediaAudioEncoder;
import com.serenegiant.encoder.MediaEncoder;
import com.serenegiant.encoder.MediaMuxerWrapper;
import com.serenegiant.encoder.MediaVideoEncoder;
import com.serenegiant.glutils.GL1977Filter;
import com.serenegiant.glutils.GLArtFilter;
import com.serenegiant.glutils.GLBloomFilter;
import com.serenegiant.glutils.GLGrayscaleFilter;
import com.serenegiant.glutils.GLPosterizeFilter;
import com.serenegiant.glutils.GLToneCurveFilter;
import com.serenegiant.mediaaudiotest.R;
import java.io.IOException;

public class CameraFragment extends Fragment {
  private static final boolean DEBUG = false;  // TODO set false on release
  private static final String TAG = "CameraFragment";

  /**
   * for camera preview display
   */
  private CameraGLView mCameraView;
  /**
   * callback methods from encoder
   */
  private final MediaEncoder.MediaEncoderListener mMediaEncoderListener =
      new MediaEncoder.MediaEncoderListener() {
        @Override public void onPrepared(final MediaEncoder encoder) {
          if (DEBUG) Log.v(TAG, "onPrepared:encoder=" + encoder);
          if (encoder instanceof MediaVideoEncoder) {
            mCameraView.setVideoEncoder((MediaVideoEncoder) encoder);
          }
        }

        @Override public void onStopped(final MediaEncoder encoder) {
          if (DEBUG) Log.v(TAG, "onStopped:encoder=" + encoder);
          if (encoder instanceof MediaVideoEncoder) mCameraView.setVideoEncoder(null);
        }
      };
  /**
   * for scale mode display
   */
  private TextView mScaleModeView;
  /**
   * button for start/stop recording
   */
  private ImageButton mRecordButton;
  /**
   * muxer for audio/video recording
   */
  private MediaMuxerWrapper mMuxer;
  /**
   * method when touch record button
   */
  private final OnClickListener mOnClickListener = new OnClickListener() {
    @Override public void onClick(final View view) {
      switch (view.getId()) {
        case R.id.cameraView:
          final int scale_mode = (mCameraView.getScaleMode() + 1) % 4;
          mCameraView.setScaleMode(scale_mode);
          updateScaleModeText();
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

  public CameraFragment() {
    // need default constructor
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
      final Bundle savedInstanceState) {
    final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
    mCameraView = (CameraGLView) rootView.findViewById(R.id.cameraView);
    mCameraView.setVideoSize(1280, 720);
    mCameraView.setOnClickListener(mOnClickListener);
    mScaleModeView = (TextView) rootView.findViewById(R.id.scalemode_textview);
    updateScaleModeText();
    mRecordButton = (ImageButton) rootView.findViewById(R.id.record_button);
    mRecordButton.setOnClickListener(mOnClickListener);
    return rootView;
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.main, menu);
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
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
      case R.id.action_tone_curve:
        GLToneCurveFilter toneCurveFilter = new GLToneCurveFilter();
        toneCurveFilter.setFromCurveFileInputStream(getActivity().getResources().openRawResource(R.raw.frozen));
        mCameraView.setDrawer(toneCurveFilter);
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override public void onResume() {
    super.onResume();
    if (DEBUG) Log.v(TAG, "onResume:");
    mCameraView.onResume();
  }

  @Override public void onPause() {
    if (DEBUG) Log.v(TAG, "onPause:");
    stopRecording();
    mCameraView.onPause();
    super.onPause();
  }

  private void updateScaleModeText() {
    final int scale_mode = mCameraView.getScaleMode();
    mScaleModeView.setText(scale_mode == 0 ? "scale to fit"
        : (scale_mode == 1 ? "keep aspect(viewport)" : (scale_mode == 2 ? "keep aspect(matrix)"
            : (scale_mode == 3 ? "keep aspect(crop center)" : ""))));
  }

  /**
   * start resorcing
   * This is a sample project and call this on UI thread to avoid being complicated
   * but basically this should be called on private thread because prepareing
   * of encoder is heavy work
   */
  private void startRecording() {
    if (DEBUG) Log.v(TAG, "startRecording:");
    try {
      mRecordButton.setColorFilter(0xffff0000);  // turn red
      mMuxer = new MediaMuxerWrapper(".mp4");  // if you record audio only, ".m4a" is also OK.
      if (true) {
        // for video capturing
        new MediaVideoEncoder(mMuxer, mMediaEncoderListener, mCameraView.getVideoWidth(),
            mCameraView.getVideoHeight(), mCameraView.getDrawer());
      }
      if (true) {
        // for audio capturing
        new MediaAudioEncoder(mMuxer, mMediaEncoderListener);
      }
      mMuxer.prepare();
      mMuxer.startRecording();
    } catch (final IOException e) {
      mRecordButton.setColorFilter(0);
      Log.e(TAG, "startCapture:", e);
    }
  }

  /**
   * request stop recording
   */
  private void stopRecording() {
    if (DEBUG) Log.v(TAG, "stopRecording:mMuxer=" + mMuxer);
    mRecordButton.setColorFilter(0);  // return to default color
    if (mMuxer != null) {
      mMuxer.stopRecording();
      mMuxer = null;
      // you should not wait here
    }
  }
}
