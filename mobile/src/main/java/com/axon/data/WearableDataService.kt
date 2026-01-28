package com.axon.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class WearableDataService(private val context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val nodeClient = Wearable.getNodeClient(context)

    private val _connectedNodeId = MutableStateFlow<String?>(null)
    val connectedNodeId: StateFlow<String?> = _connectedNodeId.asStateFlow()

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

    private var dataCollectionJob: Job? = null

    init {
        startListening()
        fetchCurrentData()
        fetchConnectedNode()
    }

    companion object {
        private const val TAG = "WearableDataService"
    }

    fun startListening() {
        // Cancel any existing collection job
        dataCollectionJob?.cancel()

        // Collect sensor data events from SharedFlow
        dataCollectionJob = serviceScope.launch {
            DataLayerEvents.sensorDataEvents.collect { event ->
                _heartRateBpm.value = event.heartRate
                _gyroscopeX.value = event.gyroX
                _gyroscopeY.value = event.gyroY
                _gyroscopeZ.value = event.gyroZ
                Log.d(TAG, "SharedFlow updated data: HR=${event.heartRate}, Gyro=(${event.gyroX}, ${event.gyroY}, ${event.gyroZ})")
            }
        }
        Log.d(TAG, "★★★ Started collecting sensor data events via SharedFlow")

        checkConnection()
        fetchCurrentData()
        Log.d(TAG, "Started listening for wearable data")
    }

    private fun fetchCurrentData() {
        Wearable.getDataClient(context).dataItems
            .addOnSuccessListener { dataItems ->
                Log.d(TAG, "fetchCurrentData found ${dataItems.count} items")
                dataItems.forEach { item ->
                    val path = item.uri.path
                    Log.d(TAG, "Checking item: $path")
                    if (path?.startsWith(DataLayerListenerService.SENSOR_DATA_PATH) == true) {
                        com.google.android.gms.wearable.DataMapItem.fromDataItem(item).dataMap.apply {
                            val heartRate = getDouble("heart_rate", 0.0)
                            val gyroX = getFloat("gyro_x", 0f)
                            val gyroY = getFloat("gyro_y", 0f)
                            val gyroZ = getFloat("gyro_z", 0f)

                            _heartRateBpm.value = heartRate
                            _gyroscopeX.value = gyroX
                            _gyroscopeY.value = gyroY
                            _gyroscopeZ.value = gyroZ
                            Log.d(TAG, "Fetched initial data: HR=$heartRate, Gyro=($gyroX, $gyroY, $gyroZ)")
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch initial data", e)
            }
    }

    private fun fetchConnectedNode() {
        serviceScope.launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                _connectedNodeId.value = nodes.firstOrNull()?.id
                Log.d("WearableDataService", "Connected nodes: ${nodes.size}")
            } catch (e: Exception) {
                Log.e("WearableDataService", "Error fetching connected nodes", e)
            }
        }
    }

    fun stopListening() {
        // Cancel the data collection job
        dataCollectionJob?.cancel()
        dataCollectionJob = null
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

    fun requestDataSync() {
        checkConnection()
    }
}
