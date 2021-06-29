package com.bugull.rtmp.opengl.filter

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import com.bugull.rtmp.opengl.R
import javax.microedition.khronos.opengles.GL
import kotlin.math.roundToInt

/**
 * Author by xpl, Date on 2021/6/5.
 */
class StickFilter(
    ctx: Context,
    name: String = "StickFilter",
) : AbstractFboFilter(ctx, R.raw.base_vert, R.raw.base_frag, name) {
    private var texturesPic: IntArray = IntArray(1) // 贴图纹理

    init {
//        textureBuffer.clear()
//        textureBuffer.put(TEXURE_180)
        OpenGLUtils.glGenTextures(texturesPic)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturesPic[0])
        val pic = BitmapFactory.decodeResource(ctx.resources, R.drawable.love)
        // 把图片加载到纹理当中
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, pic, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    override fun afterDraw(chainContext: FilterChain.FilterChainContext) {
        super.afterDraw(chainContext)
        // 画鼻子
        chainContext.face?.let { f ->


            // 开启混合模式
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            // 计算坐标
            // 1. java
            //      GLES20.glViewport(0, 0, chainContext.width, chainContext.height)
            // 2.opengl
            //      世界坐标 中心点
            //      纹理坐标 左下角

            // 方式1：
            val x = chainContext.width * f.nose_x / f.imgWidth // 鼻子中心点x
            val y = chainContext.height * (1 - f.nose_y / f.imgHeight) // 鼻子中心点y

            // 左右嘴角的宽
            val mrx = chainContext.width * (f.mouseRight_x / f.imgWidth)
            val mlx = chainContext.width * (f.mouseLeft_x / f.imgWidth)
            val noseW = (mrx - mlx) * 0.75f
            // 嘴角的Y与鼻子中心点的差
            val mly = chainContext.height * (1 - f.mouseLeft_y / f.imgHeight)
            val noseH = (y - mly) * 0.75f
            GLES20.glViewport(
                (x - noseW / 2).roundToInt(),
                (y - noseH / 2).roundToInt(),
                noseW.roundToInt(),
                noseH.roundToInt())

            // 开始draw
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

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0) // GL_TEXTURE0 1层
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturesPic[0])
            GLES20.glUniform1i(vTexture, 0)
            // 通知画画
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            // 关闭混合模式
            GLES20.glEnable(GLES20.GL_BLEND)
        }

    }


}