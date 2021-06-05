package com.bugull.rtmp.opengl.filter

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import com.bugull.rtmp.opengl.R

/**
 * Author by xpl, Date on 2021/6/3.
 *
 *  着色器 samplerExternalOES -> sampler2D
 */
class CameraFilter(ctx: Context) :
    AbstractFboFilter(ctx, R.raw.camera_vert, R.raw.camera_frag, "CameraFilter") {
    private var mtx: FloatArray = FloatArray(0)
    private var vMatrix: Int = 0

    init {
        vMatrix = GLES20.glGetUniformLocation(program, SHADER_KEY_V_MATRIX)
    }

    override fun onDraw(texture: Int, chain: FilterChain): Int {
        mtx = chain.chainContext.mtx
       val r =  super.onDraw(texture, chain)
        //Log.i("_opengl_","---> onDraw $r")
        return r
    }

    override fun onBeforeDraw() {
        //if (mtx.isEmpty()) return
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mtx, 0)
    }
}