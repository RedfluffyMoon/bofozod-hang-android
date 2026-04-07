package com.redfluffymoon.slapsound

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Helper class that registers an accelerometer listener and reports
 * slap events when the net acceleration exceeds [threshold].
 *
 * @param context      Android context used to get [SensorManager].
 * @param threshold    Minimum net acceleration (m/s²) to count as a slap.
 * @param cooldownMs   Minimum milliseconds between consecutive slap events.
 * @param onSlapDetected Callback invoked on the sensor thread when a slap is detected.
 */
class SensorHelper(
    context: Context,
    var threshold: Float = DEFAULT_THRESHOLD,
    var cooldownMs: Long = DEFAULT_COOLDOWN_MS,
    private val onSlapDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /** True if the accelerometer hardware is available on this device. */
    val isSensorAvailable: Boolean get() = accelerometer != null

    private var lastSlapTime = 0L
    private var isListening = false

    /**
     * Start listening to the accelerometer.
     * Safe to call when already started (no-op).
     */
    fun start() {
        if (isListening || accelerometer == null) return
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        isListening = true
    }

    /**
     * Stop listening to the accelerometer.
     * Safe to call when already stopped (no-op).
     */
    fun stop() {
        if (!isListening) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    // ── SensorEventListener ──────────────────────────────────────────────────

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate net acceleration by subtracting gravity (~9.81 m/s²)
        val magnitude = sqrt(x * x + y * y + z * z)
        val netAcceleration = magnitude - SensorManager.GRAVITY_EARTH

        if (netAcceleration >= threshold) {
            val now = System.currentTimeMillis()
            if (now - lastSlapTime >= cooldownMs) {
                lastSlapTime = now
                onSlapDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for slap detection
    }

    companion object {
        /** Default acceleration threshold for slap detection (m/s²). */
        const val DEFAULT_THRESHOLD = 15f

        /** Default cooldown between slap events (ms). */
        const val DEFAULT_COOLDOWN_MS = 750L

        /** Minimum selectable threshold on the slider (m/s²). */
        const val MIN_THRESHOLD = 5f

        /** Maximum selectable threshold on the slider (m/s²). */
        const val MAX_THRESHOLD = 40f
    }
}
