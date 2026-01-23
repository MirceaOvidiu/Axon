package com.axon.senzors

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SensorViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val sensorManager: SensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _gyroscopeData = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val gyroscopeData = _gyroscopeData.asStateFlow()

    private val _heartRateData = MutableStateFlow(0f)
    val heartRateData = _heartRateData.asStateFlow()

    private val gyroscopeSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    private val heartRateSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    }

    private var lastLogTime = 0L

    fun startListening(heartRatePermissionGranted: Boolean) {
        Log.d("SensorViewModel", "startListening: heartRatePermissionGranted = $heartRatePermissionGranted")
        
        // Unregister first to ensure a clean state
        sensorManager.unregisterListener(this)
        
        // Always register gyroscope
        gyroscopeSensor?.let {
            val supported = sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            Log.d("SensorViewModel", "Gyroscope sensor registration: $supported")
        }

        // Only register heart rate if we have permission
        if (heartRatePermissionGranted) {
            heartRateSensor?.let {
                val supported = sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                Log.d("SensorViewModel", "Heart rate sensor registration: $supported")
            } ?: Log.d("SensorViewModel", "Heart rate sensor NOT found on this device")
        } else {
            Log.w("SensorViewModel", "Heart rate permission NOT granted, skipping registration")
        }
    }

    fun stopListening() {
        Log.d("SensorViewModel", "stopListening")
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                _gyroscopeData.value = event.values.clone()
            }
            Sensor.TYPE_HEART_RATE -> {
                if (event.values.isNotEmpty()) {
                    val rate = event.values[0]
                    _heartRateData.value = rate
                    
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastLogTime > 1000) {
                        Log.d("SensorViewModel", "BPM Update: $rate")
                        lastLogTime = currentTime
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("SensorViewModel", "onAccuracyChanged: sensor = ${sensor?.name}, accuracy = $accuracy")
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}