package com.bugull.rtmp.opengl.filter

import android.content.Context
import android.opengl.GLES20
import com.bugull.rtmp.opengl.R

/**
 * Author by xpl, Date on 2021/6/3.
 *
 *  着色器 samplerExternalOES -> sampler2D
 */
class CameraFilter(ctx: Context) :
    AbstractFboFilter(ctx, R.raw.camera_vert, R.raw.camera_frag,"CameraFilter") {
    private var mtx: FloatArray = FloatArray(0)
    private var vMatrix: Int = 0

    init {
        vMatrix = GLES20.glGetUniformLocation(program, SHADER_KEY_V_MATRIX)
    }

    fun setTransformMatrix(mtx: FloatArray){
        this.mtx = mtx
    }

    override fun onBeforeDraw() {
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mtx, 0)
    }
}