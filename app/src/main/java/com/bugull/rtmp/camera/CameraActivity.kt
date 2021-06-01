package com.bugull.rtmp.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var fos: FileOutputStream? = null

    private val rtmpClient: RTMPClient = RTMPClient(this) { connect, state ->
        runOnUiThread {
            changeState(connect, state)
        }
    }

    private fun changeState(connect: Boolean, state: ConnectState = ConnectState.Connnecting) {
        RTMPClient.logger("changeState $connect ${state.text}")
        connect_state.visibility = if (connect) View.VISIBLE else View.GONE
        connect_state.text = state.text
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        val url = intent?.extras?.getString(URL, "") ?: BuildConfig.rtmp
        findViewById<TextView>(R.id.tv_url).text = "url : \n $url "
        changeState(false)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Set up the listener for take photo button
        camera_capture_button.setOnClickListener { /*takePhoto()*/
            rtmpClient.startLive(url)
        }

        connect_state.setOnClickListener { /*takePhoto()*/
            rtmpClient.stopLive()
        }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
        val path = this.applicationContext.filesDir.absoluteFile.absolutePath + "/e.yuv"
        Log.i(TAG, "path $path ")
        try {
            fos = FileOutputStream(path)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        Log.i(TAG, "fos $fos ")

        rtmpClient.initVideo(480, 640, 10, 640_000)
        rtmpClient.initAudio(44100, 2)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults:
        IntArray,
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            })
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                //.setTargetResolution(Size(640,480)) // 并不是最终的分辨率
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider) // 设置预览
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                // .setTargetResolution(Size(1024,768))
                .setBackgroundExecutor(Executors.newSingleThreadExecutor())
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer(rtmpClient, fos) { luma ->
                        Log.d(TAG, "Average luminosity: $luma")
                    })
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        rtmpClient.stopLive()
        rtmpClient.release()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        private const val URL = "url"

        @JvmStatic
        fun skip(activity: FragmentActivity, url: String) {
            activity.startActivity(Intent(activity, CameraActivity::class.java).apply {
                putExtras(bundleOf(URL to url))
            })
        }
    }


    private class LuminosityAnalyzer(
        private val rtmpClient: RTMPClient,
        private val fos: FileOutputStream?,
        private val listener: (Double) -> Unit,
    ) :
        ImageAnalysis.Analyzer {


        override fun analyze(image: ImageProxy) {
            try {
                val parseData = ImageUtils.getBytes2(image, rtmpClient.width, rtmpClient.height)
                rtmpClient.sendVideo(parseData)
            } catch (e: Throwable) {
                e.printStackTrace()
            }


            /*          // todo 处理数据
                      // YUV420
                      Log.i(TAG, "${image.width}*${image.height}")

                      try {
                          val parseData = parseData(image)
                          *//*   val parseData = ImageUtils.getBytes(image,
                       image.imageInfo.rotationDegrees,
                       image.width,
                       image.height)*//*
                Log.i(TAG, "parseData ${parseData.size}")
                fos?.also {
                    it.write(parseData)
//                it.flush()
//                it.close()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }*/


            /*     val buffer = image.planes[0].buffer
                 val data = buffer.toByteArray()
                 val pixels = data.map { it.toInt() and 0xFF }
                 val luma = pixels.average()

                 listener(luma)*/

            image.close()
        }

        private lateinit var i420: ByteBuffer

        private fun parseData(image: ImageProxy): ByteArray {
            val size = image.width * image.height * 3 / 2
//            while (!this::i420.isInitialized) {
//                i420 = ByteBuffer.allocate(size)
//            }
//            if (i420.capacity()<size){
//                i420 = ByteBuffer.allocate(size)
//            }
            if (!this::i420.isInitialized) {
                synchronized(this) {
                    if (!this::i420.isInitialized || i420.capacity() < size) {
                        i420 = ByteBuffer.allocate(size)
                    }
                }
            }
            i420.position(0)
            try {
                Log.i(TAG, "parseData")
                val format = image.format
                if (format != ImageFormat.YUV_420_888) {
                    return byteArrayOf()
                }
                // 原始：YUV420 [NV21]
                // Y Y Y Y
                // Y Y Y Y
                // Y Y Y Y
                // Y Y Y Y
                // U U U U
                // U U V V
                // 目标：[I420]
                // Y Y Y Y
                // Y Y Y Y
                // Y Y Y Y
                // Y Y Y Y
                // U U
                // U U
                // V V
                // V V
                // y数据的stride(行内数据间隔)只会是1 val yStride = image.planes[0].pixelStride

                fillY(image, i420)
                fillU(image, i420)
                fillV(image, i420)
                val rotationDegrees = image.imageInfo.rotationDegrees
                Log.i(TAG, "rotationDegrees = $rotationDegrees")
                if (rotationDegrees == 90 || rotationDegrees == 270) {
                    // todo 旋转
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            return i420.toByteArray()
        }

        private fun fillY(image: ImageProxy, i420: ByteBuffer) {
            val buffer = image.planes[0].buffer
            val rowStride = image.planes[0].rowStride
            // rowSkip>with 每行多出来的数组
            // rowSkip=with 空数组
            val rowSkip = ByteArray(rowStride - image.width)
            val row = ByteArray(image.width)
            (0 until image.height).forEach {
                buffer.get(row)
                i420.put(row)
                // 最后一行不需要丢弃
                if (it < image.height - 1) {
                    buffer.get(rowSkip)
                }
            }
        }

        private fun fillU(image: ImageProxy, i420: ByteBuffer) {
            fillUV(image, i420, 1)
        }

        private fun fillV(image: ImageProxy, i420: ByteBuffer) {
            fillUV(image, i420, 2)
        }

        private fun fillUV(image: ImageProxy, i420: ByteBuffer, index: Int) {
            val buffer = image.planes[index].buffer
            // 大于 等于
            val rowStride = image.planes[index].rowStride
            // 混合 不混合
            val pixelStride = image.planes[index].pixelStride
            val width = image.width / 2
            val height = image.height / 2
            // 每行
            (0 until height).forEach { j ->
                // 一次处理一个字节
                (0 until rowStride).forEachIndexed() { k, _ ->

                    if (j == height - 1) {

                        if (pixelStride == 1) {
                            // 如果是不混在一起且最后一行，且大于宽
                            if (k >= width) {
                                return@forEachIndexed;
                            }
                        } else if (pixelStride == 2) {
                            // 如果是混在一起且最后一行，且大于宽
                            // UVUV
                            //
                            if (k >= image.width - 1) {
                                return@forEachIndexed;
                            }
                        }
                    }
                    val per = buffer.get()
                    if (pixelStride == 1) {
                        // uv没有混合在一起
                        if (k < width) {
                            i420.put(per)
                        }
                    } else if (pixelStride == 2) {
                        // uv混合在一起
                        if (k % 2 == 0 && k < image.width) {
                            // 1.偶数位下标2.小于宽
                            i420.put(per)
                        }
                    }
                }
            }
        }

        private fun getByteArray(image: ImageProxy, index: Int): ByteArray {
            val planeProxy: ImageProxy.PlaneProxy = image.planes[index]
            return when (planeProxy.pixelStride) {
                1 -> planeProxy.buffer.toByteArray()
                2 -> ByteArray(image.width / 2 * image.height / 2)
                    .also { temp ->
                        val vBuffer = planeProxy.buffer
                        (0 until vBuffer.remaining()).forEach { i ->
                            if (i % 2 == 0) {
                                temp[i] = vBuffer.get()
                                vBuffer.get()// 丢弃一个数据，这个数据其实是U数据
                            }
                        }
                    }

                else -> byteArrayOf()
            }

        }
    }


}


fun ByteBuffer.toByteArray(): ByteArray {
    rewind()    // Rewind the buffer to zero
    val data = ByteArray(remaining())
    get(data)   // Copy the buffer into a byte array
    return data // Return the byte array
}