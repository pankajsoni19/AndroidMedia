package com.pankajsoni19.media.camera;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Created by Pankaj Soni <pankajsoni19@live.com> on 01/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public final class CameraHandler extends Handler {

    private static final String TAG = "CameraHandler";

    private static final int MSG_PREVIEW_START = 1;
    private static final int MSG_PREVIEW_STOP = 2;
    private static final int MSG_TOGGLE_FLASH = 3;
    private static final int MSG_UPDATE_FLASH = 4;
    private static final int MSG_TOGGLE_CAMERA = 5;
    private static final int MSG_UPDATE_CAMERA = 6;
    private static final int MSG_RECORDING_START = 7;
    private static final int MSG_RECORDING_STOP = 8;
    private static final int MSG_CAM_TORCH_OFF = 9;

    private static final long DELAY_START_PREVIEW = 200; //android min animation duration

    private CameraThread mThread;

    CameraHandler(final CameraThread thread) {
        mThread = thread;
    }

    void startPreview(final int width, final int height) {
        removeMessages(MSG_PREVIEW_START);
        sendMessageDelayed(obtainMessage(MSG_PREVIEW_START, width, height), DELAY_START_PREVIEW);
    }

    void toggleFlash() {
        sendEmptyMessage(MSG_TOGGLE_FLASH);
    }

    void updateFlashStatus() {
        sendEmptyMessage(MSG_UPDATE_FLASH);
    }

    void toggleCamera() {
        sendEmptyMessage(MSG_TOGGLE_CAMERA);
    }

    void updateCameraIcon() {
        sendEmptyMessage(MSG_UPDATE_CAMERA);
    }

    void onRecordingStart() {
        sendEmptyMessage(MSG_RECORDING_START);
    }

    void onRecordingStop() {
        sendEmptyMessage(MSG_RECORDING_STOP);
    }

    void forceTorchOff() {
        sendEmptyMessage(MSG_CAM_TORCH_OFF);
    }
    /**
     * request to stop camera preview
     *
     * @param needWait need to wait for stopping camera preview
     */
    void stopPreview(final boolean needWait) {
        synchronized (this) {
            sendEmptyMessage(MSG_PREVIEW_STOP);
            if (needWait && mThread.mIsRunning) {
                try {
                    Log.d(TAG, "wait for terminating of camera thread");
                    wait();
                } catch (final InterruptedException ignore) {
                }
            }
        }
    }

    /**
     * message handler for camera thread
     */
    @Override
    public void handleMessage(final Message msg) {
        switch (msg.what) {
            case MSG_PREVIEW_START:
                mThread.startPreview(msg.arg1, msg.arg2);
                mThread.updateCameraIcon();
                mThread.updateFlashStatus();
                break;
            case MSG_PREVIEW_STOP:
                mThread.stopPreview();
                synchronized (this) {
                    notifyAll();
                }
                Looper looper = Looper.myLooper();
                if (looper != null) looper.quit();
                mThread = null;
                break;
            case MSG_TOGGLE_FLASH:
                mThread.toggleFlash();
                break;
            case MSG_UPDATE_FLASH:
                mThread.updateFlashStatus();
                break;
            case MSG_TOGGLE_CAMERA:
                mThread.toggleCamera();
                break;
            case MSG_UPDATE_CAMERA:
                mThread.updateCameraIcon();
                break;
            case MSG_RECORDING_START:
                mThread.onRecordingStart();
                break;
            case MSG_RECORDING_STOP:
                mThread.onRecordingStop();
                break;
            case MSG_CAM_TORCH_OFF:
                mThread.torchOff();
                break;
            default:
                throw new RuntimeException("unknown message:what=" + msg.what);
        }
    }
}
