package com.calendrier.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class TapDetectorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // Triple tap detection
    private val tapTimestamps = mutableListOf<Long>()
    private var lastTapTime = 0L
    private val TAP_THRESHOLD = 2.8f       // g-force spike to count as tap
    private val TAP_WINDOW_MS = 1200L      // 3 taps must happen within this window
    private val TAP_COOLDOWN_MS = 150L     // ignore spikes within same tap
    private var isGravityBaseline = false
    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = 0f

    // Browser state
    private var browserVisible = false

    companion object {
        const val CHANNEL_ID = "sync_channel"
        const val NOTIF_ID = 1
        var instance: TapDetectorService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // High-pass filter to remove gravity
        val alpha = 0.8f
        gravityX = alpha * gravityX + (1 - alpha) * x
        gravityY = alpha * gravityY + (1 - alpha) * y
        gravityZ = alpha * gravityZ + (1 - alpha) * z

        val linX = x - gravityX
        val linY = y - gravityY
        val linZ = z - gravityZ

        val magnitude = sqrt(linX * linX + linY * linY + linZ * linZ)

        val now = System.currentTimeMillis()

        if (magnitude > TAP_THRESHOLD * 9.8f && (now - lastTapTime) > TAP_COOLDOWN_MS) {
            lastTapTime = now

            // Add tap, clean old ones
            tapTimestamps.add(now)
            tapTimestamps.removeAll { now - it > TAP_WINDOW_MS }

            if (tapTimestamps.size >= 3) {
                tapTimestamps.clear()
                onTripleTapDetected()
            }
        }
    }

    private fun onTripleTapDetected() {
        if (browserVisible) {
            hideBrowser()
        } else {
            showBrowser()
        }
    }

    private fun showBrowser() {
        browserVisible = true
        val intent = Intent(this, BrowserActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun hideBrowser() {
        browserVisible = false
        // Hide via the activity reference if available
        // The activity handles hiding itself via moveTaskToBack
        // We send a broadcast to BrowserActivity
        sendBroadcast(Intent("com.calendrier.HIDE_BROWSER"))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Synchronisation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Service de synchronisation"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Calendrier")
            .setContentText("Synchronisation active")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        instance = null
    }
}
