package com.axon.senzors


import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SensorViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val sensorManager: SensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // --- Gyroscope Sensor ---
    private val _gyroscopeData = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val gyroscopeData = _gyroscopeData.asStateFlow()
    private val gyroscopeSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    // --- Heart Rate (BPM) Sensor ---
    private val _heartRateData = MutableStateFlow(0f)
    val heartRateData = _heartRateData.asStateFlow()
    private val heartRateSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    }

    // Note: There is no standard Sensor.TYPE_EDA for Electrodermal Activity.
    // Accessing EDA data often requires vendor-specific libraries or Health Services API.

    init {
        registerListeners()
    }

    private fun registerListeners() {
        gyroscopeSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        heartRateSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun unregisterListeners() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        viewModelScope.launch {
            when (event.sensor.type) {
                Sensor.TYPE_GYROSCOPE -> {
                    // event.values contains x, y, and z rotational speed
                    _gyroscopeData.value = event.values.clone()
                }
                Sensor.TYPE_HEART_RATE -> {
                    // event.values[0] contains the heart rate in beats per minute
                    _heartRateData.value = event.values[0]
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // You can use this to inform the user about sensor accuracy changes.
        // For example, if accuracy is SensorManager.SENSOR_STATUS_UNRELIABLE
    }

    override fun onCleared() {
        super.onCleared()
        unregisterListeners()
    }
}