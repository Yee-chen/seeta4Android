package cn.edu.zstu.facedetection;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import cn.edu.zstu.R;
import cn.edu.zstu.facedetection.Detection.AssetUtil;
import cn.edu.zstu.facedetection.Detection.FaceDetector;
import cn.edu.zstu.facedetection.Detection.FaceRect;
import cn.edu.zstu.facedetection.Detection.FaceUtil;
import cn.edu.zstu.facedetection.cameraPreview.CameraTexturePreview;
import cn.edu.zstu.facedetection.cameraPreview.CameraWrapper;
import cn.edu.zstu.facedetection.util.UIThreadInterface;

/**
 * Created by Chenlei on 2017/04/28.
 */
public class MainActivity extends Activity implements
        CompoundButton.OnCheckedChangeListener, UIThreadInterface {
    static final String    TAG = "MainActivity";
    public static final boolean DEBUG = true;

    private SurfaceView    mFaceSurface;
    private Canvas         mCanvas;
    private Toast          mToast;
    private FaceDetector   mFacetector;
    private Matrix         mScaleMatrix = new Matrix();
    private int            mSurfaceHeight;
    private int            mSurfaceWidth;
    private Switch         mSwitch;
    private RelativeLayout mSwitchLayout;
    private boolean        mIsDisplay;

    private HandlerThread  mDataCopyThread;
    private Runnable       mDataCopyRunnable;
    private Handler        mDataCopyHandler;

    @Override
    public void onDrawRect(FaceRect[] faces) {
        if (DEBUG) Log.d(TAG, "[onDrawRect()]");
        mCanvas = mFaceSurface.getHolder().lockCanvas();
        if (null == mCanvas) return;
        mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        if (faces == null) {
            mFaceSurface.getHolder().unlockCanvasAndPost(mCanvas);
            return;
        }
        for (FaceRect face : faces) {
//            mCanvas.setMatrix(CameraTexturePreview.mScaleMatrix);
            FaceUtil.drawRect(mCanvas, face);
        }
        mFaceSurface.getHolder().unlockCanvasAndPost(mCanvas);
    }

    @Override
    public void onFrame(final byte[] data) {
        if (mDataCopyRunnable == null) {
            mDataCopyRunnable = new Runnable() {
                @Override
                public void run() {
                    mFacetector.onFrameData(data);
                }
            };
        }
        mDataCopyHandler.post(mDataCopyRunnable);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        if (DEBUG) Log.d(TAG, "[onCreate()]");

        init();
        initRSwitch();

        AssetUtil.copyAssetToCache(this);
        CameraWrapper.getInstance().setUIThreadInterface(this);
//        mFacetector = new FaceDetector(this);
        mFacetector = FaceDetector.getInstance();
        mFacetector.setUIThreadInterface(this);
        mDataCopyThread = new HandlerThread("DataCopy");
        mDataCopyThread.start();
        mDataCopyHandler = new Handler(mDataCopyThread.getLooper());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG) Log.d(TAG, "[onPause()]");
        CameraTexturePreview.closeCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG) Log.d(TAG, "[onResume()]");

        CameraTexturePreview.openCamera();
    }

    private void init() {
        mFaceSurface = (SurfaceView) findViewById(R.id.sfv_face);
        mFaceSurface.setZOrderOnTop(true);
        mFaceSurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        mToast = Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT);
    }

    public void initRSwitch() {
        mSwitchLayout = (RelativeLayout) findViewById(R.id.det_layout);
        mSwitch = (Switch) findViewById(R.id.swt);
        mSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.swt:
                if (!isChecked) {
                    mFacetector.stopDetector();
                } else {
                    mFacetector.startDetector();
                }
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mIsDisplay) {
                mSwitchLayout.setVisibility(View.INVISIBLE);
                mIsDisplay = false;
            } else {
                mSwitchLayout.setVisibility(View.VISIBLE);
                mIsDisplay = true;
            }
        }
        return super.onTouchEvent(event);
    }

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
