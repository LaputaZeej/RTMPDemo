package com.bugull.rtmp.opengl.face

import android.annotation.SuppressLint

/**
 * Author by xpl, Date on 2021/6/4.
 */
class FaceTracker(faceModel: String, landmarkerModel: String) {

    private val mNativeObj: Long = nativeCreateObject(faceModel, landmarkerModel)

    fun release() {
        nativeDestroyObject(mNativeObj)
    }

    fun start(){
        nativeStart(mNativeObj)
    }

    fun stop(){
        nativeStop(mNativeObj)
    }

    fun detect(inputImage: ByteArray,width: Int,height:Int,rotationDegrees: Int):Face{
        return nativeDetect(mNativeObj,inputImage,width,height,rotationDegrees)
    }

    companion object {
        init {
            System.loadLibrary("FaceTracker")
        }

        @SuppressLint("SdCardPath")
        const val S1 = "/sdcard/lbpcascade_frontalface.xml"
        @SuppressLint("SdCardPath")
        const val S2 = "/sdcard/pd_2_00_pts5.dat"
    }

    external fun nativeCreateObject(faceModel: String, landmarkerModel: String): Long
    external fun nativeDestroyObject(thiz: Long)
    external fun nativeStart(thiz: Long)
    external fun nativeStop(thiz: Long)
    external fun nativeDetect(
        thiz: Long,
        inputImage: ByteArray,
        width: Int,
        heigh: Int,
        rotationDegrees: Int,
    ): Face


}