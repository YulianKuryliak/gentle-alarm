package com.example.gentlealarm

import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var rootView: View
    private lateinit var tvStatus: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    private val handler = Handler(Looper.getMainLooper())
    private var isBlinkOn = false
    private var isRunning = false
    private var startTimeMs = 0L

    private var toneGenerator: ToneGenerator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rootView = findViewById(android.R.id.content)
        tvStatus = findViewById(R.id.tvStatus)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)

        writeLog("=== App started at ${System.currentTimeMillis()} ===")

        tvStatus.text = "Будильник вимкнений"

        btnStart.setOnClickListener {
            startAlarmSafe()
        }

        btnStop.setOnClickListener {
            stopAlarm()
        }
    }

    private fun startAlarmSafe() {
        appendLog("Start button pressed")

        try {
            startAlarm()
        } catch (e: Exception) {
            appendLog("ERROR startAlarm: ${e.message}")
            Toast.makeText(this, "Помилка старту: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startAlarm() {
        if (isRunning) return

        appendLog("Alarm started")

        toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        isRunning = true
        startTimeMs = System.currentTimeMillis()
        isBlinkOn = false
        tvStatus.text = "Будильник увімкнено"

        handler.post(blinkRunnable)
    }

    private val blinkRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return

            val elapsed = System.currentTimeMillis() - startTimeMs
            if (elapsed >= 5 * 60 * 1000) {
                appendLog("Alarm auto-stop after 5 min")
                stopAlarm()
                return
            }

            try {
                isBlinkOn = !isBlinkOn

                // Миготіння екраном (без камери)
                rootView.setBackgroundColor(if (isBlinkOn) Color.WHITE else Color.BLACK)

                // Звук
                if (isBlinkOn) {
                    toneGenerator?.startTone(
                        ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
                        150
                    )
                }

            } catch (e: Exception) {
                appendLog("Blink ERROR: ${e.message}")
                stopAlarm()
            }

            handler.postDelayed(this, 500)
        }
    }

    private fun stopAlarm() {
        if (!isRunning) return

        appendLog("Alarm stopped")

        isRunning = false
        handler.removeCallbacks(blinkRunnable)

        // Відновити колір
        rootView.setBackgroundColor(Color.BLACK)

        toneGenerator?.release()
        toneGenerator = null

        tvStatus.text = "Будильник вимкнений"
    }

    // -----------------------------
    //       Л О Г У В А Н Н Я
    // -----------------------------

    private fun writeLog(content: String) {
        try {
            val file = File(filesDir, "last_log.txt")
            file.writeText(content + "\n")
        } catch (_: Exception) {}
    }

    private fun appendLog(line: String) {
        try {
            val file = File(filesDir, "last_log.txt")
            file.appendText(line + "\n")
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        appendLog("App destroyed")
        stopAlarm()
        super.onDestroy()
    }
}
