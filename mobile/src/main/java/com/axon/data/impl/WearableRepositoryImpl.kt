package com.axon.data.impl

import android.content.Context
import com.axon.data.datasource.WearableEventBus
import com.axon.domain.model.RawSensorData
import com.axon.domain.repository.WearableRepository
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class WearableRepositoryImpl
    @Inject
    constructor(
        private val context: Context,
        private val eventBus: WearableEventBus,
    ) : WearableRepository {
        private val nodeClient = Wearable.getNodeClient(context)
        private val _connectedNodeName = MutableStateFlow<String?>(null)

        override fun getSensorStream(): Flow<RawSensorData> =
            eventBus.sensorDataEvents.map { dto ->
                RawSensorData(dto.heartRate, dto.gyroX, dto.gyroY, dto.gyroZ)
            }

        override fun getConnectedNodeName(): Flow<String?> = _connectedNodeName

        override fun isDeviceConnected(): Flow<Boolean> = _connectedNodeName.map { it != null }

        override suspend fun checkConnection() {
            try {
                val nodes = nodeClient.connectedNodes.await()
                val firstNode = nodes.firstOrNull()
                _connectedNodeName.value = firstNode?.displayName
            } catch (e: Exception) {
                _connectedNodeName.value = null
            }
        }
    }
