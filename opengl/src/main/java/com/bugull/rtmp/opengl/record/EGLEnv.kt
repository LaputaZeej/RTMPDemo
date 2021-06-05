package com.bugull.rtmp.opengl.record

import android.content.Context
import android.opengl.*
import android.util.Log
import android.view.Surface
import com.bugull.rtmp.opengl.filter.FilterChain
import com.bugull.rtmp.opengl.filter.RecordFilter

/**
 * Author by xpl, Date on 2021/6/3.
 */
class EGLEnv(
    val context: Context,
    mGlContext: EGLContext, // 摄像头的上下文
    surface: Surface,
     val width: Int,
    val height: Int,
) {
    private val mEglDisplay: EGLDisplay // 窗口
    private val mEglSurface: EGLSurface // 画布
    private val mEglConfig: EGLConfig // 配置
    private val mEglContext: EGLContext // 上下文 一个是编码的上下文
    private val recordFilter: RecordFilter
    private val filterChain: FilterChain

    init {
        // 获得显示窗口，作为OpenGL的绘制目标
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw  RuntimeException("eglGetDisplay failed");
        }

        // 初始化顯示窗口
        val version = IntArray(2)
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            throw  RuntimeException("eglInitialize failed");
        }

        // 配置 属性选项
        val configAttribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,  //颜色缓冲区中红色位数
            EGL14.EGL_GREEN_SIZE, 8,  //颜色缓冲区中绿色位数
            EGL14.EGL_BLUE_SIZE, 8,  //
            EGL14.EGL_ALPHA_SIZE, 8,  //
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,  //opengl es 2.0
            EGL14.EGL_NONE
        )
        val numConfigs = IntArray(1)
        val configs: Array<EGLConfig?> = arrayOfNulls<EGLConfig>(1)
        //EGL 根据属性选择一个配置
        if (!EGL14.eglChooseConfig(mEglDisplay, configAttribs, 0, configs, 0, configs.size,
                numConfigs, 0)
        ) {
            throw  RuntimeException("EGL error " + EGL14.eglGetError());
        }
        mEglConfig = configs[0]!!
        /**
         * EGL上下文
         */
        val context_attrib_list = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        //mGlContext：與 GLSurfaceView中的EGLContext 共享數據，只有這樣才能拿到處理完之後顯示的图像纹理。
        mEglContext =
            EGL14.eglCreateContext(mEglDisplay, mEglConfig, mGlContext, context_attrib_list, 0)

        if (mEglContext === android.opengl.EGL14.EGL_NO_CONTEXT) {
            throw java.lang.RuntimeException("EGL error " + EGL14.eglGetError())
        }

        /**
         * 创建EGLSurface
         */
        val surface_attrib_list = intArrayOf(
            EGL14.EGL_NONE
        )
        mEglSurface =
            EGL14.eglCreateWindowSurface(mEglDisplay,
                mEglConfig, /*来自MediaCodec的surface*/
                surface,
                surface_attrib_list,
                0)
        // mEglSurface == null
        // mEglSurface == null
        if (mEglSurface == null) {
            throw java.lang.RuntimeException("EGL error " + EGL14.eglGetError())
        }
        /**
         * 绑定当前线程的显示器display
         */
        if (!EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw  RuntimeException("EGL error " + EGL14.eglGetError());
        }
        recordFilter = RecordFilter(context)
        val s = FilterChain.FilterChainContext().apply {
            setSize(this@EGLEnv.width, this@EGLEnv.height)
        }
        filterChain = FilterChain(listOf(), 0, s)

    }

    // 往MediaCodec surface填充数据
    fun draw(texture: Int, timestamp: Long) {
        recordFilter.onDraw(texture, filterChain)
        EGLExt.eglPresentationTimeANDROID(mEglDisplay, mEglSurface, timestamp);
        //EGLSurface是双缓冲模式
        EGL14.eglSwapBuffers(mEglDisplay, mEglSurface);
    }

    fun release() {
        EGL14.eglDestroySurface(mEglDisplay, mEglSurface)
        EGL14.eglMakeCurrent(mEglDisplay,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroyContext(mEglDisplay, mEglContext)
        EGL14.eglReleaseThread()
        EGL14.eglTerminate(mEglDisplay)
        recordFilter.release()
    }
}