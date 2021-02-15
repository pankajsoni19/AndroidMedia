package com.pankajsoni19.demo;

/**
 * Copied from
 * https://android.googlesource.com/platform/cts/+/jb-mr2-release/tests/tests/media/src/android/media/cts/EncoderTest.java
 */

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import com.google.common.truth.Truth.assertThat;

public class EncoderTest {
    private static final String TAG = "EncoderTest";
    private static final boolean VERBOSE = false;
    private static final int kNumInputBytes = 256 * 1024;
    private static final long kTimeoutUs = 10000;

    public void testAACEncoders() {

        LinkedList<MediaFormat> formats = new LinkedList<>();

        final int kAACProfiles[] = {
                MediaCodecInfo.CodecProfileLevel.AACObjectLC,
                //MediaCodecInfo.CodecProfileLevel.AACObjectHE,
                MediaCodecInfo.CodecProfileLevel.AACObjectELD
        };

        final int kSampleRates[] = {8000, 11025, 22050, 44100, 48000};
        final int kBitRates[] = {64000};

        for (int k = 0; k < kAACProfiles.length; ++k) {
            for (int i = 0; i < kSampleRates.length; ++i) {
                for (int j = 0; j < kBitRates.length; ++j) {
                    MediaFormat format = MediaFormat.createAudioFormat("audio/mp4a-latm", 8000, 1);
//                    format.setString(MediaFormat.KEY_MIME, );
                    format.setInteger(MediaFormat.KEY_AAC_PROFILE, kAACProfiles[k]);
                    //format.setInteger(MediaFormat.KEY_SAMPLE_RATE, kSampleRates[i]);
                    format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
                    format.setInteger(MediaFormat.KEY_BIT_RATE, kBitRates[j]);
                    formats.push(format);
                }
            }
        }

        testEncoderWithFormats("audio/mp4a-latm", formats);
    }

    private void testEncoderWithFormats(String mime, List<MediaFormat> formats) {

        List<String> componentNames = getEncoderNamesForType(mime);

        for (String componentName : componentNames) {
            Log.d(TAG, "testing component '" + componentName + "'");
            for (MediaFormat format : formats) {
                Log.d(TAG, "  testing format '" + format + "'");
                try {
                    assertThat(mime, format.getString(MediaFormat.KEY_MIME));
                    testEncoder(componentName, format);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private List<String> getEncoderNamesForType(String mime) {
        LinkedList<String> names = new LinkedList<>();
        int n = MediaCodecList.getCodecCount();
        for (int i = 0; i < n; ++i) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            if (!info.isEncoder()) {
                continue;
            }
            if (!info.getName().startsWith("OMX.")) {
                // Unfortunately for legacy reasons, "AACEncoder", a
                // non OMX component had to be in this list for the video
                // editor code to work... but it cannot actually be instantiated
                // using MediaCodec.
                Log.d(TAG, "skipping '" + info.getName() + "'.");
                continue;
            }

            String[] supportedTypes = info.getSupportedTypes();

            for (int j = 0; j < supportedTypes.length; ++j) {
                if (supportedTypes[j].equalsIgnoreCase(mime)) {
                    names.push(info.getName());
                    break;
                }
            }
        }

        return names;
    }

    private int queueInputBuffer(MediaCodec codec, ByteBuffer[] inputBuffers, int index) {
        ByteBuffer buffer = inputBuffers[index];
        buffer.clear();
        int size = buffer.limit();
        byte[] zeroes = new byte[size];
        buffer.put(zeroes);
        codec.queueInputBuffer(index, 0 /* offset */, size, 0 /* timeUs */, 0);
        return size;
    }

    private void dequeueOutputBuffer(
            MediaCodec codec, ByteBuffer[] outputBuffers,
            int index, MediaCodec.BufferInfo info) {
        codec.releaseOutputBuffer(index, false /* render */);
    }

    private void testEncoder(String componentName, MediaFormat format) throws IOException {
        MediaCodec codec = MediaCodec.createByCodecName(componentName);
        try {
            codec.configure(
                    format,
                    null /* surface */,
                    null /* crypto */,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IllegalStateException e) {
            Log.e(TAG, "codec '" + componentName + "' failed configuration.");
        }

        codec.start();
        ByteBuffer[] codecInputBuffers = codec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

        int numBytesSubmitted = 0;
        boolean doneSubmittingInput = false;
        int numBytesDequeued = 0;
        while (true) {
            int index;
            if (!doneSubmittingInput) {
                index = codec.dequeueInputBuffer(kTimeoutUs /* timeoutUs */);
                if (index != MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (numBytesSubmitted >= kNumInputBytes) {
                        codec.queueInputBuffer(
                                index,
                                0 /* offset */,
                                0 /* size */,
                                0 /* timeUs */,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        if (VERBOSE) {
                            Log.d(TAG, "queued input EOS.");
                        }
                        doneSubmittingInput = true;
                    } else {
                        int size = queueInputBuffer(
                                codec, codecInputBuffers, index);
                        numBytesSubmitted += size;
                        if (VERBOSE) {
                            Log.d(TAG, "queued " + size + " bytes of input data.");
                        }
                    }
                }
            }

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            index = codec.dequeueOutputBuffer(info, kTimeoutUs /* timeoutUs */);

            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
            } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = codec.getOutputBuffers();
            } else {
                dequeueOutputBuffer(codec, codecOutputBuffers, index, info);
                numBytesDequeued += info.size;
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE) {
                        Log.d(TAG, "dequeued output EOS.");
                    }
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "dequeued " + info.size + " bytes of output data.");
                }
            }
        }

        if (VERBOSE) {
            Log.d(TAG, "queued a total of " + numBytesSubmitted + "bytes, "
                    + "dequeued " + numBytesDequeued + " bytes.");
        }

        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        int inBitrate = sampleRate * channelCount * 16;  // bit/sec
        int outBitrate = format.getInteger(MediaFormat.KEY_BIT_RATE);

        float desiredRatio = (float) outBitrate / (float) inBitrate;
        float actualRatio = (float) numBytesDequeued / (float) numBytesSubmitted;

//        if (actualRatio < 0.9 * desiredRatio || actualRatio > 1.1 * desiredRatio) {
//            Log.w(TAG, "desiredRatio = " + desiredRatio + ", actualRatio = " + actualRatio);
//        }

        Log.w(TAG, "desiredRatio = " + desiredRatio + ", actualRatio = " + actualRatio);
        codec.release();
    }
}
