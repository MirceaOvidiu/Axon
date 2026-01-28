package com.axon.senzors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HealthServicesManager(context: Context) : SensorEventListener {
    private val healthServicesClient = HealthServices.getClient(context)
    private val measureClient = healthServicesClient.measureClient
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _heartRateBpm = MutableStateFlow(0.0)
    val heartRateBpm: StateFlow<Double> = _heartRateBpm

    private val _skinTemperature = MutableStateFlow<Double?>(null)
    val skinTemperature: StateFlow<Double?> = _skinTemperature

    private val _availability = MutableStateFlow<Availability?>(null)
    val availability: StateFlow<Availability?> = _availability

    private val _skinTemperatureAvailable = MutableStateFlow(false)
    val skinTemperatureAvailable: StateFlow<Boolean> = _skinTemperatureAvailable

    // Skin temperature sensor (TYPE_SKIN_TEMPERATURE = 65540 in some devices, use Sensor.TYPE_AMBIENT_TEMPERATURE as fallback)
    private val skinTemperatureSensor: Sensor? by lazy {
        // Try to find skin temperature sensor (vendor-specific on many devices)
        sensorManager.getSensorList(Sensor.TYPE_ALL).find { sensor ->
            sensor.name.contains("skin", ignoreCase = true) &&
            sensor.name.contains("temp", ignoreCase = true)
        } ?: sensorManager.getDefaultSensor(65540) // Some devices use this type code
    }

    private val measureCallback = object : MeasureCallback {
        override fun onAvailabilityChanged(
            dataType: DeltaDataType<*, *>,
            availability: Availability
        ) {
            _availability.value = availability
            Log.d("HealthServicesManager", "Availability changed: $availability")
        }

        override fun onDataReceived(data: DataPointContainer) {
            val heartRateData = data.getData(DataType.HEART_RATE_BPM)
            if (heartRateData.isNotEmpty()) {
                _heartRateBpm.value = heartRateData.last().value
                Log.d("HealthServicesManager", "Heart rate: ${heartRateData.last().value}")
            }
        }
    }

    fun registerForHeartRateData() {
        Log.d("HealthServicesManager", "Registering for heart rate data")
        measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, measureCallback)
    }

    fun unregisterForHeartRateData() {
        Log.d("HealthServicesManager", "Unregistering for heart rate data")
        measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, measureCallback)
    }

    fun startSkinTemperatureMonitoring() {
        skinTemperatureSensor?.let { sensor ->
            val supported = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            _skinTemperatureAvailable.value = supported
            Log.d("HealthServicesManager", "Skin temperature sensor registration: $supported")
        } ?: run {
            _skinTemperatureAvailable.value = false
            Log.d("HealthServicesManager", "Skin temperature sensor not available on this device")
        }
    }

    fun stopSkinTemperatureMonitoring() {
        sensorManager.unregisterListener(this)
        Log.d("HealthServicesManager", "Skin temperature monitoring stopped")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        // Skin temperature sensor typically reports in Celsius
        if (event.values.isNotEmpty()) {
            _skinTemperature.value = event.values[0].toDouble()
            Log.d("HealthServicesManager", "Skin temperature: ${event.values[0]}")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("HealthServicesManager", "Sensor accuracy changed: ${sensor?.name}, accuracy: $accuracy")
    }
}
