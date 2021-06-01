#include <jni.h>
#include <string>
#include <x264.h>
#include "librtmp/rtmp.h"
#include "JavaCallHelper.h"
#include "VideoChannel.h"
#include "AudioChannel.h"
#include <android/log.h>
#include <pthread.h>
#include <unistd.h>

JavaVM *javaVm = 0;
pthread_t pid;
pthread_t connectStatePid;
char *path = 0;
RTMP *rtmp = 0;
JavaCallHelper *javaCallHelper = 0;
VideoChannel *videoChannel = 0;
AudioChannel *audioChannel = 0;
uint64_t startTime = 0;
pthread_mutex_t mutex;
int flag = 1;

uint64_t checkTime = 0;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVm = vm;
    return JNI_VERSION_1_4;
}

void *checkConnectState(void *args);

void onPacketCallback(RTMPPacket *packet) {
    pthread_mutex_lock(&mutex);
    if (rtmp) {
//        int connect = checkConnectState();
//        if (connect) {
        packet->m_nInfoField2 = rtmp->m_stream_id;
        packet->m_nTimeStamp = RTMP_GetTime() - startTime;
        // 1 异步
        int ret = RTMP_SendPacket(rtmp, packet, 1);
        if (!ret) {
            LOGI("RTMP_SendPacket FAIL %d", ret);
        } else {
            LOGI("RTMP_SendPacket SUCCESS %d", ret);
        }
//        } else {
//            LOGI("onPacketCallback connect %d", connect);
//        }
    }
    pthread_mutex_unlock(&mutex);
    RTMPPacket_Free(packet);
    delete (packet);
}

void doDisconnect() {
    LOGI("disconnect");
    pthread_mutex_lock(&mutex);
    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = 0;
    }
    pthread_mutex_unlock(&mutex);
}

void *connect(void *args) {
    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = 0;
    }
    rtmp = RTMP_Alloc();
    RTMP_Init(rtmp);
    int ret;
    do {
        ret = RTMP_SetupURL(rtmp, path);
        if (!ret) {
            LOGI("RTMP_SetupURL fail %d", ret);
            break;
        }
        //开启输出模式，播放拉流时可以不开
        RTMP_EnableWrite(rtmp);
        do {
            LOGI("connect RTMP_Connect %d", ret);
            ret = RTMP_Connect(rtmp, 0);
            /* if (!ret) {
                 LOGI("RTMP_Connect fail %d", ret);
                 break;
             }*/
            if (ret)break;
            sleep(3);
        } while (1);
        do {
            LOGI("connect RTMP_ConnectStream %d", ret);
            ret = RTMP_ConnectStream(rtmp, 0);
            /* if (!ret) {
                 LOGI("RTMP_ConnectStream fail %d", ret);
                 break;
             }*/
            if (ret)break;
            sleep(3);
        } while (1);
    } while (0);

    if (!ret) {
        LOGI("connect fail %d", ret);
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = 0;
    } else {
        LOGI("connect result ---> %d", ret);
        /* delete (path);
    path = 0;*/
        // 发送 aac:audio specific config（告诉播放器怎么解码）
        if (audioChannel) {
            LOGI("发送 aac:audio specific config");
            RTMPPacket *packet = audioChannel->getAudioConfig();
            onPacketCallback(packet);
        }
        // 通知java可以开始推流
        javaCallHelper->onParpare(ret);
        // 开启一个线程，检测连接状态
        pthread_create(&connectStatePid, 0, checkConnectState, 0);
        //pthread_exit(&pid);
    }
    startTime = RTMP_GetTime();
    return 0;
}

void *checkConnectState(void *args) {
    flag = 1;
    LOGI("====================checkConnectState start ====================== %d", flag);
    int ret;
    while (flag) {
//        uint64_t nowTime = RTMP_GetTime();
        sleep(5);
//        if (nowTime - checkTime > 3000) {
//            checkTime = nowTime;
        LOGI("==================== checkConnectState loop ======================");
        if (rtmp) {
            ret = RTMP_IsConnected(rtmp);
            javaCallHelper->onConnectState(ret);
              if (!ret) {
                  flag = 0;
                  doDisconnect();
                 /* if (videoChannel) {
                      videoChannel->resetPts();
                  }*/
                  sleep(5);
                  LOGI("尝试重连");
                  pthread_create(&pid, 0, connect, 0);
                  break;
              }
        }
    }
//    }
    LOGI("====================checkConnectState 检查结束====================== %d", flag);
    //pthread_exit(&connectStatePid);

    return 0;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_bugull_rtmp_camera_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

/**
 * 初始化
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_bugull_rtmp_camera_RTMPClient_nativeInit(JNIEnv *env, jobject thiz) {
    javaCallHelper = new JavaCallHelper(javaVm, env, thiz);
    pthread_mutex_init(&mutex, 0);
}

/**
 * 反初始化
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_bugull_rtmp_camera_RTMPClient_nativeDeInit(JNIEnv *env, jobject thiz) {
    if (javaCallHelper) {
        delete (javaCallHelper);
        javaCallHelper = 0;
    }
    pthread_mutex_destroy(&mutex);
    startTime = 0;
}

/**
 * 连接
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_bugull_rtmp_camera_RTMPClient_nativeConnect(JNIEnv *env, jobject thiz, jstring url) {
    const char *path_ = env->GetStringUTFChars(url, 0);
    path = new char[strlen(path_) + 1];
    strcpy(path, path_);
    // 启动线程
    pthread_create(&pid, 0, connect, 0);
    env->ReleaseStringUTFChars(url, path_);
}
/**
 * 断开连接
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_bugull_rtmp_camera_RTMPClient_nativeDisConnect(JNIEnv *env, jobject thiz) {
    LOGI("Java_com_bugull_rtmp_camera_RTMPClient_nativeDisConnect");
    flag = 0;
    doDisconnect();
    if (javaCallHelper) {
        // 线程注意
        javaCallHelper->onConnectState(0, THREAD_MAIN);
    }
    if (videoChannel) {
        videoChannel->resetPts();
    }
    if (path) {
        delete (path);
        path = 0;
    }
}

/**
 * 初始视频解码器
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_bugull_rtmp_camera_RTMPClient_nativeInitVideoEnc(JNIEnv *env, jobject thiz, jint width,
                                                          jint height, jint fps, jint rate) {
    // 准备好编码器
    videoChannel = new VideoChannel();
    videoChannel->openCodec(width, height, fps, rate);
    videoChannel->setCallback(onPacketCallback);
}
/**
 * 释放视频解码器
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_bugull_rtmp_camera_RTMPClient_nativeReleaseVideoEnc(JNIEnv *env, jobject thiz) {
    LOGI("Java_com_bugull_rtmp_camera_RTMPClient_nativeReleaseVideoEnc");
    if (videoChannel) {
        delete (videoChannel);
        videoChannel = 0;
    }
}

/**
 * 发送视频
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_bugull_rtmp_camera_RTMPClient_nativeSendVideo(JNIEnv *env, jobject thiz,
                                                       jbyteArray data) {
//    int ret = checkConnectState();
//    if (!ret) {
//        return;
//    }
//    if (!rtmp){
//        return;
//    }
//    if (!RTMP_IsConnected(rtmp)){
//        return;
//    }

    LOGI("Send Video");
    // 视频编码与推流
    jbyte *buffer = env->GetByteArrayElements(data, 0);
    //pthread_mutex_lock(&mutex); // 这个锁？
    if (videoChannel) {
        videoChannel->encode(reinterpret_cast<uint8_t *>(buffer));
    }
    //pthread_mutex_destroy(&mutex);
    env->ReleaseByteArrayElements(data, buffer, 0);
}

/**
 * 初始音频解码器
 */
extern "C"
JNIEXPORT jint JNICALL
Java_com_bugull_rtmp_camera_RTMPClient_nativeInitAudioEnc(JNIEnv *env, jobject thiz,
                                                          jint sample_rate,
                                                          jint channels) {
    LOGI("audio InitAudioEnc");
    audioChannel = new AudioChannel();
    audioChannel->openCodec(sample_rate, channels);
    audioChannel->setCallback(onPacketCallback);
    int inputByteNumber = audioChannel->getInputByteNumber();
    return inputByteNumber;
}

/**
 * 释放音频解码器
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_bugull_rtmp_camera_RTMPClient_nativeReleaseAudioEnc(JNIEnv *env, jobject thiz) {
    LOGI("audio ReleaseAudio");
    if (audioChannel) {
        delete (audioChannel);
        audioChannel = 0;
    }
}

/**
 * 发送音频
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_bugull_rtmp_camera_RTMPClient_nativeSendAudio(JNIEnv *env, jobject thiz,
                                                       jbyteArray byte_array, jint len) {
//    int ret = checkConnectState();
//    if (!ret) {
//        return;
//    }
    LOGI("audio SendAudio");
    jbyte *data = env->GetByteArrayElements(byte_array, 0);

//    pthread_mutex_lock(&mutex);
    if (audioChannel) {
        audioChannel->encode(reinterpret_cast<int32_t *>(data), len);
    }
//    pthread_mutex_destroy(&mutex);
    env->ReleaseByteArrayElements(byte_array, data, 0);
}

