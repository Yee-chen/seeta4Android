package cn.edu.zstu.facedetection.Detection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaActionSound;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.spec.DESedeKeySpec;

import cn.edu.zstu.R;
import cn.edu.zstu.facedetection.MainActivity;
import cn.edu.zstu.facedetection.cameraPreview.CameraTexturePreview;
import cn.edu.zstu.facedetection.cameraPreview.CameraWrapper;
import cn.edu.zstu.facedetection.util.UIThreadInterface;

/**
 * Created by Chenlei on 2017/04/28.
 */
public class FaceDetector {
    private static final String TAG = "FaceDetector";
    private boolean           DEBUG = MainActivity.DEBUG;

    private int               PREVIEW_WIDTH = CameraWrapper.IMAGE_WIDTH;
    private int               PREVIEW_HEIGHT = CameraWrapper.IMAGE_HEIGHT;
    private Bitmap            mImageBitmap;
    private YuvImage          mImageYuv;
    private Bitmap            mImage;
    private volatile byte[]   mVideoSource;
    private boolean           mIsProcessing = false;
    private long              mNowTime = 0;
    private long              mReferenceTime = 0;
    private long              WAITTIME = 5 * 1000;

    private HandlerThread     mDetectThread;
    private Handler           mDetectHandler;

    private UIThreadInterface mUIti;
    private FaceRect[]        mFaces = null;
    private boolean           ONOFF = false;

    private static FaceDetector mFaceDetector = null;
    private int mLeft, mTop, mWidth, mHeight;

    static {
        System.loadLibrary("ImageProcessor");
    }
    public native boolean predict(Bitmap pTarget, byte[] pSource);

    public void callback(int count, int num, int x, int y, int width, int height){
        if (DEBUG) Log.i(TAG, "== face callback: count: " + count + "  x:" + x + "  y:" + y + " w:" + width + " h:" + height);

        if (mFaces == null)
            mFaces = new FaceRect[count];

        // 因为送去检测的时候对图像进行了1/2的尺寸压缩，这里为了得到准确的坐标位置需要×2.
        mLeft = (int)((((x * 2) / (float)PREVIEW_WIDTH) * CameraTexturePreview.mSurfaceWidth) + 0.5);
        mTop = (int)((((y * 2) / (float)PREVIEW_HEIGHT) * CameraTexturePreview.mSurfaceHeight) + 0.5);
        mWidth = (int)((((width * 2) / (float)PREVIEW_WIDTH) * CameraTexturePreview.mSurfaceWidth) + 0.5);
        mHeight = (int)((((height * 2) / (float)PREVIEW_HEIGHT) * CameraTexturePreview.mSurfaceHeight) + 0.5);
        int len = (int)(mWidth / 8.0 + 0.5);
        mFaces[num] = new FaceRect();
        mFaces[num].bound.left = mLeft - len;
        mFaces[num].bound.top = mTop -len;
        mFaces[num].bound.right = mLeft + mWidth + len;
        mFaces[num].bound.bottom = mTop + mHeight + len;

        if ((num + 1) == count) {
            mUIti.onDrawRect(mFaces);
            mFaces = null;
        }

        saveFace(x, y, x + width, y + height);

        if (DEBUG) Log.e(TAG, "[callback] " + "  x:" + x + "  y:" + y + " w:" + width + " h:" + height);
        if (DEBUG) Log.e(TAG, "[callback] " + "  mLeft:" + mLeft + "  mTop:" + mTop + " mWidth:" + mWidth + " mHeight:" + mHeight);

    }

    public FaceDetector(UIThreadInterface obj) {
        mVideoSource = new byte[PREVIEW_WIDTH * PREVIEW_HEIGHT * 2];
        mImageBitmap = Bitmap.createBitmap(PREVIEW_WIDTH, PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888);
        mUIti = obj;
    }

    public FaceDetector() {
        mVideoSource = new byte[PREVIEW_WIDTH * PREVIEW_HEIGHT * 2];
        mImageBitmap = Bitmap.createBitmap(PREVIEW_WIDTH, PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888);
        mDetectThread = new HandlerThread("Detect");
        mDetectThread.start();
        mDetectHandler = new Handler(mDetectThread.getLooper());
    }

    public void setUIThreadInterface(UIThreadInterface obj) {
        mUIti = obj;
    }

    public static FaceDetector getInstance() {
        if (mFaceDetector == null) {
            synchronized (FaceDetector.class) {
                if (mFaceDetector == null) {
                    mFaceDetector = new FaceDetector();
                }
            }
        }
        return mFaceDetector;
    }

    public void startDetector() {
        ONOFF = true;
        mIsProcessing = false;
    }

    public void stopDetector() {
        ONOFF = false;

        mDetectHandler.removeCallbacks(DoImageProcessing);
        mUIti.onDrawRect(null);
    }

    public boolean getIsStarted() {
        return ONOFF;
    }

    public void onFrameData(byte[] data) {
        if (ONOFF) {
            if (!mIsProcessing) {

                mNowTime = System.currentTimeMillis();
                if (mNowTime > (mReferenceTime + WAITTIME)) {
                    mReferenceTime = mNowTime;

                    if (!mIsProcessing) {
                        synchronized (mVideoSource) {
                            System.arraycopy(data, 0, mVideoSource, 0, data.length);
                        }
//                        new Thread(DoImageProcessing).start();
                        mDetectHandler.post(DoImageProcessing);
                    }
                }
            }
        }
    }

    private Runnable DoImageProcessing = new Runnable() {
        public void run() {
            mIsProcessing = true;
            if (DEBUG) Log.i(TAG, "== invoking predict() ==");
            boolean predictResult = predict(mImageBitmap, mVideoSource);

            if (predictResult) {
                if (DEBUG) Log.i(TAG, "[检测到人脸]");
                WAITTIME = 1 * 1000;
                // Save image
                FaceUtil.saveByteToFile(mVideoSource, PREVIEW_WIDTH, PREVIEW_HEIGHT);
            } else {
                WAITTIME = 3 *1000;
                mUIti.onDrawRect(null);
            }
//            Log.i(TAG, "== after predict() ==");
            mIsProcessing = false;
        }
    };

    private void saveFace(final int x, final int y, final int r, final int b) {
        if (DEBUG) Log.d(TAG, "[saveFace()]");
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (mVideoSource) {
                    mImageYuv = new YuvImage(mVideoSource, ImageFormat.NV21, PREVIEW_WIDTH, PREVIEW_HEIGHT, null);
                }
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                mImageYuv.compressToJpeg(new Rect(0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT), 100, stream);
                Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());

                int left = (x > 0) ? x : 0;
                int top = (y > 0) ? y : 0;
                int creatW = (r < PREVIEW_WIDTH) ? (r - x) : (PREVIEW_WIDTH - x);
                int creatH = (b < PREVIEW_HEIGHT) ? (b - y) : (PREVIEW_HEIGHT - y);

                mImage = Bitmap.createBitmap(bitmap, left, top, creatW, creatH, null, false);
                if (DEBUG) Log.d(TAG, "[saveFace()] x:" + x + "  y:" + y + "\n" +
                                      "[saveFace()] h:" + mImage.getHeight() + "  w:" + mImage.getWidth());
                if (null != mImage)
                    FaceUtil.saveBitmapToFile(mImage);
            }
        }).start();
    }
}
