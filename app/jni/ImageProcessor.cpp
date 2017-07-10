//
// Created by Chenlei on 2017/04/28.
// Modified by Chenlei on 2017/05/02.
//
#include "cn_edu_zstu_facedetection_Detection_FaceDetector.h"
#include <android/log.h>
#include <android/bitmap.h>

#include <string>
#include <opencv2/core.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/imgproc.hpp>
#include "face_detection.h"

using namespace std;
using namespace cv;
using namespace seeta;

#define  LOG_TAG    "ImageProcessor"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  DEBUG 0

/** Global variables */
AndroidBitmapInfo bitmapInfo;
uint32_t* bitmapContent; // Links to Bitmap content
jbyte* source;
Mat dist;

seeta::FaceDetection detector;
seeta::ImageData img_data;
std::vector<seeta::FaceInfo> faces;
cv::Rect face_rect;
int num_face;
int once = 0;
double t; // for measuring time performance

void initDetector() {
    if (once != 1) {
        once = 1;
        detector.load_model("/data/data/cn.edu.zstu.facedetection/files/seeta_frontal.bin");
        detector.SetMinFaceSize(40);
        detector.SetScoreThresh(2.f);
        detector.SetImagePyramidScaleFactor(0.8f);
        detector.SetWindowStep(4, 4);
    }
}

/*
 * Class:     cn_edu_zstu_facedetection_Detection_FaceDetector
 * Method:    predict
 * Signature: (Landroid/graphics/Bitmap;[B)V
 */
JNIEXPORT jboolean JNICALL Java_cn_edu_zstu_facedetection_Detection_FaceDetector_predict
        (JNIEnv * pEnv, jobject clazz, jobject pTarget, jbyteArray pSource) {

    if (AndroidBitmap_getInfo(pEnv, pTarget, &bitmapInfo) < 0) abort();
    if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) abort();

    // Access source array data... OK
    source = (jbyte*)pEnv->GetPrimitiveArrayCritical(pSource, 0);
    if (source == NULL) {
        if (DEBUG) {
            LOGE("== the source is NULL");
        }
        abort();
    }

    // cv::Mat for YUV420sp source and output GRAY
    Mat srcGray(bitmapInfo.height, bitmapInfo.width, CV_8UC1, (unsigned char *)source);
    // 压缩待检测图像的尺寸可以加快检测速度。这里尺寸变为原来的一半
    cv::resize(srcGray, dist, cv::Size(bitmapInfo.width / 2, bitmapInfo.height / 2), (0, 0), (0, 0), cv::INTER_LINEAR);

    if (DEBUG) {
        LOGI("== Starting native image processing...");
    }
    // Detect face
    t = (double)getTickCount();

    initDetector();
    img_data.data = dist.data;
    img_data.width = dist.cols;
    img_data.height = dist.rows;
    img_data.num_channels = 1;
    faces = detector.Detect(img_data);

    t = 1000*((double)getTickCount() - t)/getTickFrequency();
    if (DEBUG) {
        LOGI("== Face detect time = %lf ms.", t);
        LOGI("== Successfully finished native image processing...");
    }

    // Release Java byte buffer
    pEnv-> ReleasePrimitiveArrayCritical(pSource,source,0);

    //Callback Java method
    jclass cls = pEnv->GetObjectClass(clazz);// get Java class instance
    jmethodID callbackID = pEnv->GetMethodID(cls , "callback" , "(IIIIII)V") ;// get callbackID

    if(callbackID == NULL) {
        LOGE("== get callbackID is failed");
    }

    num_face = static_cast<int>(faces.size());
    for(int i = 0; i < num_face; i++) {
        face_rect.x = faces[i].bbox.x;
        face_rect.y = faces[i].bbox.y;
        face_rect.width = faces[i].bbox.width;
        face_rect.height = faces[i].bbox.height;
        pEnv->CallVoidMethod(clazz , callbackID , num_face, i, face_rect.x, face_rect.y, face_rect.width, face_rect.height);
    }

    // The predict result
    if (num_face > 0) {
        if (DEBUG) {
            LOGI("检测到人脸...");
        }
        return true;
    } else {
        if (DEBUG){
            LOGI("未检测到目标...");
        }
        return false;
    }
}