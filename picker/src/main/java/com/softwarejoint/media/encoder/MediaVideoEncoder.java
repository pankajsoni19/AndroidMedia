package com.softwarejoint.media.encoder;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: MediaVideoEncoder.java
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
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.opengl.EGLContext;
import android.util.Log;
import android.view.Surface;

import com.softwarejoint.media.glutils.GLDrawer2D;
import com.softwarejoint.media.glutils.RenderHandler;

import java.io.IOException;

import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;

@SuppressWarnings("WeakerAccess")
public class MediaVideoEncoder extends MediaEncoder {

    private static final String TAG = "MediaVideoEncoder";

    private static final String MIME_TYPE = "video/avc";

    // parameters for recording
    private static final int DEFAULT_FRAME_RATE = 25;
    private static final int DEFAULT_IFRAME_INTERVAL = 10;

    private static final float BPP = 0.25f; //bytes per pixel

    private final int mWidth;
    private final int mHeight;
    private RenderHandler mRenderHandler;
    private Surface mSurface;

    public MediaVideoEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener,
                             final int width, final int height, GLDrawer2D drawer) {
        super(muxer, listener);

        Log.d(TAG, "MediaVideoEncoder: ");

        mWidth = width;
        mHeight = height;
        mRenderHandler = RenderHandler.createHandler(TAG, drawer);
    }

    /**
     * select the first codec that match a specific MIME type
     *
     * @return null if no codec matched
     */
    protected static MediaCodecInfo selectVideoCodec(final String mimeType) {
        Log.d(TAG, "selectVideoCodec:");

        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();

        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {  // skipp decoder
                continue;
            }

            // select first codec that match a specific MIME type and color format
            final String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    Log.d(TAG, "codec:" + codecInfo.getName() + ",MIME=" + type);

                    final int format = setVideoFormat(codecInfo, mimeType);

                    if (format > 0) {
                        return codecInfo;
                    }
                }
            }
        }

        return null;
    }

    /**
     * select color format available on specific codec and we can use.
     *
     * @return 0 if no colorFormat is matched
     */
    protected static int setVideoFormat(final MediaCodecInfo codecInfo, final String mimeType) {
        Log.d(TAG, "setVideoFormat: ");

        final MediaCodecInfo.CodecCapabilities caps;

        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            caps = codecInfo.getCapabilitiesForType(mimeType);
        } finally {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }

        for (int i = 0; i < caps.colorFormats.length; i++) {
            final int colorFormat = caps.colorFormats[i];
            if (colorFormat == COLOR_FormatSurface) {
                return colorFormat;
            }
        }

        Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return 0;
    }

    public void setMatrix(final float[] mvp_matrix) {
        mRenderHandler.setMatrix(mvp_matrix);
    }

    public boolean frameAvailableSoon(final float[] stMatrix) {
        if (super.frameAvailableSoon()) {
            mRenderHandler.draw(stMatrix);
            return true;
        }

        return false;
    }

    @Override
    protected void prepare() throws IOException {
        Log.d(TAG, "prepare: ");

        mTrackIndex = -1;
        mMuxerStarted = mIsEOS = false;

        final MediaCodecInfo videoCodecInfo = selectVideoCodec(MIME_TYPE);

        if (videoCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }

        Log.d(TAG, "selected codec: " + videoCodecInfo.getName());

        final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                COLOR_FormatSurface);  // API >= 18
        format.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate());
        format.setInteger(MediaFormat.KEY_FRAME_RATE, DEFAULT_FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, DEFAULT_IFRAME_INTERVAL);

        Log.d(TAG, "format: " + format);

        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // get Surface for encoder input
        // this method only can call between #configure and #start
        mSurface = mMediaCodec.createInputSurface();  // API >= 18
        mMediaCodec.start();

        Log.d(TAG, "prepare finishing");

        if (mListener != null) {
            try {
                mListener.onPrepared(this);
            } catch (final Exception e) {
                Log.e(TAG, "prepare:", e);
            }
        }
    }

    public void setEglContext(final EGLContext shared_context, final int tex_id) {
        mRenderHandler.setEglContext(shared_context, tex_id, mSurface);
    }

    @Override
    protected void release() {
        Log.d(TAG, "release:");

        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }

        if (mRenderHandler != null) {
            mRenderHandler.release();
            mRenderHandler = null;
        }
        super.release();
    }

    private int calcBitRate() {
        final int bitrate = (int) (BPP * DEFAULT_FRAME_RATE * mWidth * mHeight);
        final float mbps = bitrate / 1024f / 1024f;
        Log.d(TAG, "bitrate: " + mbps + " [MBPS] width: " + mWidth + " height: " + mHeight);
        return bitrate;
    }

    @Override
    protected void signalEndOfInputStream() {
        Log.d(TAG, "sending EOS to encoder");
        mMediaCodec.signalEndOfInputStream();  // API >= 18
        mIsEOS = true;
    }
}