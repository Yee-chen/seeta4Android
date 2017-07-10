# Build ImageProcessing

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
 
OPENCV_LIB_TYPE:=STATIC
OPENCV_INSTALL_MODULES:=on

include C:/Users/cdr/Downloads/OpenCV-3.0.0-rc1-android-sdk-1/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := ImageProcessor
LOCAL_SRC_FILES += ImageProcessor.cpp \
                   FaceDetection/src/fust.cpp \
                   FaceDetection/src/face_detection.cpp \
                   FaceDetection/src/classifier/lab_boosted_classifier.cpp \
                   FaceDetection/src/classifier/mlp.cpp \
                   FaceDetection/src/classifier/surf_mlp.cpp \
                   FaceDetection/src/feat/lab_feature_map.cpp \
                   FaceDetection/src/feat/surf_feature_map.cpp \
                   FaceDetection/src/io/lab_boost_model_reader.cpp \
                   FaceDetection/src/io/surf_mlp_model_reader.cpp \
                   FaceDetection/src/util/image_pyramid.cpp \
                   FaceDetection/src/util/nms.cpp
LOCAL_C_INCLUDES += FaceDetection/include \
                    FaceDetection/include/classifier \
                    FaceDetection/include/feat \
                    FaceDetection/include/io \
                    FaceDetection/include/util
LOCAL_CFLAGS += -std=c++11
LOCAL_LDLIBS +=  -llog -ldl -ljnigraphics

include $(BUILD_SHARED_LIBRARY)