package com.bugull.rtmp.opengl.filter

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import com.bugull.rtmp.opengl.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Author by xpl, Date on 2021/6/3.
 */
abstract class AbstractFilter(
    ctx: Context,
    private val vertexShaderId: Int = R.raw.base_vert,
    private val fragmentShaderId: Int = R.raw.base_frag,
    private val name: String = "",
) {
    private val context: Context = ctx.applicationContext
    private val vertexBuffer: FloatBuffer // 顶点坐标缓冲区
    private val textureBuffer: FloatBuffer // 纹理坐标

    protected var program: Int = 0
    private var vPosition: Int = 0
    private var vCoord: Int = 0
    private var vTexture: Int = 0

    init {
        vertexBuffer =
            ByteBuffer.allocateDirect(4 * 4 * 2).order(ByteOrder.nativeOrder()).asFloatBuffer()
        textureBuffer =
            ByteBuffer.allocateDirect(4 * 4 * 2).order(ByteOrder.nativeOrder()).asFloatBuffer()
        initCoord()
        initGL(context)
    }

    private fun initCoord() {
        vertexBuffer.clear()
        vertexBuffer.put(VERTEX)
        textureBuffer.clear()
        textureBuffer.put(TEXURE)
    }

    private fun initGL(context: Context) {
        //Log.i("_opengl_", "name = $name -> initGL")
        val vertexSharder = OpenGLUtils.readRawTextFile(context, vertexShaderId)
        val fragSharder = OpenGLUtils.readRawTextFile(context, fragmentShaderId)
        program = OpenGLUtils.loadProgram(vertexSharder, fragSharder)
        vPosition = GLES20.glGetAttribLocation(program, SHADER_KEY_V_POSITION)
        vCoord = GLES20.glGetAttribLocation(program, SHADER_KEY_V_COORD)
        vTexture = GLES20.glGetUniformLocation(program, SHADER_KEY_V_TEXTURE)
    }

    open fun onDraw(texture: Int, chain: FilterChain): Int {
        val chainContext = chain.chainContext
        Log.i("_opengl_",
            "AbstractFilter::onDraw ${name} ${chain.index} size = ${chain.filters.size} ${chainContext.width} * ${chainContext.height}")
        // 设置绘制区域
        GLES20.glViewport(0, 0, chainContext.width, chainContext.height)
        GLES20.glUseProgram(program)

        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(vPosition,
            2,
            GLES20.GL_FLOAT,/*归一化 normalized  [-1,1] . 把[2,2]转换为[-1,1]*/
            false,
            0,
            vertexBuffer)
        GLES20.glEnableVertexAttribArray(vPosition)

        textureBuffer.position(0)
        GLES20.glVertexAttribPointer(vCoord,
            2,
            GLES20.GL_FLOAT,/*归一化 normalized  [-1,1] . 把[2,2]转换为[-1,1]*/
            false,
            0,
            textureBuffer)
        GLES20.glEnableVertexAttribArray(vCoord)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glUniform1i(vTexture, 0)
        onBeforeDraw()
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        return texture
    }

    open fun release() {
        GLES20.glDeleteProgram(program)
    }

    open fun onBeforeDraw() {}

    companion object {
        val VERTEX: FloatArray = floatArrayOf(
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f
        )

        val TEXURE: FloatArray = floatArrayOf(
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        )

        internal const val SHADER_KEY_V_POSITION = "vPosition"
        internal const val SHADER_KEY_V_COORD = "vCoord"
        internal const val SHADER_KEY_V_TEXTURE = "vTexture"
        internal const val SHADER_KEY_V_MATRIX = "vMatrix"
    }
}