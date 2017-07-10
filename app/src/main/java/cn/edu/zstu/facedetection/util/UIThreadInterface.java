package cn.edu.zstu.facedetection.util;

import cn.edu.zstu.facedetection.Detection.FaceRect;

/**
 * Created by Chen Lei on 2017/5/16.
 */
public interface UIThreadInterface {
    void onDrawRect(FaceRect[] faces);
    void onFrame(byte[] data);
}
