# seeta4Android
seeta face detection for Android

### 介绍

本Demo将seeta编译成.so文件供Android平台使用，
做出的修改如下：

    1、对face_detection.cpp文件中的FaceDetection类的构造函数做了适当的修改。

    2、增加了FaceDetection::load_model()函数。

### 用法

具体用法见：jni/ImageProcessor.cpp

首先，图片资源进行灰度化

其次，在第一次使用时调用initDetector()初始化seeta检测器，设置常用参数。

最后，detector.Detect(img_data);

### 结果

检测耗时不到700ms



### 自定义修改

    1、cd app\jni

    2、ndk-build

    若成功，出现如下日志： 

    [armeabi-v7a] Compile++ thumb: ImageProcessor <= ImageProcessor.cpp
    [armeabi-v7a] Compile++ thumb: ImageProcessor <= fust.cpp
    [armeabi-v7a] Compile++ thumb: ImageProcessor <= face_detection.cpp
    [armeabi-v7a] Compile++ thumb: ImageProcessor <= lab_boosted_classifier.cpp
    [armeabi-v7a] Compile++ thumb: ImageProcessor <= mlp.cpp
    [armeabi-v7a] Compile++ thumb: ImageProcessor <= surf_mlp.cpp
    [armeabi-v7a] Compile++ thumb: ImageProcessor <= lab_feature_map.cpp
    [armeabi-v7a] Compile++ thumb: ImageProcessor <= surf_feature_map.cpp
    [armeabi-v7a] Compile++ thumb: ImageProcessor <= lab_boost_model_reader.cpp
    [armeabi-v7a] Compile++ thumb: ImageProcessor <= surf_mlp_model_reader.cpp
    [armeabi-v7a] Compile++ thumb: ImageProcessor <= image_pyramid.cpp
    [armeabi-v7a] Compile++ thumb: ImageProcessor <= nms.cpp
    [armeabi-v7a] SharedLibrary  : libImageProcessor.so
    [armeabi-v7a] Install        : libImageProcessor.so => libs/armeabi-v7a/libImageProcessor.so

    3、将libs/armeabi-v7a/libImageProcessor.so拷贝到jniLibs/armeabi-v7a/

    4、build app
