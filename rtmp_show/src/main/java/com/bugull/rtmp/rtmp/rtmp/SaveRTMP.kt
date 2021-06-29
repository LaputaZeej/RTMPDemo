package com.bugull.rtmp.rtmp.rtmp

import android.util.Log

/**
 * Author by xpl, Date on 2021/6/28.
 */
class SaveRTMP {

    fun saveRtmp(url: String, path: String) {
        nativeSaveRtmp(url, path)
    }

    fun sendRtmp(url: String, path: String) {
        Log.i("_rtmp_","sendRtmp $url $path")
        nativeSendRtmp(url, path)
    }

    fun close() {
        nativeClose()
    }


    external fun nativeSaveRtmp(url: String, path: String)
    external fun nativeClose()
    external fun nativeSendRtmp(url: String, path: String)

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}