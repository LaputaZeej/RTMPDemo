package com.bugull.rtmp.rtmp.rtmp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bugull.rtmp.common.PathFragment

class MainActivity : FragmentActivity() {
    private val saveRtmp = RTMPHelper()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportFragmentManager.beginTransaction().replace(R.id.frame_content, PathFragment(
            REQUIRED_PERMISSIONS, this::onPathClick), "F").commit()

        findViewById<View>(R.id.url).setOnClickListener {
            Thread {
                saveRtmp.saveRtmp("rtmp://192.168.199.144/laputa/abcd",
                    applicationContext.filesDir.absolutePath + "/nice.flv")
            }.start()

        }

        findViewById<View>(R.id.send).setOnClickListener {
            Thread {
                saveRtmp.sendRtmp("rtmp://192.168.199.144/laputa/abcd",
                    applicationContext.filesDir.absolutePath + "/nice02.flv")
            }.start()

        }

        findViewById<View>(R.id.send_h264).setOnClickListener {
            Thread {
                saveRtmp.sendRtmpH264("rtmp://192.168.199.144/laputa/abcd",
                    applicationContext.filesDir.absolutePath + "/nice01.h264")
            }.start()

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        saveRtmp.close()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun onPathClick(it: PathFragment.PathData) {
        if (allPermissionsGranted()) {
            PlayActivity.skip(this@MainActivity, it.value)
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, 0x99
            )
        }
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}