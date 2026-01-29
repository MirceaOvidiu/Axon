package com.axon.domain.repository

import com.axon.domain.model.RawSensorData
import kotlinx.coroutines.flow.Flow

interface WearableRepository {
    fun getSensorStream(): Flow<RawSensorData>

    fun getConnectedNodeName(): Flow<String?>

    fun isDeviceConnected(): Flow<Boolean>

    suspend fun checkConnection()
}
