//
// Created by xpl on 2021/6/1.
//

#ifndef RTMP_CAM_JAVACALLONSTATECHANGEDCALLBACK_H
#define RTMP_CAM_JAVACALLONSTATECHANGEDCALLBACK_H

#include <jni.h>
//标记线程 因为子线程需要attach
#define THREAD_MAIN 1
#define THREAD_CHILD 2

class JavaCallOnStateChangedCallback {
public:
    JavaCallOnStateChangedCallback(JavaVM *_javaVM, JNIEnv *_env, jobject &_jobj);

    ~JavaCallOnStateChangedCallback();

    void onConnectState(jboolean isConnect, int thread = THREAD_CHILD);

public:
    JavaVM *javaVM;
    JNIEnv *env;
    jobject jobj;
    jmethodID jmid_disconnected;


};

#endif //RTMP_CAM_JAVACALLONSTATECHANGEDCALLBACK_H
