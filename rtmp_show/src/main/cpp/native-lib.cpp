#include <jni.h>
#include <string>
#include <log.h>
#include "librtmp/rtmp.h"
#include <android/log.h>
#include "librtmp/rtmp_sys.h"
#include <stdint.h>
#include <math.h>
#include "H264.h"


JavaVM *javaVm = 0;

char *mPath = 0;
char *mUrl = 0;

bool start = 0;
bool b_send = 0;

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"_rtmp_c",__VA_ARGS__)
#define TAG "_rtmp_send_"

#define HTON16(x)  ((x>>8&0xff)|(x<<8&0xff00))
#define HTON24(x)  ((x>>16&0xff)|(x<<16&0xff0000)|(x&0xff00))
#define HTON32(x)  ((x>>24&0xff)|(x>>8&0xff00)|\
         (x<<8&0xff0000)|(x<<24&0xff000000))
#define HTONTIME(x) ((x>>16&0xff)|(x<<16&0xff0000)|(x&0xff00)|(x&0xff000000))

/*read 1 byte*/
int ReadU8(uint32_t *u8, FILE *fp) {
    if (fread(u8, 1, 1, fp) != 1)
        return 0;
    return 1;
}

/*read 2 byte*/
int ReadU16(uint32_t *u16, FILE *fp) {
    if (fread(u16, 2, 1, fp) != 1)
        return 0;
    *u16 = HTON16(*u16);
    return 1;
}

/*read 3 byte*/
int ReadU24(uint32_t *u24, FILE *fp) {
    if (fread(u24, 3, 1, fp) != 1)
        return 0;
    *u24 = HTON24(*u24);
    return 1;
}

/*read 4 byte*/
int ReadU32(uint32_t *u32, FILE *fp) {
    if (fread(u32, 4, 1, fp) != 1)
        return 0;
    *u32 = HTON32(*u32);
    return 1;
}

/*read 1 byte,and loopback 1 byte at once*/
int PeekU8(uint32_t *u8, FILE *fp) {
    if (fread(u8, 1, 1, fp) != 1)
        return 0;
    fseek(fp, -1, SEEK_CUR);
    return 1;
}

/*read 4 byte and convert to time format*/
int ReadTime(uint32_t *utime, FILE *fp) {
    if (fread(utime, 4, 1, fp) != 1)
        return 0;
    *utime = HTONTIME(*utime);
    return 1;

}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVm = vm;
    return JNI_VERSION_1_4;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bugull_rtmp_rtmp_rtmp_RTMPHelper_nativeSaveRtmp(JNIEnv *env, jobject thiz, jstring url,
                                                         jstring path) {

    RTMP_LogPrintf("_RTMP_log___", "开始保存");
    const char *_path = env->GetStringUTFChars(path, 0);
    const char *_url = env->GetStringUTFChars(url, 0);
    mPath = new char[strlen(_path) + 1];
    strcpy(mPath, _path);
    env->ReleaseStringUTFChars(path, _path);
    mUrl = new char[strlen(_url) + 1];
    strcpy(mUrl, _url);
    env->ReleaseStringUTFChars(url, _url);
    LOGI("start......path=%s url=%s", path, url);
    double duration = -1;
    int nRead;
    //is live stream ?
    bool bLiveStream = true;


    int bufsize = 1024 * 1024 * 10;
    char *buf = (char *) malloc(bufsize);
    memset(buf, 0, bufsize);
    long countbufsize = 0;

    FILE *fp = fopen(mPath, "wb");
    if (!fp) {
        LOGI("Open File Error.\n");
        return;
    }

    /* set log level */
    //RTMP_LogLevel loglvl=RTMP_LOGDEBUG;
    //RTMP_LogSetLevel(loglvl);

    RTMP *rtmp = RTMP_Alloc();
    RTMP_Init(rtmp);
    //set connection timeout,default 30s
    rtmp->Link.timeout = 10;
    // HKS's live URL
    if (!RTMP_SetupURL(rtmp, mUrl)) {
        LOGI("SetupURL Err\n");
        RTMP_Free(rtmp);
        return;
    }
    if (bLiveStream) {
        rtmp->Link.lFlags |= RTMP_LF_LIVE;
    }

    //1hour
    RTMP_SetBufferMS(rtmp, 3600 * 1000);

    if (!RTMP_Connect(rtmp, NULL)) {
        LOGI("Connect Err\n");
        RTMP_Free(rtmp);
        return;
    }

    if (!RTMP_ConnectStream(rtmp, 0)) {
        LOGI("ConnectStream Err\n");
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        return;
    }

    start = 1;
    while (start && (nRead = RTMP_Read(rtmp, buf, bufsize))) {
        fwrite(buf, 1, nRead, fp);

        countbufsize += nRead;
        LOGI("Receive: %5dByte, Total: %5.2fkB\n", nRead, countbufsize * 1.0 / 1024);
    }

    if (fp)
        fclose(fp);

    if (buf) {
        free(buf);
    }

    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = NULL;
    }

    if (mUrl) {
        delete[]mUrl;
        mUrl = 0;
    }

    if (mPath) {
        delete[]mPath;
        mPath = 0;
    }
    LOGI("end");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bugull_rtmp_rtmp_rtmp_RTMPHelper_nativeClose(JNIEnv *env, jobject thiz) {
    start = 0;
}

/**
 * 发送视频
 */

extern "C"
JNIEXPORT void JNICALL
Java_com_bugull_rtmp_rtmp_rtmp_RTMPHelper_nativeSendRtmp(JNIEnv *env, jobject thiz, jstring url,
                                                         jstring path) {
    LOGI("%s start send ...", TAG);
    const char *url_ = env->GetStringUTFChars(url, 0);
    char *sendUrl = new char[strlen(url_) + 1];
    strcpy(sendUrl, url_);
    env->ReleaseStringUTFChars(url, url_);
    const char *path_ = env->GetStringUTFChars(path, 0);
    char *sendPath = new char[strlen(path_) + 1];
    strcpy(sendPath, path_);
    env->ReleaseStringUTFChars(path, path_);

    LOGI("%s url = %s , path = %s", TAG, sendUrl, sendPath);

    RTMP *rtmp = NULL;
    RTMPPacket *packet = NULL;
    uint32_t start_time = 0;
    uint32_t now_time = 0;
    //
    long pre_frame_time = 0;
    long last_time = 0;
    int bNextIsKey = 1;
    char *pFileBuf = NULL;

    uint32_t preTagSize = 0;
    // packet attributes
    uint32_t type = 0;
    uint32_t dataLength = 0;
    uint32_t timestamp = 0;
    uint32_t streamId = 0;

    FILE *fp = NULL;
    fp = fopen(sendPath, "rb");
    if (!fp) {
        LOGI("%s open file error : %p \n", TAG, fp);
        delete[] sendUrl;
        sendUrl = 0;
        delete[] sendPath;
        sendPath = 0;
        return;
    }
    rtmp = RTMP_Alloc();
    RTMP_Init(rtmp);
    rtmp->Link.timeout = 5;
    if (!RTMP_SetupURL(rtmp, sendUrl)) {

        LOGI("%s RTMP_SetupURL error", TAG);
        delete[] sendUrl;
        sendUrl = 0;
        delete[] sendPath;
        sendPath = 0;
        RTMP_Free(rtmp);
        rtmp = 0;
        return;
    }
    RTMP_EnableWrite(rtmp);
    if (!RTMP_Connect(rtmp, NULL)) {
        LOGI("%s RTMP_Connect error", TAG);
        delete[] sendUrl;
        sendUrl = 0;
        delete[] sendPath;
        sendPath = 0;
        RTMP_Free(rtmp);
        rtmp = 0;
        return;

    }

    if (!RTMP_ConnectStream(rtmp, 0)) {
        LOGI("%s RTMP_ConnectStream error", TAG);
        delete[] sendUrl;
        sendUrl = 0;
        delete[] sendPath;
        sendPath = 0;
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = 0;
        return;

    }

    packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, 1024 * 64);
    RTMPPacket_Reset(packet);
    packet->m_hasAbsTimestamp = 0; // 相对时间
    packet->m_nChannel = 0x04; // ?
    packet->m_nInfoField2 = rtmp->m_stream_id; // ?通道

    LOGI("%s start to send data...", TAG);
    b_send = 1;

    // jump over FLV Header 9个字节
    fseek(fp, 9, SEEK_SET); // 指针；偏移9（负前正后）；从头开始
    // jump previous tag size #0 4个字节
    fseek(fp, 4, SEEK_CUR);
    // 到达第一个数据区
    start_time = RTMP_GetTime();
    LOGI("%s start_time=%ld", TAG, start_time);
    while (true) {
        LOGI("%s -----------111111111111------------", TAG);
        LOGI("%s ______now_time=%ld pre_frame_time=%ld lasttime=%ld bNextIsKey=%d", TAG, now_time,
             pre_frame_time,
             last_time, bNextIsKey);


//        bool a = (std::abs((long) ((now_time = RTMP_GetTime()) - start_time)) < pre_frame_time)
//                 && bNextIsKey;
//        if(a){
        if (
                (-((now_time = RTMP_GetTime()) - start_time) < (pre_frame_time))
                && bNextIsKey) {

            if (pre_frame_time > last_time) {
                // wait for 1 sec if the send process is too fast
                // this mechanism is not very good,need some improvement
                LOGI("%s TimeStamp:%8lu ms\n", TAG, pre_frame_time);
                last_time = pre_frame_time;
            }
            usleep(1000000);
            continue;
        }
        //not quite the same as FLV spec
        // 详见FLV格式 https://blog.csdn.net/leixiaohua1020/article/details/17934487
        // 一、读取FLV TAG header
        if (!ReadU8(&type, fp)) {// 读类型 1字节 0x08音频；0x09视频；0x12 script data
            LOGI("%s read type fail ", TAG);
            break;
        }

        if (!ReadU24(&dataLength, fp)) {// datasize 3字节 tag data部分大小
            LOGI("%s read dataLength fail ", TAG);
            break;
        }

        if (!ReadTime(&timestamp, fp)) {// tag 时间戳 4字节 前3个字节和后1个字节extra
            LOGI("%s read timestamp fail ", TAG);
            break;
        }

        if (!ReadU24(&streamId, fp)) {// streamId 3字节 总是0
            LOGI("%s read streamId fail ", TAG);
            break;
        }

        if (type != 0x08 && type != 0x09) { // 不是音视频tag
            LOGI("%s read 不是音视频tag ", TAG);
            fseek(fp, dataLength + 4/*一个 previous tag的小大 4个字节*/, SEEK_CUR);
            continue;
        }
        // 二、读取data
        if (fread(packet->m_body, 1, dataLength, fp) != dataLength) {
            LOGI("%s ______ 大小不对", TAG);
            break;
        }
        // 设置
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
        packet->m_nTimeStamp = timestamp;
        packet->m_packetType = type;
        packet->m_nBodySize = dataLength;
        // 记录
        pre_frame_time = timestamp;
        LOGI("%s ______send size = %6d pre_frame_time=%ld lasttime=%ld", TAG, dataLength,
             pre_frame_time, last_time);
        if (!RTMP_IsConnected(rtmp)) {
            LOGI("%s rtmp is not connected", TAG);
            break;
        }
        // 发送
        if (!RTMP_SendPacket(rtmp, packet, 0)) {
            LOGI("%s RTMP_SendPacket error ", TAG);
            break;
        }
        // 记录数据的size
        if (!ReadU32(&preTagSize, fp)) {
            break;
        }
        // 读取下一个tag的类型，然后又移回指针，现在的type为下一个tag的类型
        if (!PeekU8(&type, fp)) {
            break;
        }
        if (type == 0x09) {// 视频
            if (fseek(fp, 11, SEEK_CUR) != 0) { // 跳过头，来到数据data
                break;
            }
            if (!PeekU8(&type, fp)) { // 获取数据的第一个字节，前四位数值表示帧类型：1为keyframe关键帧
                break;
            }
            if (type == 0x17)// 前四位数值表示帧类型：1为keyframe关键帧
                bNextIsKey = 1;
            else
                bNextIsKey = 0;
            fseek(fp, -11, SEEK_CUR);// 返回原来的指针，即当前指针位置在下一个tag开头。
        }
    }
    LOGI("%s send data over !!!!", TAG);
    if (fp) {
        fclose(fp);
    }
    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = 0;
    }
    if (packet) {
        RTMPPacket_Free(packet);
        free(packet);
        packet = 0;
    }
    delete[] sendPath;
    sendPath = 0;
    delete[] sendUrl;
    sendUrl = 0;
}

FILE *fp_send_264;

int read_buffer(unsigned char *buf, int size) {
    if (!feof(fp_send_264)) {
        LOG("   -> send h264 read_buffer3333 ");
        int true_size = fread(buf, 1, size, fp_send_264);
        LOG("   -> send h264 read_buffer4444 %d ",true_size);
        return true_size;
    } else {
        LOG("   -> send h264 read_buffer5555 %d ",-1);
        return -1;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bugull_rtmp_rtmp_rtmp_RTMPHelper_nativeSendRtmpH264(JNIEnv *env, jobject thiz, jstring url,
                                                             jstring path) {
    LOG("send h264...");
    const char *url_ = env->GetStringUTFChars(url, 0);
    char *sendUrl = new char[strlen(url_) + 1];
    strcpy(sendUrl, url_);
    env->ReleaseStringUTFChars(url, url_);
    const char *path_ = env->GetStringUTFChars(path, 0);
    char *sendPath = new char[strlen(path_) + 1];
    strcpy(sendPath, path_);
    env->ReleaseStringUTFChars(path, path_);

    LOG("send h264. url = %s , path = %s", sendUrl, sendPath);
    fp_send_264 = fopen(sendPath, "rb");
    if (!fp_send_264) {
        LOGI("open file error : %p \n",  fp_send_264);
        delete[] sendUrl;
        sendUrl = 0;
        delete[] sendPath;
        sendPath = 0;
        return;
    }
    int ret = RTMP264_Connect(sendUrl);
    if (!ret) {
        LOG("send h264 RTMP264_Connect fail");
        return;
    }

    RTMP264_Send(read_buffer);

    RTMP264_Close();
}