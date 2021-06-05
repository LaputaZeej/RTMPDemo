package com.bugull.rtmp.opengl

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.HandlerThread
import android.system.Os.bind
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import androidx.camera.core.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bugull.rtmp.opengl.face.Face
import com.bugull.rtmp.opengl.face.FaceTracker
import com.bugull.rtmp.opengl.filter.BigEyeFilter
import com.bugull.rtmp.opengl.filter.CameraFilter
import com.bugull.rtmp.opengl.filter.FilterChain
import com.bugull.rtmp.opengl.filter.ScreenFilter
import com.bugull.rtmp.opengl.record.MediaRecorder
import com.bugull.rtmp.opengl.util.ImageUtils
import java.util.concurrent.Executors
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
    class CameraRender(private val view: CameraView) : Renderer, LifecycleEventObserver {
        private val mtx: FloatArray = FloatArray(16)
        private var textures: IntArray = intArrayOf()
        private var mSurfaceTexture: SurfaceTexture? = null
        private var mMediaRecorder: MediaRecorder? = null
        private var mFilterChain: FilterChain? = null

        private val handlerThread = HandlerThread("ImageAnalysis")
        private var mFaceTracker: FaceTracker? = null
        private val mFaceTrackerLock = Any()
        private var mFace: Face? = null

        init {
            (view.context as LifecycleOwner).lifecycle.addObserver(this)
            handlerThread.start()
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
            },
                ImageAnalysis(
                    ImageAnalysisConfig.Builder()
                        .setCallbackHandler(Handler(handlerThread.looper))
                        .setLensFacing(CameraX.LensFacing.BACK)
                        .setTargetResolution(Size(DEFAULT_H, DEFAULT_W))
                        .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                        .build()
                ).also {
                    it.setAnalyzer { imageProxy, rotationDegrees ->
                        mFaceTracker?.let { ft ->
                            val bytes = ImageUtils.getBytes(imageProxy)
                            synchronized(mFaceTrackerLock) {
                                // 更新人脸眼睛信息
                                val face = ft.detect(bytes,
                                    imageProxy.width,
                                    imageProxy.height,
                                    rotationDegrees)
                                mFace = face
                                Log.i("_opengles_", "setAnalyzer ....$face")

                            }
                        }
                    }

                }
            )
        }

        var callback: ((Throwable) -> Unit)? = null

        // Opengl必须在EGL线程中调用 GLThread

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            Log.i("_opengles_", "onFrameAvailable ....")
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

                val fcc = FilterChain.FilterChainContext()
                mFilterChain = FilterChain(listOf(
                    CameraFilter(view.context),
                    BigEyeFilter(view.context),
                    ScreenFilter(view.context)
                ), 0, FilterChain.FilterChainContext())

                mMediaRecorder = MediaRecorder(
                    view.context,
                    "/sdcard/a-${System.currentTimeMillis()}.mp4",
                    EGL14.eglGetCurrentContext()
                )
            }
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            mFilterChain?.setSize(width, height)
        }

        override fun onDrawFrame(gl: GL10?) {
            //Log.i("_opengles_", "onDrawFrame .... $mSurfaceTexture")
            // 更新纹理
            mSurfaceTexture?.run {
                updateTexImage()
                getTransformMatrix(mtx)
                mFilterChain?.setTransformMatrix(mtx)
                mFilterChain?.let {
                    synchronized(mFaceTrackerLock) {
                        it.setFace(mFace)
                    }
                }
                val r = mFilterChain?.proceed(textures[0])
                r?.run {
                    mMediaRecorder?.fireFrame(this, timestamp)
                }
            }
        }

        fun onSurfaceDestroyed() {
            mFilterChain?.release()
        }

        fun startRecord(speed: Float) {
            try {
                mMediaRecorder?.start(speed)
            } catch (e: Throwable) {
                e.printStackTrace()
                callback?.invoke(e)
            }
        }

        fun stopRecord() {
            mMediaRecorder?.stop()
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    // todo 需要从assets 复制到 手机存储内存
                    mFaceTracker = FaceTracker("/sdcard/lbpcascade_frontalface.xml",
                        "/sdcard/pd_2_00_pts5.dat")
                }

                Lifecycle.Event.ON_START -> {
                    mFaceTracker?.start()
                }

                Lifecycle.Event.ON_STOP -> {
                    mFaceTracker?.stop()
                }

                Lifecycle.Event.ON_DESTROY -> {
                    mFaceTracker?.release()
                    (view.context as LifecycleOwner).lifecycle.removeObserver(this)
                }
            }
        }


    }
}