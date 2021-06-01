package com.bugull.rtmp.camera

import android.util.Log
import androidx.lifecycle.LifecycleOwner

/**
 * Author by xpl, Date on 2021/5/28.
 */
class RTMPClient(
    private val lifecycleOwner: LifecycleOwner,
    private val onConnectedListener: OnConnectedListener,
) {

    var isConnect: Boolean = false
    private var isNativeConnect = false
    var width: Int = 0
    var height: Int = 0
    private var audioChannel: AudioChannel? = null
    private var url: String = ""

    init {
        nativeInit()
    }

    fun canWork() = isConnect && isNativeConnect

    fun initVideo(width: Int, height: Int, fps: Int, rate: Int) {
        logger("--> initVideo $width*$height $fps $rate")
        nativeInitVideoEnc(width, height, fps, rate)
        this.width = width
        this.height = height
    }

    fun initAudio(sampleRate: Int, channels: Int) {
        logger("--> initAudio $sampleRate $channels ")
        audioChannel = AudioChannel(sampleRate, channels, this)
        val inputByteNumber = nativeInitAudioEnc(sampleRate, channels)
        logger("--> initAudio inputByteNumber = $inputByteNumber ")
        audioChannel?.setInputByteNumber(inputByteNumber)

    }

    fun startLive(url: String) {
        if (!isConnect) {
            logger("--> startLive $url")
            this.url = url
            nativeConnect(url)
            isConnect = true
            onConnectedListener(true, ConnectState.connectState(isNativeConnect))
        }
    }

    fun sendVideo(data: ByteArray) {
        if (canWork()) {
            // logger("--> sendVideo ${data.size}")
            nativeSendVideo(data)
        }
    }


    fun sendAudio(data: ByteArray, len: Int) {
        if (canWork()) {
            // logger("--> sendAudio")
            nativeSendAudio(data, len)
        }
    }

    fun stopLive() {
        isConnect = false
        onConnectedListener(false, ConnectState.connectState(false))
        audioChannel?.stop()
        nativeDisConnect()
        logger("--> stopLive")
    }

    fun release() {
        audioChannel?.release()
        nativeReleaseVideoEnc()
        nativeReleaseAudioEnc()
        nativeDeInit()
        logger("--> release")
    }

    private fun onPrepare(state: Boolean) {
        this.isNativeConnect = state
        logger("--> onPrepare :$state")
        if (canWork()) {
            audioChannel?.startAudio()
            onConnectedListener(true, ConnectState.connectState(true))
        }
    }

    private fun onConnectState(state: Boolean) {
        logger("--> onConnectState :$state")

        this.isNativeConnect = state && isConnect

        onConnectedListener(isConnect, ConnectState.connectState(isNativeConnect))

    }

    // Audio
    external fun nativeInitAudioEnc(sampleRate: Int, channels: Int): Int
    external fun nativeSendAudio(byteArray: ByteArray, len: Int)
    external fun nativeReleaseAudioEnc()

    // video

    external fun nativeInit()

    external fun nativeDeInit()

    external fun nativeConnect(url: String)

    external fun nativeDisConnect()

    external fun nativeSendVideo(data: ByteArray)

    external fun nativeInitVideoEnc(width: Int, height: Int, fps: Int, rate: Int)

    external fun nativeReleaseVideoEnc()

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }

        fun logger(msg: String?) {
            Log.i("_rtmp_", "${msg}")
        }
    }
}

typealias OnConnectedListener = (Boolean, ConnectState) -> Unit

sealed class ConnectState(val text: String) {
    object Connnected : ConnectState("在线")
    object Disconnected : ConnectState("离线")
    object Connnecting : ConnectState("连接中")

    companion object {
        fun connectState(state: Boolean) = if (state) Connnected else Disconnected
    }
}