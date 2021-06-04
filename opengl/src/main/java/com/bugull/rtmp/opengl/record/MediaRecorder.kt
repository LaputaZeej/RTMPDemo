package com.bugull.rtmp.opengl.record

import android.content.Context
import android.media.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import com.bugull.rtmp.opengl.DEFAULT_H
import com.bugull.rtmp.opengl.DEFAULT_W
import com.bugull.rtmp.opengl.TAG
import java.io.IOException
import java.nio.ByteBuffer


/**
 * Author by xpl, Date on 2021/6/3.
 */
class MediaRecorder(
    ctx: Context,
    private val path: String,
    private val glContext: android.opengl.EGLContext,
    private val width: Int = DEFAULT_W,
    private val height: Int = DEFAULT_H,
) {
    private val context = ctx.applicationContext
    private var mMediaCodec: MediaCodec? = null
    private var mMediaMuxer: MediaMuxer? = null
    private var mSurface: Surface? = null
    private var mHandler: Handler? = null
    private var eglEnv: EGLEnv? = null

    private var start: Boolean = false
    private var mLastTimeStamp: Long = 0
    private var track: Int = 0
    private var speed: Float = 0f

    private fun selectSupportCodec(mimeType: String): MediaCodecInfo? {
        val numCodecs: Int = MediaCodecList.getCodecCount()
        for (i in 0 until numCodecs) {
            val codecInfo: MediaCodecInfo = MediaCodecList.getCodecInfoAt(i)
            // 判断是否为编码器，否则直接进入下一次循环
            if (!codecInfo.isEncoder) {
                continue
            }
            // 如果是编码器，判断是否支持Mime类型
            val types = codecInfo.supportedTypes
            for (j in types.indices) {
                if (types[j].equals(mimeType, ignoreCase = true)) {
                    return codecInfo
                }
            }
        }
        return null
    }

    @Throws(IOException::class)
    fun start(speed: Float) {

        Log.i(TAG, "MediaRecorder::start $speed")
        this.speed = speed

        val format =
            MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        val codecInfo = mMediaCodec?.getCodecInfo()
        Log.i(TAG, "codecInfo =  $codecInfo")
        val collectors = codecInfo?.apply {
            Log.i(TAG, "isEncoder = ${this.isEncoder}")
            Log.i(TAG, "isEncoder = ${this.name}")
        }?.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)?.colorFormats
        Log.i(TAG, "collectors $collectors ${collectors?.size}")
        collectors?.forEach {
            if (it == MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface) {
                Log.i(TAG, "    *************$it")
            } else {
                Log.i(TAG, "    ------------ $it")
            }
        }
        Log.i(TAG, "MediaRecorder::start 0000000 $speed")
        // 颜色空间 从surface中获取
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        // 码率
        format.setInteger(MediaFormat.KEY_BIT_RATE, 1500_000)
        // 帧率
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 25)
        // 关键帧间隔
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10)

        //format.setInteger(MediaFormat.KEY_WIDTH, width)
        //format.setInteger(MediaFormat.KEY_HEIGHT, height)

        // 方式一：高版本出错，可能低版本可以
        // mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        // 方式二：OK
        val codecInfos = selectSupportCodec(MediaFormat.MIMETYPE_VIDEO_AVC)
        mMediaCodec = MediaCodec.createByCodecName(codecInfos?.name ?: "")

        mMediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        // 编码的画面
        mSurface = mMediaCodec?.createInputSurface()
        // 混合器 将编码的h.264封装为MP4
        // -封装格式（容器） Mp4 mp3 存贮的就是编码格式
        // -编码格式 h264
        mMediaMuxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        mMediaCodec?.start()

        val handlerThread = HandlerThread("codec-opengl")
        handlerThread.start()
        mHandler = Handler(handlerThread.looper)
        mHandler?.post {
            if (mSurface != null) {
                eglEnv = EGLEnv(context, glContext, mSurface!!, width, height)
            }
            start = true
        }
    }


    fun fireFrame(texture: Int, timestamp: Long) {
        if (!start) {
            return
        }
        Log.i(TAG, "MediaRecorder::fireFrame $start $texture $timestamp")
        mHandler?.post {
            try {
                eglEnv?.draw(texture, timestamp)
                codec(false)
            } catch (e: Throwable) {
                e.printStackTrace()
            }

        }
    }

    private fun codec(endOfStream: Boolean) {
        Log.i(TAG, "MediaRecorder::codec")
        mMediaCodec?.let { codec ->
            if (endOfStream) {
                codec.signalEndOfInputStream()
                return
            }
            while (true) {

                if(!start){
                    break
                }
                Log.i(TAG, "    MediaRecorder::codec ing ... $start")
                //获得输出缓冲区 (编码后的数据从输出缓冲区获得)
                val bufferInfo = MediaCodec.BufferInfo()
                val encoderStatus = codec.dequeueOutputBuffer(bufferInfo, 10000)
                //需要更多数据
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Log.i(TAG, "        MediaRecorder::codec ing ... <1>")
                    //如果是结束那直接退出，否则继续循环
                    if (!endOfStream) {
                        Log.i(TAG, "        MediaRecorder::codec ing ... <1> endOfStream")
                        break
                    }
                    Log.i(TAG, "        MediaRecorder::codec ing ... <1> end")
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.i(TAG, "        MediaRecorder::codec ing ... <2>")
                    //输出格式发生改变  第一次总会调用所以在这里开启混合器
                    val newFormat = codec.outputFormat
                    track = mMediaMuxer!!.addTrack(newFormat)
                    mMediaMuxer?.start()
                    Log.i(TAG, "        MediaRecorder::codec ing ... <2> end")
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    //可以忽略
                    Log.i(TAG, "        MediaRecorder::codec ing ... <3>")
                } else {
                    Log.i(TAG, "        MediaRecorder::codec ing ... <4> ")
                    //调整时间戳 实现快动作/慢动作
                    bufferInfo.presentationTimeUs = (bufferInfo.presentationTimeUs / speed).toLong()
                    //有时候会出现异常 ： timestampUs xxx < lastTimestampUs yyy for Video track
                    if (bufferInfo.presentationTimeUs <= mLastTimeStamp) {
                        bufferInfo.presentationTimeUs =
                            ((mLastTimeStamp + 1000000 / 25 / speed).toLong())
                    }
                    mLastTimeStamp = bufferInfo.presentationTimeUs

                    //正常则 encoderStatus 获得缓冲区下标
                    val encodedData: ByteBuffer? = codec.getOutputBuffer(encoderStatus)
                    //如果当前的buffer是配置信息，不管它 不用写出去
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }
                    if (encodedData != null && bufferInfo.size != 0) {
                        //设置从哪里开始读数据(读出来就是编码后的数据)
                        encodedData.position(bufferInfo.offset)
                        //设置能读数据的总长度
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        //写出为mp4
                        mMediaMuxer?.writeSampleData(track, encodedData, bufferInfo)
                    }
                    // 释放这个缓冲区，后续可以存放新的编码后的数据啦
                    codec.releaseOutputBuffer(encoderStatus, false)
                    // 如果给了结束信号 signalEndOfInputStream
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                    Log.i(TAG, "        MediaRecorder::codec ing ... <4> end")
                }
            }
            Log.i(TAG, "    MediaRecorder::codec end")
        }
    }

    fun stop() {
        Log.i(TAG, "MediaRecorder::stop")
        // 释放
        start = false
        mHandler?.post {
            codec(true)
            mMediaCodec?.stop()
            mMediaCodec?.release()
            mMediaCodec = null
            cancelMediaMuxer()
            eglEnv?.release()
            eglEnv = null
            mHandler?.removeCallbacksAndMessages(null)
            mHandler?.looper?.quitSafely()
            mMediaMuxer = null
            mSurface = null
            mHandler = null
            mLastTimeStamp=0
        }
    }

    private fun cancelMediaMuxer() {
        try {
            //mMediaMuxer?.stop()
            mMediaMuxer?.release() // 已经调用了stop
            mMediaMuxer = null
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}