//
// Created by xpl on 2021/5/30.
//

#ifndef RTMP_CAM_AUDIOCHANNEL_H
#define RTMP_CAM_AUDIOCHANNEL_H
#include <jni.h>
#include <android/log.h>
#include <faac.h>
#include "PacketCallBack.h"

#define LOGA(...) __android_log_print(ANDROID_LOG_INFO,"_rtmp_c_AUDIO",__VA_ARGS__)

class AudioChannel {

public:
    AudioChannel();

    ~AudioChannel();



public:
    void openCodec(int sampleRate, int channels);

    int getInputByteNumber() {
        return inputByteNumber;
    }

    void encode(int32_t *data, int i);

    void setCallback(PacketCallBack callBack){
        this->callBack = callBack;
    }

    RTMPPacket * getAudioConfig();

private:
    faacEncHandle codec = 0;
    unsigned long inputByteNumber = 0;
    unsigned char *outputBuffer=0;
    unsigned long maxOutputBytes;
    PacketCallBack callBack;

    void closeCodec();
};


#endif //RTMP_CAM_AUDIOCHANNEL_H
