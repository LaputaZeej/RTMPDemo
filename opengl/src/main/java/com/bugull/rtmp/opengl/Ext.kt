package com.bugull.rtmp.opengl

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Author by xpl, Date on 2021/6/3.
 */
const val TAG = "_opengl_"

const val DEFAULT_W = 480
const val DEFAULT_H = 640

val Context.path_face_01
    get() = filesDir.absolutePath+ "/" + "lbpcascade_frontalface.xml"

val Context.path_face_02
    get() = filesDir.absolutePath+ "/" + "pd_2_00_pts5.dat"

val Context.savePath
    get() = filesDir.absolutePath + "/" + "pd_2_00_pts5.dat"

fun copyAssets2Sdcard(context: Context, src: String, dst: String) {
    var inputStream: InputStream? = null
    var fileOutputStream: FileOutputStream? = null
    try {
        val file = File(dst)
        if (!file.exists()) {
            inputStream = context.assets.open(src)
            fileOutputStream = FileOutputStream(file)
            val buffer = ByteArray(1024)
            while (true) {
                val read = inputStream.read(buffer)
                if (read != -1) {
                    fileOutputStream.write(buffer, 0, read)
                } else {
                    break
                }
            }

        }
        Log.i("_opengl", "ok")
    } catch (e: Throwable) {
        e.printStackTrace()
        Log.i("_opengl", "${e.message}")
    } finally {
        try {
            inputStream?.close()
            fileOutputStream?.close()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}