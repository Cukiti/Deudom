package com.deudom.deudom.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class ProximityManager(context: Context, private val onCovered: () -> Unit) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    fun start() {
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            // If distance is close (usually < 5cm or just 0 for some binary sensors)
            if (it.values[0] < it.sensor.maximumRange && it.values[0] < 5f) {
                onCovered()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
