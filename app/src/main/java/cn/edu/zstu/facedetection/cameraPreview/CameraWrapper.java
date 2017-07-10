/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Description:   CameraWrapper.java
 * Author:        Lei Chen <lei.chen@shanjutech.com>
 * Date:          05/19/2017
 *
 * Copyright (C) 2015-2017 The ZSTU SmartHome Project
 */

package cn.edu.zstu.facedetection.cameraPreview;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.List;

import cn.edu.zstu.facedetection.util.UIThreadInterface;

@SuppressLint("NewApi")
public class CameraWrapper {
    private static final String TAG = "CameraWrapper";

    public static final int IMAGE_HEIGHT = 480;
    public static final int IMAGE_WIDTH = 640;
    private static final boolean DEBUG = true;
    private static CameraWrapper mCameraWrapper;

    Camera.PreviewCallback previewCallback;
    private Camera mCamera;
    private Camera.Parameters mCameraParamters;
    private boolean mIsPreviewing = false;
    private float mPreviewRate = -1.0f;
    private CameraPreviewCallback mCameraPreviewCallback;
    private byte[] mImageCallbackBuffer = new byte[CameraWrapper.IMAGE_WIDTH
            * CameraWrapper.IMAGE_HEIGHT * 3 / 2];
    private int openCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    private UIThreadInterface mUIThreadInterface;
    private long mTime;
    private CameraWrapper() {
    }

    public void setUIThreadInterface(UIThreadInterface obj) {
        mUIThreadInterface = obj;
    }

    public static CameraWrapper getInstance() {
        if (mCameraWrapper == null) {
            synchronized (CameraWrapper.class) {
                if (mCameraWrapper == null) {
                    mCameraWrapper = new CameraWrapper();
                }
            }
        }
        return mCameraWrapper;
    }

    public void switchCameraId() {
        if (openCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            openCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            openCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
    }

    public void doOpenCamera(CamOpenOverCallback callback) {
        if (DEBUG) Log.i(TAG, "Camera open....");
        int numCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == openCameraId) {
                mCamera = Camera.open(i);
                break;
            }
        }
        if (mCamera == null) {
            if (DEBUG) Log.d(TAG, "No front-facing camera found; opening default");
            mCamera = Camera.open();    // opens first back-facing camera
        }
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }
        if (DEBUG) Log.i(TAG, "Camera open over....");
        callback.cameraHasOpened();
    }

    public void doStartPreview(SurfaceHolder holder, float previewRate) {
        if (DEBUG) Log.i(TAG, "doStartPreview...");
        if (mIsPreviewing) {
            this.mCamera.stopPreview();
            return;
        }

        try {
            this.mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        initCamera();
    }

    public void doStartPreview(SurfaceTexture surface) {
        if (DEBUG) Log.i(TAG, "doStartPreview()");
        if (mIsPreviewing) {
            this.mCamera.stopPreview();
            return;
        }

        try {
            this.mCamera.setPreviewTexture(surface);
        } catch (IOException e) {
            e.printStackTrace();
        }
        initCamera();
    }

    public void doStopCamera() {
        if (DEBUG) Log.i(TAG, "doStopCamera");
        if (this.mCamera != null) {
            this.mCamera.setPreviewCallback(null);
            this.mCamera.stopPreview();
            this.mIsPreviewing = false;
            this.mPreviewRate = -1f;
            this.mCamera.release();
            this.mCamera = null;
        }
    }

    private void initCamera() {
        if (this.mCamera != null) {
            this.mCameraParamters = this.mCamera.getParameters();
            this.mCameraParamters.setPreviewFormat(ImageFormat.NV21);
            this.mCameraParamters.setFlashMode("off");
            this.mCameraParamters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            this.mCameraParamters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            this.mCameraParamters.setPreviewSize(IMAGE_WIDTH, IMAGE_HEIGHT);
            mCameraPreviewCallback = new CameraPreviewCallback();
            mCamera.addCallbackBuffer(mImageCallbackBuffer);
            mCamera.setPreviewCallbackWithBuffer(mCameraPreviewCallback);
            List<String> focusModes = this.mCameraParamters.getSupportedFocusModes();
            if (focusModes.contains("continuous-video")) {
                this.mCameraParamters
                        .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            this.mCamera.setParameters(this.mCameraParamters);
            this.mCamera.startPreview();

            this.mIsPreviewing = true;
        }
    }

    public void setPreviewCallback(Camera.PreviewCallback callback) {
        previewCallback = callback;
    }

    public interface CamOpenOverCallback {
        void cameraHasOpened();
    }

    class CameraPreviewCallback implements Camera.PreviewCallback {

        private CameraPreviewCallback() {
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (System.currentTimeMillis() - mTime > 500) {
                mTime = System.currentTimeMillis();
                mUIThreadInterface.onFrame(data);
            }
            camera.addCallbackBuffer(data);
        }
    }
}
