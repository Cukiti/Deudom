package com.deudom.deudom.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(private val context: Context, private val onShake: () -> Unit) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastShakeTime: Long = 0
    private val SHAKE_THRESHOLD_GRAVITY = 2.7F
    private val SHAKE_SLOP_TIME_MS = 500

    fun start() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH

            // gForce will be close to 1 when there is no movement.
            val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

            if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                val now = System.currentTimeMillis()
                // ignore shake events too close to each other (500ms)
                if (lastShakeTime + SHAKE_SLOP_TIME_MS > now) {
                    return
                }

                // Reset the shake time after 3 seconds of no shakes
                if (lastShakeTime + 3000 < now) {
                    lastShakeTime = 0
                }

                lastShakeTime = now
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}
