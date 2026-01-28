package com.axon.senzors

import android.content.Context
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.unregisterMeasureCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HealthServicesManager(context: Context) {
    private val healthServicesClient = HealthServices.getClient(context)
    private val measureClient = healthServicesClient.measureClient

    private val _heartRateBpm = MutableStateFlow(0.0)
    val heartRateBpm: StateFlow<Double> = _heartRateBpm

    private val _availability = MutableStateFlow<Availability?>(null)
    val availability: StateFlow<Availability?> = _availability

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

    suspend fun unregisterForHeartRateData() {
        Log.d("HealthServicesManager", "Unregistering for heart rate data")
        measureClient.unregisterMeasureCallback(DataType.HEART_RATE_BPM, measureCallback)
    }
}
