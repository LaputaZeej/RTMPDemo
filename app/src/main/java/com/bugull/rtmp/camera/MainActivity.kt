package com.bugull.rtmp.camera

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.bugull.rtmp.common.PathFragment

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportFragmentManager.beginTransaction().replace(R.id.frame_content, PathFragment(
            PERMISSIONS, this::onPathClick), "F").commit()

        findViewById<View>(R.id.action).setOnClickListener {
            Toast.makeText(this, "1111", Toast.LENGTH_SHORT).show()

        }
    }

    private fun onPathClick(it: PathFragment.PathData) {
        CameraActivity.skip(this, it.value)
    }



    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }

        private val PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
        )
    }
}