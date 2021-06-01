package com.bugull.rtmp.camera

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread

/**
 * Author by xpl, Date on 2021/5/30.
 */
class AudioChannel(val sampleRate: Int, val channels: Int, private val rtmpClient: RTMPClient) {
    private val handleThread: HandlerThread
    private val handler: Handler
    private val channelConfig: Int
    private var minBufferSize: Int
    private var audioRecord: AudioRecord? = null
    private var buffer: ByteArray? = null
    private var isLiving: Boolean = false

    init {
        channelConfig =
            if (channels == 2) {
                AudioFormat.CHANNEL_IN_STEREO
            } else {
                AudioFormat.CHANNEL_IN_MONO
            }
        minBufferSize =
            AudioRecord.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT)
        handleThread = HandlerThread("AudioChannel")
        handleThread.start()
        handler = Handler(handleThread.looper)
    }

    fun startAudio() {
        if (isLiving) return
        isLiving = true
        handler.post {
            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize
            )
            RTMPClient.logger("--> startAudio1 $audioRecord ")
//            while (isLiving) {
            try {
                audioRecord?.let { _audioRecord ->
                    RTMPClient.logger("--> startAudio2 ${_audioRecord.recordingState} ${buffer} ${buffer?.size} ")
                    _audioRecord.startRecording()
                    while (_audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        buffer?.let { _buff ->

                            val size = _audioRecord.read(_buff, 0, _buff.size)
                            //RTMPClient.logger("--> startAudio size = $size")
                            if (size > 0) {
                                rtmpClient.sendAudio(_buff, size shr 1)
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
//            }
            isLiving = false
        }
    }

    fun stop() {
        isLiving = false
        audioRecord?.stop()
    }

    fun setInputByteNumber(inputByteNumber: Int) {
        buffer = ByteArray(inputByteNumber)
        minBufferSize = if (inputByteNumber > minBufferSize)
            inputByteNumber else minBufferSize
    }

    fun release() {
        isLiving = false
        handler.removeCallbacksAndMessages(null)
        handleThread.quitSafely()
        audioRecord?.release()
    }
}