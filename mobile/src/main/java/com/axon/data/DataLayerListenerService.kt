package com.axon.data

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.axon.models.SessionTransferData
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DataLayerListenerService : WearableListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private lateinit var sessionRepository: SessionRepository

    companion object {
        private const val TAG = "DataLayerListener"
        const val SENSOR_DATA_PATH = "/sensor_data"
        const val SESSION_DATA_PATH = "/session_data"
        const val ACTION_DATA_RECEIVED = "com.axon.data.DATA_RECEIVED"
        const val ACTION_SESSION_RECEIVED = "com.axon.data.SESSION_RECEIVED"
        const val EXTRA_HEART_RATE = "extra_heart_rate"
        const val EXTRA_SKIN_TEMPERATURE = "extra_skin_temperature"
        const val EXTRA_GYRO_X = "extra_gyro_x"
        const val EXTRA_GYRO_Y = "extra_gyro_y"
        const val EXTRA_GYRO_Z = "extra_gyro_z"
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_DATA_POINT_COUNT = "extra_data_point_count"
    }

    override fun onCreate() {
        super.onCreate()
        sessionRepository = SessionRepository(applicationContext)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "============ onDataChanged called with ${dataEvents.count} events ============")

        try {
            dataEvents.forEach { event ->
                val path = event.dataItem.uri.path
                Log.d(TAG, "Event received with path: $path (Type: ${event.type})")

                if (event.type == DataEvent.TYPE_CHANGED) {
                    event.dataItem.also { item ->
                        if (path?.startsWith(SENSOR_DATA_PATH) == true) {
                            DataMapItem.fromDataItem(item).dataMap.apply {
                                val heartRate = getDouble("heart_rate", 0.0)
                                val skinTemp = if (containsKey("skin_temperature")) getDouble("skin_temperature", 0.0) else null
                                val gyroX = getFloat("gyro_x", 0f)
                                val gyroY = getFloat("gyro_y", 0f)
                                val gyroZ = getFloat("gyro_z", 0f)

                                Log.d(TAG, "✓✓✓ DATA RECEIVED from watch: HR=$heartRate, Temp=$skinTemp, Gyro=($gyroX, $gyroY, $gyroZ)")

                                // Broadcast the data to the app
                                val intent = Intent(ACTION_DATA_RECEIVED).apply {
                                    putExtra(EXTRA_HEART_RATE, heartRate)
                                    skinTemp?.let { putExtra(EXTRA_SKIN_TEMPERATURE, it) }
                                    putExtra(EXTRA_GYRO_X, gyroX)
                                    putExtra(EXTRA_GYRO_Y, gyroY)
                                    putExtra(EXTRA_GYRO_Z, gyroZ)
                                }
                                val sent = LocalBroadcastManager.getInstance(this@DataLayerListenerService).sendBroadcast(intent)
                                Log.d(TAG, "✓✓✓ Broadcast sent to app: $sent")
                            }
                        } else {
                            Log.w(TAG, "Path does not match SENSOR_DATA_PATH: $path")
                        }
                    }
                } else {
                    Log.d(TAG, "Event type is not TYPE_CHANGED: ${event.type}")
                }
            }
        } finally {
            dataEvents.release()
            Log.d(TAG, "============ onDataChanged finished ============")
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "============ onMessageReceived: path=${messageEvent.path} ============")

        when (messageEvent.path) {
            SESSION_DATA_PATH -> {
                handleSessionData(messageEvent.data)
            }
            else -> {
                Log.w(TAG, "Unknown message path: ${messageEvent.path}")
            }
        }
    }

    private fun handleSessionData(data: ByteArray) {
        serviceScope.launch {
            try {
                val jsonString = String(data, Charsets.UTF_8)
                Log.d(TAG, "Received session data JSON (${jsonString.length} chars)")

                val sessionData = gson.fromJson(jsonString, SessionTransferData::class.java)
                Log.d(TAG, "Parsed session: id=${sessionData.sessionId}, readings=${sessionData.sensorReadings.size}")

                // Save to database
                sessionRepository.saveSessionFromWatch(sessionData)

                // Broadcast to notify UI
                val intent = Intent(ACTION_SESSION_RECEIVED).apply {
                    putExtra(EXTRA_SESSION_ID, sessionData.sessionId)
                    putExtra(EXTRA_DATA_POINT_COUNT, sessionData.sensorReadings.size)
                }
                LocalBroadcastManager.getInstance(this@DataLayerListenerService).sendBroadcast(intent)

                Log.d(TAG, "✓✓✓ SESSION SAVED: ${sessionData.sessionId} with ${sessionData.sensorReadings.size} readings")
            } catch (e: Exception) {
                Log.e(TAG, "✗✗✗ Failed to handle session data", e)
            }
        }
    }
}
