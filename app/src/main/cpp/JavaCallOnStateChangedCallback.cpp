//
// Created by xpl on 2021/6/1.
//

#include "JavaCallOnStateChangedCallback.h"


JavaCallOnStateChangedCallback::JavaCallOnStateChangedCallback(JavaVM *_javaVM, JNIEnv *_env, jobject &_jobj) {
    jobj = env->NewGlobalRef(_jobj);
    jclass jclazz = env->GetObjectClass(jobj);

    jmid_disconnected = env->GetMethodID(jclazz, "onConnectState", "(Z)V");

}

JavaCallOnStateChangedCallback::~JavaCallOnStateChangedCallback() {
    env->DeleteGlobalRef(jobj);
    jobj = 0;
}

void JavaCallOnStateChangedCallback::onConnectState(jboolean isConnect, int thread) {
    if (thread == THREAD_CHILD) {
        JNIEnv *jniEnv;
        if (javaVM->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmid_disconnected, isConnect);
        javaVM->DetachCurrentThread();
    } else {
        env->CallVoidMethod(jobj, jmid_disconnected);
    }
}