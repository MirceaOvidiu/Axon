package com.axon.data.service

import android.util.Log
import com.axon.data.datasource.WearableEventBus
import com.axon.data.dto.SensorDataDto
import com.axon.data.mapper.toDomain
import com.axon.data.mapper.toSensorDataEntities
import com.axon.domain.model.Session
import com.axon.domain.model.SessionTransferData
import com.axon.domain.repository.SessionRepository
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DataLayerListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "DataLayerListener"
    }

    // Inject the singletons
    @Inject
    lateinit var eventBus: WearableEventBus

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var gson: Gson

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path
                    ?.startsWith("/sensor_data") == true
            ) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                // Create DTO
                val dto =
                    SensorDataDto(
                        heartRate = dataMap.getDouble("heart_rate"),
                        gyroX = dataMap.getFloat("gyro_x"),
                        gyroY = dataMap.getFloat("gyro_y"),
                        gyroZ = dataMap.getFloat("gyro_z"),
                    )

                // Send to Bus
                eventBus.emitSensorData(dto)
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/session_data") {
            serviceScope.launch {
                try {
                    val json = String(messageEvent.data, Charsets.UTF_8)
                    Log.d(TAG, "Received session data: ${json.take(200)}...")

                    val data = gson.fromJson(json, SessionTransferData::class.java)
                    Log.d(TAG, "Parsed session: id=${data.sessionId}, readings=${data.sensorReadings.size}")

                    // Create session from transfer data
                    // Note: id=0 tells Room to auto-generate, userId will be set by repository
                    val session = Session(
                        id = 0, // Let Room auto-generate the ID
                        userId = "", // Will be set by repository based on authenticated user
                        startTime = data.startTime,
                        endTime = data.endTime,
                        receivedAt = System.currentTimeMillis(),
                        dataPointCount = data.sensorReadings.size
                    )

                    // Insert session (repository will associate with current user and upload to Firestore)
                    val newSessionId = sessionRepository.insertSession(session)
                    Log.d(TAG, "Session inserted with ID: $newSessionId")

                    // Convert entities to domain models using mapper and insert
                    val sensorEntities = data.toSensorDataEntities(newSessionId)
                    Log.d(TAG, "Converting ${sensorEntities.size} entities to domain models")

                    val sensorDataList = sensorEntities.map { it.toDomain() }
                    Log.d(TAG, "Inserting ${sensorDataList.size} sensor data points for session $newSessionId")

                    sessionRepository.insertSensorData(sensorDataList)
                    Log.d(TAG, "Successfully inserted sensor data for session $newSessionId")

                } catch (e: IllegalStateException) {
                    Log.e(TAG, "User not authenticated - cannot save session: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process session data", e)
                }
            }
        }
    }
}
