//
// Created by xpl on 2021/6/29.
//

#ifndef RTMP_CAM_H264_H
#define RTMP_CAM_H264_H

#include <jni.h>
#include <string>
#include <log.h>
#include "librtmp/rtmp.h"
#include <android/log.h>
#include "librtmp/rtmp_sys.h"
#include <stdint.h>
#include <math.h>


#define LOG(...) __android_log_print(ANDROID_LOG_INFO,"_rtmp_c_h264",__VA_ARGS__)
/**
 * 初始化并连接到服务器
 *
 * @param url 服务器上对应webapp的地址
 *
 * @成功则返回1 , 失败则返回0
 */
int RTMP264_Connect(const char* url);

/**
 * 将内存中的一段H.264编码的视频数据利用RTMP协议发送到服务器
 *
 * @param read_buffer 回调函数，当数据不足的时候，系统会自动调用该函数获取输入数据。
 *					2个参数功能：
 *					uint8_t *buf：外部数据送至该地址
 *					int buf_size：外部数据大小
 *					返回值：成功读取的内存大小
 * @成功则返回1 , 失败则返回0
 */
int RTMP264_Send(int (*read_buffer)(unsigned char*buf,int buf_size));

/**
 * 断开连接，释放相关的资源。
 *
 */
void RTMP264_Close();

#endif //RTMP_CAM_H264_H
