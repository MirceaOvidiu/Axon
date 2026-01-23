package com.axon.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class WearableDataSender(private val context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    companion object {
        private const val TAG = "WearableDataSender"
        const val SENSOR_DATA_PATH = "/sensor_data"
        const val KEY_HEART_RATE = "heart_rate"
        const val KEY_GYRO_X = "gyro_x"
        const val KEY_GYRO_Y = "gyro_y"
        const val KEY_GYRO_Z = "gyro_z"
        const val KEY_TIMESTAMP = "timestamp"
    }

    suspend fun sendSensorData(
        heartRate: Double,
        gyroX: Float,
        gyroY: Float,
        gyroZ: Float
    ) {
        try {
            val putDataReq = PutDataMapRequest.create(SENSOR_DATA_PATH).apply {
                dataMap.putDouble(KEY_HEART_RATE, heartRate)
                dataMap.putFloat(KEY_GYRO_X, gyroX)
                dataMap.putFloat(KEY_GYRO_Y, gyroY)
                dataMap.putFloat(KEY_GYRO_Z, gyroZ)
                dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            }.asPutDataRequest()

            putDataReq.setUrgent()

            dataClient.putDataItem(putDataReq).await()
            Log.d(TAG, "Sent sensor data: HR=$heartRate, Gyro=($gyroX, $gyroY, $gyroZ)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send sensor data", e)
        }
    }

    suspend fun checkConnection(): Boolean {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            val isConnected = nodes.isNotEmpty()
            Log.d(TAG, "Connected nodes: ${nodes.size}")
            isConnected
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check connection", e)
            false
        }
    }
}
