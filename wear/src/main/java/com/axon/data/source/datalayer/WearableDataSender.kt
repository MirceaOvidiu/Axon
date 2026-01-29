package com.axon.data.source.datalayer

import android.content.Context
import android.util.Log
import com.axon.domain.models.SessionTransferData
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await

class WearableDataSender(
    context: Context,
) {
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val gson = Gson()

    companion object {
        private const val TAG = "WearableDataSender"
        const val SENSOR_DATA_PATH = "/sensor_data"
        const val SESSION_DATA_PATH = "/session_data"
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
        gyroZ: Float,
    ) {
        try {
            Log.d(TAG, "▶▶▶ Preparing to send data: HR=$heartRate, Gyro=($gyroX, $gyroY, $gyroZ)")

            val putDataReq =
                PutDataMapRequest
                    .create(SENSOR_DATA_PATH)
                    .apply {
                        dataMap.putDouble(KEY_HEART_RATE, heartRate)
                        dataMap.putFloat(KEY_GYRO_X, gyroX)
                        dataMap.putFloat(KEY_GYRO_Y, gyroY)
                        dataMap.putFloat(KEY_GYRO_Z, gyroZ)
                        dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                    }.asPutDataRequest()

            putDataReq.setUrgent()

            val result = dataClient.putDataItem(putDataReq).await()
            Log.d(TAG, "▶▶▶ DATA SENT SUCCESSFULLY! URI: ${result.uri}")
        } catch (e: Exception) {
            Log.e(TAG, "✗✗✗ FAILED to send sensor data", e)
        }
    }

    /**
     * Send a complete recording session to the phone via MessageClient.
     * Uses MessageClient instead of DataClient for large payloads.
     */
    suspend fun sendSessionData(sessionData: SessionTransferData): Boolean {
        return try {
            Log.d(TAG, "▶▶▶ Sending session data: sessionId=${sessionData.sessionId}, readings=${sessionData.sensorReadings.size}")

            val jsonData = gson.toJson(sessionData)
            val dataBytes = jsonData.toByteArray(Charsets.UTF_8)

            // Get connected nodes
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                Log.w(TAG, "No connected nodes found. Cannot send session data.")
                return false
            }

            var success = false
            for (node in nodes) {
                try {
                    messageClient.sendMessage(node.id, SESSION_DATA_PATH, dataBytes).await()
                    Log.d(TAG, "SESSION DATA SENT to node: ${node.displayName} (${node.id})")
                    success = true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send to node: ${node.displayName}", e)
                }
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "✗✗✗ FAILED to send session data", e)
            false
        }
    }
}
