package com.example.gentlealarm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private var toneGenerator: ToneGenerator? = null

    private val handler = Handler(Looper.getMainLooper())
    private var isBlinkOn = false
    private var isRunning = false
    private var startTimeMs: Long = 0L

    private lateinit var tvStatus: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    private val blinkRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) {
                setTorch(false)
                return
            }

            val elapsed = System.currentTimeMillis() - startTimeMs
            // 5 хвилин
            if (elapsed >= 5 * 60 * 1000L) {
                stopAlarm()
                return
            }

            // Перемикаємо ліхтарик
            isBlinkOn = !isBlinkOn
            setTorch(isBlinkOn)
            if (isBlinkOn) {
                playBeep()
            }

            // Інтервал між спалахами, наприклад 500 мс
            handler.postDelayed(this, 500L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }

        btnStart.setOnClickListener {
            if (!isRunning) {
                startAlarmWithPermissionCheck()
            }
        }

        btnStop.setOnClickListener {
            stopAlarm()
        }

        updateStatus("Будильник вимкнений")
    }

    private fun startAlarmWithPermissionCheck() {
        val hasCameraPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED

        if (!hasCameraPermission) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA
            )
        } else {
            startAlarm()
        }
    }

    private fun startAlarm() {
        if (cameraId == null) {
            Toast.makeText(this, "На цьому пристрої немає спалаху", Toast.LENGTH_SHORT).show()
        }

        toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        isRunning = true
        startTimeMs = System.currentTimeMillis()
        isBlinkOn = false

        handler.post(blinkRunnable)
        updateStatus("Будильник увімкнено")
    }

    private fun stopAlarm() {
        isRunning = false
        handler.removeCallbacks(blinkRunnable)
        setTorch(false)
        toneGenerator?.release()
        toneGenerator = null
        isBlinkOn = false
        updateStatus("Будильник вимкнено")
    }

    private fun setTorch(on: Boolean) {
        val id = cameraId ?: return
        try {
            cameraManager.setTorchMode(id, on)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playBeep() {
        try {
            toneGenerator?.startTone(
                ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
                150 // тривалість піка
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateStatus(text: String) {
        tvStatus.text = text
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA) {
            val granted = grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                startAlarm()
            } else {
                Toast.makeText(
                    this,
                    "Без дозволу на камеру ліхтарик не працюватиме",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }

    companion object {
        private const val REQUEST_CAMERA = 1001
    }
}
