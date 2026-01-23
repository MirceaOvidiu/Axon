package com.axon.presentation

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.axon.data.WearableDataSender
import com.axon.senzors.HealthServicesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {
    private val healthServicesManager = HealthServicesManager(application)
    private val sensorManager: SensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val wearableDataSender = WearableDataSender(application)

    val heartRateBpm = healthServicesManager.heartRateBpm
    val availability = healthServicesManager.availability

    private val _gyroscopeData = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val gyroscopeData = _gyroscopeData.asStateFlow()

    private val gyroscopeSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    private var lastSendTime = 0L
    private val sendInterval = 500L // Send data every 500ms

    init {
        healthServicesManager.registerForHeartRateData()
        startGyroscope()

        // Start monitoring data changes to send to phone
        viewModelScope.launch {
            heartRateBpm.collect { heartRate ->
                sendDataToPhone(heartRate)
            }
        }
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
                // Send data to phone periodically
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastSendTime > sendInterval) {
                    sendDataToPhone(heartRateBpm.value)
                    lastSendTime = currentTime
                }
            }
        }
    }

    private fun sendDataToPhone(heartRate: Double) {
        viewModelScope.launch {
            val gyro = _gyroscopeData.value
            wearableDataSender.sendSensorData(
                heartRate = heartRate,
                gyroX = gyro[0],
                gyroY = gyro[1],
                gyroZ = gyro[2]
            )
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
