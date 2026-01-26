package com.axon.data

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.Intent

class DataLayerListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "DataLayerListener"
        const val SENSOR_DATA_PATH = "/sensor_data"
        const val ACTION_DATA_RECEIVED = "com.axon.data.DATA_RECEIVED"
        const val EXTRA_HEART_RATE = "extra_heart_rate"
        const val EXTRA_GYRO_X = "extra_gyro_x"
        const val EXTRA_GYRO_Y = "extra_gyro_y"
        const val EXTRA_GYRO_Z = "extra_gyro_z"
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
                                val gyroX = getFloat("gyro_x", 0f)
                                val gyroY = getFloat("gyro_y", 0f)
                                val gyroZ = getFloat("gyro_z", 0f)

                                Log.d(TAG, "✓✓✓ DATA RECEIVED from watch: HR=$heartRate, Gyro=($gyroX, $gyroY, $gyroZ)")

                                // Broadcast the data to the app
                                val intent = Intent(ACTION_DATA_RECEIVED).apply {
                                    putExtra(EXTRA_HEART_RATE, heartRate)
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
}
