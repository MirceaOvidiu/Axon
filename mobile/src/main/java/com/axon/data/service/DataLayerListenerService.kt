package com.axon.data.service

import com.axon.data.datasource.WearableEventBus
import com.axon.data.dto.SensorDataDto
import com.axon.data.local.dao.SessionDao
import com.axon.data.mapper.toDomainSession
import com.axon.data.mapper.toSensorDataEntities
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

@AndroidEntryPoint // Required for Hilt Injection in Services
class DataLayerListenerService : WearableListenerService() {
    // Inject the singletons
    @Inject
    lateinit var eventBus: WearableEventBus

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var sessionDao: SessionDao

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
                    val data = gson.fromJson(json, SessionTransferData::class.java)

                    // Insert session and get its ID
                    sessionRepository.insertSession(data.toDomainSession())

                    // Insert sensor data entities
                    sessionDao.insertAllSensorData(data.toSensorDataEntities())
                } catch (e: Exception) {
                    // Log error
                }
            }
        }
    }
}
