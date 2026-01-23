package com.axon.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WearableDataService(private val context: Context) {
    private val nodeClient = Wearable.getNodeClient(context)

    private val _heartRateBpm = MutableStateFlow(0.0)
    val heartRateBpm: StateFlow<Double> = _heartRateBpm.asStateFlow()

    private val _gyroscopeX = MutableStateFlow(0f)
    val gyroscopeX: StateFlow<Float> = _gyroscopeX.asStateFlow()

    private val _gyroscopeY = MutableStateFlow(0f)
    val gyroscopeY: StateFlow<Float> = _gyroscopeY.asStateFlow()

    private val _gyroscopeZ = MutableStateFlow(0f)
    val gyroscopeZ: StateFlow<Float> = _gyroscopeZ.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    companion object {
        private const val TAG = "WearableDataService"
    }

    fun startListening() {
        // Set up callback for DataLayerListenerService
        DataLayerListenerService.setDataCallback { heartRate, gyroX, gyroY, gyroZ ->
            _heartRateBpm.value = heartRate
            _gyroscopeX.value = gyroX
            _gyroscopeY.value = gyroY
            _gyroscopeZ.value = gyroZ
            Log.d(TAG, "Updated data: HR=$heartRate, Gyro=($gyroX, $gyroY, $gyroZ)")
        }
        checkConnection()
        Log.d(TAG, "Started listening for wearable data")
    }

    fun stopListening() {
        DataLayerListenerService.removeDataCallback()
        Log.d(TAG, "Stopped listening for wearable data")
    }

    private fun checkConnection() {
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            _isConnected.value = nodes.isNotEmpty()
            Log.d(TAG, "Connected nodes: ${nodes.size}")
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to check connection", exception)
            _isConnected.value = false
        }
    }

    suspend fun requestDataSync() {
        checkConnection()
    }
}
