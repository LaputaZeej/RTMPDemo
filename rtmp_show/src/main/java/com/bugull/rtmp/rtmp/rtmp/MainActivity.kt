package com.bugull.rtmp.rtmp.rtmp

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bugull.rtmp.common.PathActivity

class MainActivity : PathActivity(REQUIRED_PERMISSIONS) {
    override fun onPathClick(it: PathData) {

        if (allPermissionsGranted()) {
            PlayActivity.skip(this@MainActivity, it.value)
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, 0x99
            )
        }

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}