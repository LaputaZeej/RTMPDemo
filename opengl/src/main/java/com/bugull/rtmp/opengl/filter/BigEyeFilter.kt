package com.bugull.rtmp.opengl.filter

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import com.bugull.rtmp.opengl.R
import com.bugull.rtmp.opengl.face.Face
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Author by xpl, Date on 2021/6/4.
 */
class BigEyeFilter(
    ctx: Context,
    private val scaleX: Float = 0.3f,
    name: String = "BigEyeFilter",
) : AbstractFboFilter(ctx, R.raw.base_vert, R.raw.bigeye_frag, name) {
    private val vLeftEye: Int
    private val vRightEye: Int
    private val vScale: Int
    private val vScale2: Int
    private val left: FloatBuffer
    private val right: FloatBuffer
    private val scale: FloatBuffer
    private val scale2: FloatBuffer
    var face: Face? = null

    init {
        left = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).asFloatBuffer()
        right = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).asFloatBuffer()
        scale = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).asFloatBuffer()
        scale2 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asFloatBuffer()

        vLeftEye = GLES20.glGetUniformLocation(program, "left_eye")
        vRightEye = GLES20.glGetUniformLocation(program, "right_eye")
        vScale = GLES20.glGetUniformLocation(program, "scale")
        vScale2 = GLES20.glGetUniformLocation(program, "scale2")

    }

    override fun onDraw(texture: Int, chain: FilterChain): Int {
        this.face = chain.chainContext.face
        return super.onDraw(texture, chain)
    }

    override fun onBeforeDraw(chainContext: FilterChain.FilterChainContext) {
        super.onBeforeDraw(chainContext)
        Log.i("_opengl_", "---> onBeforeDraw  face = $face")
        face?.let { f ->
            val x: Float = f.left_x / f.imgWidth // 0-1 不需要具体的值 *width
            val y: Float = 1.0f - f.left_y / f.imgHeight //0-1
            left.clear()
            left.put(x).put(y).position(0)
            GLES20.glUniform2fv(vLeftEye, 1, left)

            val xR: Float = f.right_x / f.imgWidth
            val yR: Float = 1.0f - f.right_y / f.imgHeight
            right.clear()
            right.put(xR).put(yR).position(0)
            GLES20.glUniform2fv(vRightEye, 1, right)

            scale.clear()
            scale.put(scaleX).put(scaleX).position(0)
            GLES20.glUniform2fv(vScale, 1, scale)

            scale2.clear()
            GLES20.glUniform1f(vScale2,scaleX)
        }
    }
}