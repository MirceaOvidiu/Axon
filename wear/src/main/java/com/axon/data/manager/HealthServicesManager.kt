package com.axon.data.manager

import android.content.Context
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.MeasureClient
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.getCapabilities
import androidx.health.services.client.unregisterMeasureCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HealthServicesManager(
    context: Context,
) {
    companion object {
        private const val TAG = "HealthServicesManager"
    }

    private val healthServicesClient = HealthServices.getClient(context)
    private val measureClient: MeasureClient = healthServicesClient.measureClient
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _heartRateBpm = MutableStateFlow(0.0)
    val heartRateBpm: StateFlow<Double> = _heartRateBpm

    private val _availability = MutableStateFlow<Availability?>(null)
    val availability: StateFlow<Availability?> = _availability

    private var isRegistered = false

    private val measureCallback =
        object : MeasureCallback {
            override fun onAvailabilityChanged(
                dataType: DeltaDataType<*, *>,
                availability: Availability,
            ) {
                _availability.value = availability
                Log.d(TAG, "Availability changed: $availability")
            }

            override fun onDataReceived(data: DataPointContainer) {
                val heartRateData = data.getData(DataType.HEART_RATE_BPM)
                Log.d(TAG, "onDataReceived called, heartRateData size: ${heartRateData.size}")
                if (heartRateData.isNotEmpty()) {
                    val bpm = heartRateData.last().value
                    Log.d(TAG, "Heart rate received: $bpm BPM")
                    _heartRateBpm.value = bpm
                }
            }
        }

    fun registerForHeartRateData() {
        if (isRegistered) {
            Log.d(TAG, "Already registered for heart rate data")
            return
        }

        Log.d(TAG, "Starting registration process...")

        scope.launch {
            try {
                Log.d(TAG, "Checking device capabilities...")
                // Check if heart rate measurement is supported
                val capabilities = measureClient.getCapabilities()
                val supportsHeartRate = DataType.HEART_RATE_BPM in capabilities.supportedDataTypesMeasure

                Log.d(TAG, "Heart rate measurement supported: $supportsHeartRate")
                Log.d(TAG, "Supported data types: ${capabilities.supportedDataTypesMeasure}")

                if (supportsHeartRate) {
                    Log.d(TAG, "Registering callback for heart rate data...")
                    measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, measureCallback)
                    isRegistered = true
                    Log.d(TAG, "Successfully registered for heart rate data")
                } else {
                    Log.e(TAG, "Heart rate measurement is NOT supported on this device")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register for heart rate data: ${e.message}", e)
            }
        }
    }

    suspend fun unregisterForHeartRateData() {
        if (!isRegistered) {
            Log.d(TAG, "Not registered, skipping unregister")
            return
        }

        try {
            Log.d(TAG, "Unregistering for heart rate data")
            measureClient.unregisterMeasureCallback(DataType.HEART_RATE_BPM, measureCallback)
            isRegistered = false
            Log.d(TAG, "Successfully unregistered for heart rate data")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister for heart rate data: ${e.message}", e)
        }
    }
}
