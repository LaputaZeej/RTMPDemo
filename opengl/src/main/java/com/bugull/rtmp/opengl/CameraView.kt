package com.bugull.rtmp.opengl

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import androidx.camera.core.CameraX
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import androidx.lifecycle.LifecycleOwner
import com.bugull.rtmp.opengl.filter.CameraFilter
import com.bugull.rtmp.opengl.filter.ScreenFilter
import com.bugull.rtmp.opengl.record.MediaRecorder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.jvm.Throws

/**
 * Author by xpl, Date on 2021/6/1.
 */
class CameraView(context: Context?, attrs: AttributeSet?) : GLSurfaceView(context, attrs) {
    val cameraRender: CameraRender

    init {

        Log.ASSERT
        setEGLContextClientVersion(2)
        cameraRender = CameraRender(this)
        setRenderer(cameraRender)
        /**
         * 刷新方式：
         *     RENDERMODE_WHEN_DIRTY 手动刷新，調用requestRender();
         *     RENDERMODE_CONTINUOUSLY 自動刷新，大概16ms自動回調一次onDraw方法
         */
        // 注意必须在setRenderer 后面。
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        super.surfaceDestroyed(holder)
        cameraRender.onSurfaceDestroyed();
    }

    /**
     * Render
     */
    class CameraRender(private val view: CameraView) : Renderer {
        private val mtx: FloatArray = FloatArray(16)
        private var textures: IntArray = intArrayOf()
        private var mSurfaceTexture: SurfaceTexture? = null
        private var mCameraFilter: CameraFilter? = null
        private var mScreenFilter: ScreenFilter? = null
        private var mMediaRecorder: MediaRecorder? = null

        init {
            // camera data
            CameraX.bindToLifecycle(view.context as LifecycleOwner, Preview(
                PreviewConfig.Builder()
                    .setTargetResolution(Size(DEFAULT_H, DEFAULT_W))
                    .setLensFacing(CameraX.LensFacing.BACK)
                    .build()
            ).apply {
                setOnPreviewOutputUpdateListener {
                    //Log.i("_opengles_", "onUpdated .... $it")
                    mSurfaceTexture = it?.surfaceTexture
                }
            })
        }

        // Opengl必须在EGL线程中调用 GLThread

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            // 创建OpenGL 纹理 ,把摄像头的数据与这个纹理关联
            // 当做能在opengl用的一个图片的ID
            textures = IntArray(1)
            mSurfaceTexture?.run {
                attachToGLContext(textures[0])
                // 当摄像头数据有更新回调 onFrameAvailable
                setOnFrameAvailableListener {
                    view.requestRender()
                    // 手动请求渲染 RENDERMODE_WHEN_DIRTY
                    //Log.i("_opengles_", "onFrameAvailable ....")

                }
                mCameraFilter = CameraFilter(view.context)
                mScreenFilter = ScreenFilter(view.context)

                mMediaRecorder = MediaRecorder(view.context,
                    "/sdcard/a-${System.currentTimeMillis()}.mp4",
                    EGL14.eglGetCurrentContext())
            }
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            mCameraFilter?.setSize(width, height)
            mScreenFilter?.setSize(width, height)
        }

        override fun onDrawFrame(gl: GL10?) {
            //Log.i("_opengles_", "onDrawFrame .... $mSurfaceTexture")
            // 更新纹理
            mSurfaceTexture?.run {
                updateTexImage()
                getTransformMatrix(mtx)
                mCameraFilter?.setTransformMatrix(mtx)
                mCameraFilter?.onDraw(textures[0])?.let { r1 ->
                    mScreenFilter?.onDraw(r1)?.let { r2 ->
                        mMediaRecorder?.fireFrame(r2, timestamp)
                    }
                }
            }
        }

        fun onSurfaceDestroyed() {
            mScreenFilter?.release()
            mCameraFilter?.release()
        }

        fun startRecord(speed: Float) {
//            try {
            mMediaRecorder?.start(speed)
//            } catch (e: Throwable) {
//                e.printStackTrace()
//            }

        }

        fun stopRecord() {
            mMediaRecorder?.stop()
        }


    }
}