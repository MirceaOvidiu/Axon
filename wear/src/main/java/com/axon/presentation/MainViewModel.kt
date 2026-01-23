package com.axon.presentation

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.axon.senzors.HealthServicesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {
    private val healthServicesManager = HealthServicesManager(application)
    private val sensorManager: SensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val heartRateBpm = healthServicesManager.heartRateBpm
    val availability = healthServicesManager.availability

    private val _gyroscopeData = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val gyroscopeData = _gyroscopeData.asStateFlow()

    private val gyroscopeSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    init {
        healthServicesManager.registerForHeartRateData()
        startGyroscope()
    }

    private fun startGyroscope() {
        gyroscopeSensor?.let {
            val supported = sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            Log.d("MainViewModel", "Gyroscope sensor registration: $supported")
        }
    }

    private fun stopGyroscope() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                _gyroscopeData.value = event.values.clone()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("MainViewModel", "onAccuracyChanged: sensor = ${sensor?.name}, accuracy = $accuracy")
    }

    override fun onCleared() {
        super.onCleared()
        healthServicesManager.unregisterForHeartRateData()
        stopGyroscope()
    }
}
