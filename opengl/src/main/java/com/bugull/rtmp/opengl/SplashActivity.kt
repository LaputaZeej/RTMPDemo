package com.bugull.rtmp.opengl

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.bugull.rtmp.opengl.face.FaceTracker

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        //checkPermission()

        findViewById<View>(R.id.skip).setOnClickListener {
            //checkPermission()
            startActivity(Intent(this, MainActivity::class.java))
        }

        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE),
            0x0a)

        copy()

    }

    private fun copy() {
        Thread {
            copyAssets2Sdcard(
                this, "lbpcascade_frontalface.xml",
                this.applicationContext.path_face_01)
            copyAssets2Sdcard(
                this,
                "pd_2_00_pts5.dat", this.applicationContext.path_face_02
            )
        }.start()

    }

    private fun checkPermission() {
        var success = true
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                0x0a)
            success = false
        }
        if (success) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            Toast.makeText(this, "检查权限", Toast.LENGTH_SHORT).show()
        }
    }
}