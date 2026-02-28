package com.axon.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GyroDataSource(
    context: Context,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _values = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val values: StateFlow<FloatArray> = _values

    private val samplingInterval = 20_000

    fun start() {
        gyroscope?.let {
            sensorManager.registerListener(this, it, samplingInterval)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        scope.cancel()
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) = Unit

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val copy = event.values.clone()
            scope.launch { _values.emit(copy) }
        }
    }
}
