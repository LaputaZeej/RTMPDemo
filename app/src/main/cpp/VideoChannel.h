//
// Created by xpl on 2021/5/29.
//


#ifndef RTMP_CAM_VIDEOCHANNEL_H
#define RTMP_CAM_VIDEOCHANNEL_H
#include <jni.h>
#include <x264.h>
#include "PacketCallBack.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"_rtmp_c",__VA_ARGS__)

/**
 * 封装x264编码操作
 */
class VideoChannel {
public:
    VideoChannel();
    ~VideoChannel();
    void encode(uint8_t*data);

public:
    void openCodec(int width,int height,int fps,int bitrate);
    void setCallback(PacketCallBack callBack);

    void resetPts(){
        i_pts=0;
    }


private:
    x264_t *codec;
    int ySize;
    int uvSize;
    int width;
    int height;
    int64_t i_pts;
    PacketCallBack callBack;

    void sendVideoConfig(uint8_t *sps, int spslen, uint8_t *pps, int ppslen);

    void sendFrame(int type, uint8_t *payload, int plaload);
};




#endif //RTMP_CAM_VIDEOCHANNEL_H
