package cn.edu.zstu.facedetection.Detection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;

import cn.edu.zstu.facedetection.MainActivity;
import cn.edu.zstu.facedetection.cameraPreview.CameraTexturePreview;
import cn.edu.zstu.facedetection.cameraPreview.CameraWrapper;

/**
 * Created by Chenlei on 2017/04/28.
 */
public class FaceDetector {
    private static final String TAG = "FaceDetector";
    private boolean DEBUG = MainActivity.DEBUG;

    private SurfaceView mFaceSurface;
    private Canvas mCanvas;
    private Bitmap mImageBitmap;
    private YuvImage mImageYuv;
    private Bitmap mImage;
    private volatile byte[] mVideoSource = null;
    private boolean mIsProcessing = false;
    private HandlerThread mDetectThread;
    private Handler mDetectHandler;
    private Rect[] mFaces = null;
    private boolean ONOFF = false;

    private static FaceDetector mFaceDetector = null;
    private int mLeft, mTop, mWidth, mHeight;

    static {
        System.loadLibrary("ImageProcessor");
    }

    public native boolean predict(Bitmap pTarget, byte[] pSource);

    public void callback(int count, int num, int x, int y, int width, int height) {
        if (DEBUG)
            Log.i(TAG, "== face callback: count: " + count + "  x:" + x + "  y:" + y + " w:" + width + " h:" + height);

        if (mFaces == null)
            mFaces = new Rect[count];

        // 因为送去检测的时候对图像进行了1/2的尺寸压缩，这里为了得到准确的坐标位置需要×2.
        mLeft = (x * 2);
        mTop = (y * 2);
        mWidth = (width * 2);
        mHeight = (height * 2);
        int len = (int) (mWidth / 8.0 + 0.5);
        mFaces[num] = new Rect();
        mFaces[num].left = mLeft - len;
        mFaces[num].top = mTop - len;
        mFaces[num].right = mLeft + mWidth + len;
        mFaces[num].bottom = mTop + mHeight + len;

        if ((num + 1) == count) {
            drawRect(mFaces);
            mFaces = null;
        }

        saveFace(x, y, x + width, y + height);

        if (DEBUG)
            Log.e(TAG, "[callback] " + "  x:" + x + "  y:" + y + " w:" + width + " h:" + height);
        if (DEBUG)
            Log.e(TAG, "[callback] " + "  mLeft:" + mLeft + "  mTop:" + mTop + " mWidth:" + mWidth + " mHeight:" + mHeight);

    }

    public FaceDetector(SurfaceView obj) {
        mFaceSurface = obj;
    }

    public FaceDetector() {
        mDetectThread = new HandlerThread("Detect");
        mDetectThread.start();
        mDetectHandler = new Handler(mDetectThread.getLooper());
    }

    public void setUIThreadInterface(SurfaceView obj) {
        mFaceSurface = obj;
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
        mImageBitmap = Bitmap.createBitmap(CameraWrapper.IMAGE_WIDTH, CameraWrapper.IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        ONOFF = true;
        mIsProcessing = false;
    }

    public void stopDetector() {
        ONOFF = false;
        mDetectHandler.removeCallbacks(DoImageProcessing);
        drawRect(null);
    }

    public boolean getIsStarted() {
        return ONOFF;
    }

    public void onFrameData(byte[] data) {
        if (ONOFF) {
            if (!mIsProcessing) {
                if (mVideoSource == null) {
                    mVideoSource = data.clone();
                    mDetectHandler.post(DoImageProcessing);
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
                // Save image
                FaceUtil.saveByteToFile(mVideoSource, CameraWrapper.IMAGE_WIDTH, CameraWrapper.IMAGE_HEIGHT);
            } else {
                drawRect(null);
            }
            mIsProcessing = false;
            mVideoSource = null;
        }
    };

    private void saveFace(final int x, final int y, final int r, final int b) {
        if (DEBUG) Log.d(TAG, "[saveFace()]");
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (mVideoSource) {
                    mImageYuv = new YuvImage(mVideoSource, ImageFormat.NV21, CameraWrapper.IMAGE_WIDTH, CameraWrapper.IMAGE_HEIGHT, null);
                }
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                mImageYuv.compressToJpeg(new Rect(0, 0, CameraWrapper.IMAGE_WIDTH, CameraWrapper.IMAGE_HEIGHT), 100, stream);
                Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());

                int left = (x > 0) ? x : 0;
                int top = (y > 0) ? y : 0;
                int creatW = (r < CameraWrapper.IMAGE_WIDTH) ? (r - x) : (CameraWrapper.IMAGE_HEIGHT - x - 1);
                int creatH = (b < CameraWrapper.IMAGE_WIDTH) ? (b - y) : (CameraWrapper.IMAGE_HEIGHT - y - 1);

                mImage = Bitmap.createBitmap(bitmap, left, top, creatW, creatH, null, false);
                if (DEBUG) Log.d(TAG, "[saveFace()] x:" + x + "  y:" + y + "\n" +
                        "[saveFace()] h:" + mImage.getHeight() + "  w:" + mImage.getWidth());
                if (null != mImage)
                    FaceUtil.saveBitmapToFile(mImage);
            }
        }).start();
    }

    public void drawRect(Rect[] rects) {
        mCanvas = mFaceSurface.getHolder().lockCanvas();
        if (null == mCanvas) {
            return;
        }
        mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        if (rects == null) {
            mFaceSurface.getHolder().unlockCanvasAndPost(mCanvas);
            return;
        }
        mCanvas.setMatrix(CameraTexturePreview.mScaleMatrix);
        for (Rect rect:rects) {
            FaceUtil.drawRect(mCanvas, rect);
        }
        mFaceSurface.getHolder().unlockCanvasAndPost(mCanvas);
    }
}
