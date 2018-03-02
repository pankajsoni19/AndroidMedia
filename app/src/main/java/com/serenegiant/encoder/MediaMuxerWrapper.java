package com.serenegiant.encoder;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: MediaMuxerWrapper.java
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

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaScannerConnection;
import android.util.Log;

import com.serenegiant.MainApp;
import com.serenegiant.mediaaudiotest.BuildConfig;

import java.io.IOException;
import java.nio.ByteBuffer;

@SuppressWarnings("WeakerAccess")
public class MediaMuxerWrapper {

    private static final String TAG = "MediaMuxerWrapper";

    private final MediaMuxer mMediaMuxer;  // API >= 18
    private int mEncoderCount, mStartedCount;
    private boolean mIsStarted;
    private MediaEncoder mVideoEncoder, mAudioEncoder;

    /**
     * Constructor
     *
     * @param filePath output file path
     * @throws IOException
     */
    public MediaMuxerWrapper(String filePath) throws IOException {
        mMediaMuxer = new MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        mEncoderCount = mStartedCount = 0;
        mIsStarted = false;
    }

    public void prepare() throws IOException {
        if (mVideoEncoder != null) mVideoEncoder.prepare();
        if (mAudioEncoder != null) mAudioEncoder.prepare();
    }

    public void startRecording() {
        if (mVideoEncoder != null) mVideoEncoder.startRecording();
        if (mAudioEncoder != null) mAudioEncoder.startRecording();
    }

    public void stopRecording() {
        if (mVideoEncoder != null) mVideoEncoder.stopRecording();
        mVideoEncoder = null;
        if (mAudioEncoder != null) mAudioEncoder.stopRecording();
        mAudioEncoder = null;
    }

    public synchronized boolean isStarted() {
        return mIsStarted;
    }

    //**********************************************************************
    //**********************************************************************

    /**
     * assign encoder to this class. this is called from encoder.
     *
     * @param encoder instance of MediaVideoEncoder or MediaAudioEncoder
     */
  /*package*/ void addEncoder(final MediaEncoder encoder) {
        if (encoder instanceof MediaVideoEncoder) {
            if (mVideoEncoder != null)
                throw new IllegalArgumentException("Video encoder already added.");
            mVideoEncoder = encoder;
        } else if (encoder instanceof MediaAudioEncoder) {
            if (mAudioEncoder != null)
                throw new IllegalArgumentException("Video encoder already added.");
            mAudioEncoder = encoder;
        } else {
            throw new IllegalArgumentException("unsupported encoder");
        }
        mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
    }

    /**
     * request start recording from encoder
     *
     * @return true when muxer is ready to write
     */
    /*package*/
    synchronized boolean start() {
        Log.d(TAG, "start:");

        mStartedCount++;

        if ((mEncoderCount > 0) && (mStartedCount == mEncoderCount)) {
            mMediaMuxer.start();
            mIsStarted = true;
            notifyAll();
            Log.d(TAG, "MediaMuxer started:");
        }

        return mIsStarted;
    }

    /**
     * request stop recording from encoder when encoder received EOS
     */
	/*package*/
    synchronized void stop() {
        Log.d(TAG, "stop:mStartedCount=" + mStartedCount);

        mStartedCount--;

        if ((mEncoderCount > 0) && (mStartedCount <= 0)) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mIsStarted = false;
            Log.d(TAG, "MediaMuxer stopped:");
        }
    }

    /**
     * assign encoder to muxer
     *
     * @return minus value indicate error
     */
	/*package*/
    synchronized int addTrack(final MediaFormat format) {
        if (mIsStarted) throw new IllegalStateException("muxer already started");
        final int trackIx = mMediaMuxer.addTrack(format);
        Log.d(TAG, "addTrack:trackNum=" + mEncoderCount + ",trackIx=" + trackIx + ",format=" + format);
        return trackIx;
    }

    /**
     * write encoded data to muxer
     */
	/*package*/
    synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf,
                                      final MediaCodec.BufferInfo bufferInfo) {
        if (mStartedCount > 0) {
            mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
        }
    }
}