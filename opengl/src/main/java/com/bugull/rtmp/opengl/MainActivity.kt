package com.bugull.rtmp.opengl

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    var start: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        updateButton()
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.CAMERA),
                0x99)
        } else {

        }

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                0x88)
        } else {

        }
        val cameraView = findViewById<CameraView>(R.id.camera)

        findViewById<AppCompatCheckBox>(R.id.btn_start).setOnClickListener {
            if (!start) {
                start = true
                val checkedRadioButtonId = findViewById<RadioGroup>(R.id.rg_speed).checkedRadioButtonId
                cameraView.cameraRender.startRecord(when (checkedRadioButtonId) {
                    R.id.btn_extra_slow -> 0.3f
                    R.id.btn_slow -> 0.5f
                    R.id.btn_normal -> 1f
                    R.id.btn_fast -> 1.3f
                    R.id.btn_extra_fast -> 2f
                    else -> 1f
                })
            } else {
                cameraView.cameraRender.stopRecord()
                start =false
            }
            updateButton()
        }

    }

    private fun updateButton() {
        findViewById<AppCompatCheckBox>(R.id.btn_start).isChecked = start
        findViewById<AppCompatCheckBox>(R.id.btn_start).setText(if (start) "录制中" else "开始录制")
    }
}