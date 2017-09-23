package cn.edu.zstu.facedetection.util;

import android.app.Application;
import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.DisplayMetrics;

import java.util.List;

/**
 * Created by Chenlei on 2017/9/23.
 */
public class MyApplication extends Application {

    private static MyApplication sInstance;

    public MyApplication() {
        sInstance = this;
    }

    public static Context getContext() {
        return sInstance;
    }

    /**
     * 获取屏幕宽度和高度，单位为px
     * @return
     */
    public static Point getScreenMetrics(){
        DisplayMetrics dm = sInstance.getResources().getDisplayMetrics();
        int w_screen = dm.widthPixels;
        int h_screen = dm.heightPixels;
        return new Point(w_screen, h_screen);
    }

    /**
     * 获取最佳预览大小
     * @param parameters 相机参数
     * @param screenResolution 屏幕宽高
     * @return
     */
    public static Point getBestCameraResolution(Camera.Parameters parameters, Point screenResolution) {
        float tmp = 0f;
        float mindiff = 100f;
        float x_d_y = (float) screenResolution.x / (float) screenResolution.y;
        Camera.Size best = null;
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        for (Camera.Size s : supportedPreviewSizes) {
            tmp = Math.abs(((float) s.width / (float) s.height) - x_d_y);
            if (tmp < mindiff) {
                mindiff = tmp;
                best = s;
            }
        }
        return new Point(best.width, best.height);
    }
}
