//
// Created by xpl on 2021/5/29.
//

#include "VideoChannel.h"
#include <android/log.h>
#include <cstring>
#include <alloca.h>
#include "librtmp/rtmp.h"
#include <jni.h>

VideoChannel::VideoChannel() {

}

VideoChannel::~VideoChannel() {
    if (codec) {
        x264_encoder_close(codec);
        codec = 0;
    }
}

void x264_log_default2(void *, int i_level, const char *psz, va_list list) {
    __android_log_vprint(ANDROID_LOG_ERROR, "x264", psz, list);
}

void VideoChannel::openCodec(int width, int height, int fps, int bitrate) {
    // 编码器参数
    x264_param_t param;
    // 参数二：编码速度与质量控制，使用最快的模式编码 x264_preset_names
    const char *preset = x264_preset_names[0];
    // 参数三：编码场景 无延迟编码
    const char *tune = x264_tune_names[7];
    x264_param_default_preset(&param, preset, tune);
    // main base_line ：high
    //base_line 3.2 编码规格 无B帧(B帧：数据量最小，但是解码速度最慢)
    param.i_level_idc = 32;
    //输入数据格式
    param.i_csp = X264_CSP_I420;
    param.i_width = width;
    param.i_height = height;
    //无b帧
    param.i_bframe = 0;
    //参数i_rc_method表示码率控制，CQP(恒定质量)，CRF(恒定码率)，ABR(平均码率)
    param.rc.i_rc_method = X264_RC_ABR;
    //码率(比特率,单位Kbps)
    param.rc.i_bitrate = bitrate / 1000;
    //瞬时最大码率
    param.rc.i_vbv_max_bitrate = bitrate / 1000 * 1.2;

    //帧率
    param.i_fps_num = fps;
    param.i_fps_den = 1;
    param.pf_log = x264_log_default2;
    //帧距离(关键帧)  2s一个关键帧
    param.i_keyint_max = fps * 2;
    // 是否复制sps和pps放在每个关键帧的前面 该参数设置是让每个关键帧(I帧)都附带sps/pps。
    param.b_repeat_headers = 1;
    //不使用并行编码。zerolatency场景下设置param.rc.i_lookahead=0;
    // 那么编码器来一帧编码一帧，无并行、无延时
    param.i_threads = 1;
    param.rc.i_lookahead = 0;
    x264_param_apply_profile(&param, "baseline");

    codec = x264_encoder_open_159(&param);
    ySize = width * height;
    uvSize = (width >> 1) * (height >> 1);
    this->width = width;
    this->height = height;
}

void VideoChannel::setCallback(PacketCallBack callBack) {
    this->callBack = callBack;
}

void VideoChannel::encode(uint8_t *data) {
    LOGI("encode video ");
    // 待编码数据
    x264_picture_t pic_in;
    x264_picture_alloc(&pic_in, X264_CSP_I420, width, height);
    pic_in.img.plane[0] = data; // Y
    pic_in.img.plane[1] = data + ySize;// U
    pic_in.img.plane[2] = data + ySize + uvSize;// V
    // 编码的i_pts每次需要增长,相当于编号
    pic_in.i_pts = i_pts++;
    //
    x264_nal_t *pp_nal;
    int pi_nal;
    //
    x264_picture_t pic_out;
    // 编码
    int error = x264_encoder_encode(
            //编码器
            codec,
            // 编码结果，指针的指针，表示out输出,修改指针的指向?
            &pp_nal,
            // sps pps i 输出了多少个nal(帧)
            &pi_nal,
            // 输入
            &pic_in,
            //输出
            &pic_out
    );
    if (error <= 0) {
        // 编码失败或还需要更多信息才能编码 等等
        // 比如：MediaCodec.INFO_TRY_AGAIN_LATER..
        LOGI("encode video FAIL %d", error);
        return;
    }

    int spslen, ppslen;
    uint8_t *sps;
    uint8_t *pps;
    // 一个原始的NALU单元结构如下[StartCode][NALU Header][NALU Payload]三部分
    // StartCode，是一个NALU单元开始，必须是00 00 00 01 或者00 00 01。
    for (int i = 0; i < pi_nal; ++i) {
        int type = pp_nal[i].i_type;
        //
        uint8_t *p_payload = pp_nal[i].p_payload;
        int i_plaload = pp_nal[i].i_payload;
        if (type == NAL_SPS) {

            // sps 解码相关 后面肯定跟着pps
            spslen = i_plaload - 4; // 去掉间隔00 00 00 01
            sps = (uint8_t *) alloca(spslen);// 申请栈内存 不需要释放
            memcpy(sps, p_payload + 4, spslen);

        } else if (type == NAL_PPS) {
            // pps 解码相关
            ppslen = i_plaload - 4; // 去掉间隔00 00 00 01
            pps = (uint8_t *) alloca(ppslen);// 申请栈内存 不需要释放
            memcpy(pps, p_payload + 4, ppslen);
            // 后面肯定有i帧
            // 发送sps pps
            sendVideoConfig(sps, spslen, pps, ppslen);
        } else {
            // 关键帧 i
            // 非关键帧 pb
            sendFrame(type, p_payload, i_plaload);
        }


    }

}

void VideoChannel::sendVideoConfig(uint8_t *sps, int spslen, uint8_t *pps, int ppslen) {
    LOGI("send Video Config ");
    RTMPPacket *packet = new RTMPPacket;
    int bodySize = 13 + spslen + 3 + ppslen;
    RTMPPacket_Alloc(packet, bodySize);
    int i = 0;
    //固定头
    packet->m_body[i++] = 0x17;
    //类型
    packet->m_body[i++] = 0x00;
    //composition time 0x000000
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;

    //版本
    packet->m_body[i++] = 0x01;
    //编码规格
    packet->m_body[i++] = sps[1];
    packet->m_body[i++] = sps[2];
    packet->m_body[i++] = sps[3];
    packet->m_body[i++] = 0xFF;

    //整个sps
    packet->m_body[i++] = 0xE1;
    //sps长度
    packet->m_body[i++] = (spslen >> 8) & 0xff;
    packet->m_body[i++] = spslen & 0xff;
    memcpy(&packet->m_body[i], sps, spslen);
    i += spslen;

    //pps
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = (ppslen >> 8) & 0xff;
    packet->m_body[i++] = (ppslen) & 0xff;
    memcpy(&packet->m_body[i], pps, ppslen);

    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = bodySize;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    packet->m_nTimeStamp = 0; // sps pps 时间戳无意义
    packet->m_hasAbsTimestamp = 0;// 是否使用相对时间 无意义
    packet->m_nChannel = 0x10;

    callBack(packet);

}

void VideoChannel::sendFrame(int type, uint8_t *p_payload, int i_payload) {
    LOGI("send Video Frame ");
//去掉 00 00 00 01 / 00 00 01
    if (p_payload[2] == 0x00) {
        i_payload -= 4;
        p_payload += 4;
    } else if (p_payload[2] == 0x01) {
        i_payload -= 3;
        p_payload += 3;
    }
    RTMPPacket *packet = new RTMPPacket;
    int bodysize = 9 + i_payload;
    RTMPPacket_Alloc(packet, bodysize);
    RTMPPacket_Reset(packet);
//    int type = payload[0] & 0x1f;
    packet->m_body[0] = 0x27;
    //关键帧
    if (type == NAL_SLICE_IDR) {
        packet->m_body[0] = 0x17;
    }
    //类型
    packet->m_body[1] = 0x01;
    //时间戳
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;
    //数据长度 int 4个字节 相当于把int转成4个字节的byte数组
    packet->m_body[5] = (i_payload >> 24) & 0xff;
    packet->m_body[6] = (i_payload >> 16) & 0xff;
    packet->m_body[7] = (i_payload >> 8) & 0xff;
    packet->m_body[8] = (i_payload) & 0xff;

    //图片数据
    memcpy(&packet->m_body[9], p_payload, i_payload);

    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = bodysize;
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nChannel = 0x10;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    callBack(packet);
}



