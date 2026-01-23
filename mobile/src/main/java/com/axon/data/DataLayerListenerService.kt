package com.axon.data

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class DataLayerListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "DataLayerListener"
        const val SENSOR_DATA_PATH = "/sensor_data"

        // Shared instance for communication with ViewModel
        private var dataCallback: ((Double, Float, Float, Float) -> Unit)? = null

        fun setDataCallback(callback: (Double, Float, Float, Float) -> Unit) {
            dataCallback = callback
        }

        fun removeDataCallback() {
            dataCallback = null
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged called with ${dataEvents.count} events")

        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                event.dataItem.also { item ->
                    if (item.uri.path?.compareTo(SENSOR_DATA_PATH) == 0) {
                        DataMapItem.fromDataItem(item).dataMap.apply {
                            val heartRate = getDouble("heart_rate", 0.0)
                            val gyroX = getFloat("gyro_x", 0f)
                            val gyroY = getFloat("gyro_y", 0f)
                            val gyroZ = getFloat("gyro_z", 0f)

                            Log.d(TAG, "Received data: HR=$heartRate, Gyro=($gyroX, $gyroY, $gyroZ)")

                            // Notify callback
                            dataCallback?.invoke(heartRate, gyroX, gyroY, gyroZ)
                        }
                    }
                }
            }
        }
    }
}
