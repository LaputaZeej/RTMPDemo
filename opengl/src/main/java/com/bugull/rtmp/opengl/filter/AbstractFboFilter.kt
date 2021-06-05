package com.bugull.rtmp.opengl.filter

import android.content.Context
import android.opengl.GLES20

/**
 * Author by xpl, Date on 2021/6/3.
 *
 * 输出sampler2D
 */
abstract class AbstractFboFilter(
    ctx: Context,
    vertexShaderId: Int,
    fragmentShaderId: Int,
    name: String = "AbstractFboFilter",
) :
    AbstractFilter(ctx, vertexShaderId, fragmentShaderId, name) {
    private var frameBuffer: IntArray = IntArray(1)
    private var frameTextures: IntArray = IntArray(1)

    private fun createFrame(width: Int, height: Int) {
        releaseFrame()
        // 1.创建FBO
        frameBuffer = IntArray(1)
        GLES20.glGenFramebuffers(1, frameBuffer, 0)
        // 2.配置纹理
        frameTextures = IntArray(1)
        // 纹理和实际几何大小不一致
        OpenGLUtils.glGenTextures(frameTextures)
        // 3.fbo 和纹理关联
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameTextures[0])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            width,
            height,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            null)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0])
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            frameTextures[0],
            0)
        // 4.解除绑定
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    override fun onDraw(texture: Int, chain: FilterChain): Int {
        createFrame(chain.chainContext.width, chain.chainContext.height)
        // 摄像头纹理-》2d纹理
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]) // to fbo
        super.onDraw(texture, chain)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        return chain.proceed(frameTextures[0]) // 返回2d
    }

    override fun release() {
        super.release()
        releaseFrame()
    }

    private fun releaseFrame() {
        GLES20.glDeleteTextures(1, frameTextures, 0)
        GLES20.glDeleteFramebuffers(1, frameBuffer, 0)
    }


}