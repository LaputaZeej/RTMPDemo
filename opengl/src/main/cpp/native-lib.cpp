#include <jni.h>
#include <string>
#include <android/log.h>
#include "FaceTrack.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"_opengl_c",__VA_ARGS__)

extern "C"
JNIEXPORT jlong JNICALL
Java_com_bugull_rtmp_opengl_face_FaceTracker_nativeCreateObject(JNIEnv *env, jobject clazz,
                                                          jstring facemodel_,
                                                          jstring landmarkermodel_) {
    const char *facemodel = env->GetStringUTFChars(facemodel_, 0);
    const char *landmarkermodel = env->GetStringUTFChars(landmarkermodel_, 0);
    LOGI("Java_com_bugull_rtmp_opengl_face_FaceTracker_nativeCreateObject");
    FaceTrack *faceTrack = new FaceTrack(facemodel, landmarkermodel);

    env->ReleaseStringUTFChars(facemodel_, facemodel);
    env->ReleaseStringUTFChars(landmarkermodel_, landmarkermodel);
    return (jlong) faceTrack;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bugull_rtmp_opengl_face_FaceTracker_nativeDestroyObject(JNIEnv *env, jobject clazz, jlong thiz) {
    LOGI("Java_com_bugull_rtmp_opengl_face_FaceTracker_nativeDestroyObject");
    if (thiz != 0) {
        FaceTrack *tracker = reinterpret_cast<FaceTrack *>(thiz);
        tracker->stop();
        delete tracker;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bugull_rtmp_opengl_face_FaceTracker_nativeStart(JNIEnv *env, jobject clazz, jlong thiz) {
    LOGI("Java_com_bugull_rtmp_opengl_face_FaceTracker_nativeStart");
    if (thiz != 0) {
        FaceTrack *tracker = reinterpret_cast<FaceTrack *>(thiz);
        tracker->run();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bugull_rtmp_opengl_face_FaceTracker_nativeStop(JNIEnv *env, jobject clazz, jlong thiz) {
    LOGI("Java_com_bugull_rtmp_opengl_face_FaceTracker_nativeStop");
    if (thiz != 0) {
        FaceTrack *tracker = reinterpret_cast<FaceTrack *>(thiz);
        tracker->stop();
    }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_bugull_rtmp_opengl_face_FaceTracker_nativeDetect(JNIEnv *env, jobject clazz, jlong thiz,
                                                    jbyteArray inputImage_,
                                                    jint width, jint height,
                                                    jint rotationDegrees) {
    LOGI("Java_com_bugull_rtmp_opengl_face_FaceTracker_nativeDetect");
    if (thiz == 0) {
        return 0;
    }
    LOGI("face start  ...");
    FaceTrack *tracker = reinterpret_cast<FaceTrack *>(thiz);
    jbyte *inputImage = env->GetByteArrayElements(inputImage_, 0);

    //I420
    Mat src(height * 3 / 2, width, CV_8UC1, inputImage);

    // 转为RGBA
    cvtColor(src, src, CV_YUV2RGBA_I420);
    //旋转
    if (rotationDegrees == 90) {
        rotate(src, src, ROTATE_90_CLOCKWISE);
    } else if (rotationDegrees == 270) {
        rotate(src, src, ROTATE_90_COUNTERCLOCKWISE);
    }
    //镜像问题，可以使用此方法进行垂直翻转
//    flip(src,src,1);
    Mat gray;
    cvtColor(src, gray, CV_RGBA2GRAY);
    equalizeHist(gray, gray);

    cv::Rect face;
    std::vector<SeetaPointF> points;
    tracker->process(gray, face, points);


    int w = src.cols;
    int h = src.rows;
    gray.release();
    src.release();
    env->ReleaseByteArrayElements(inputImage_, inputImage, 0);



    if (!face.empty() && !points.empty()){
        LOGI("face ok ..");
        jclass cls = env->FindClass("com/bugull/rtmp/opengl/face/Face");
        jmethodID construct = env->GetMethodID(cls, "<init>", "(IIIIIIFFFF)V");
        SeetaPointF left = points[0];
        SeetaPointF right = points[1];
        jobject obj = env->NewObject(cls, construct, face.width, face.height,w,h, face.x, face.y,(float)left.x,(float)left.y,(float)right.x,(float)right.y);
        return obj;
    }
    LOGI("face not ok ..");
    return 0;

}