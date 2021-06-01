//
// Created by xpl on 2021/5/30.
//


#include <cstdlib>
#include <malloc.h>
#include <cstring>
#include "librtmp/rtmp.h"
#include "AudioChannel.h"

AudioChannel::AudioChannel() {

}

AudioChannel::~AudioChannel() {
    closeCodec();
    if (outputBuffer) {
        free(outputBuffer);
        outputBuffer = 0;
    }
    LOGA("~AudioChannel");
}

void AudioChannel::openCodec(int sampleRate, int channels) {
    // 输入样本 样本数 2字节
    unsigned long inputSamples;
    codec = faacEncOpen(sampleRate, channels, &inputSamples, &maxOutputBytes);
    // 样本是16位 一个样本就是2个字节 ??
    inputByteNumber = inputSamples * 2;

    outputBuffer = static_cast<unsigned char *>(malloc(maxOutputBytes));

    // 当前编码器参数配置
    faacEncConfigurationPtr configurationPtr = faacEncGetCurrentConfiguration(codec);
    configurationPtr->mpegVersion = MPEG4;
    configurationPtr->aacObjectType = LOW;
    // 1:每一帧音频编码的结果数据都会携带ADTS（包含了采样、声道等信息的数据头）
    // 0：编码出aac裸数据
    configurationPtr->outputFormat = 0;
    configurationPtr->inputFormat = FAAC_INPUT_16BIT; // TOOD 从app传进来保持一致
    faacEncSetConfiguration(codec, configurationPtr);


}

void AudioChannel::closeCodec() {
    if (codec) {
        faacEncClose(codec);
        codec = 0;
    }
}

void AudioChannel::encode(int32_t *data, int len) {
    LOGA("encode audio");
    // 参数三：输入的样本数 len 样本数 java字节数/2
    // 参数四：输出 编码之后的结果
    // 参数五：编码结果缓存区能接受数据的个数
    int byteLen = faacEncEncode(codec, data, len, outputBuffer, maxOutputBytes);
    if (byteLen > 0) {
        RTMPPacket *packet = new RTMPPacket;
        RTMPPacket_Alloc(packet, byteLen + 2);
        packet->m_body[0] = 0xAF;
        packet->m_body[1] = 0x01;
        memcpy(&packet->m_body[2], outputBuffer, byteLen);

        packet->m_hasAbsTimestamp = 0;
        packet->m_nBodySize = byteLen + 2;
        packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
        packet->m_nChannel = 0x11;
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
        callBack(packet);
    }
}

// 相当于视频的sps/pps
// 视频 需要在i帧之前都发送一次
// 音频 发送音频之前只发送一次就可以
RTMPPacket *AudioChannel::getAudioConfig() {
    LOGA("get audio config");
    u_char *buf;
    u_long len;
    faacEncGetDecoderSpecificInfo(codec, &buf, &len);
    RTMPPacket *packet = new RTMPPacket;
    RTMPPacket_Alloc(packet, len + 2);
    packet->m_body[0] = 0xAF;
    packet->m_body[1] = 0x00;

    memcpy(&packet->m_body[2], buf, len);

    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = len + 2;
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nChannel = 0x11;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    return packet;
}
