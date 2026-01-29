package com.axon.domain.repository.sync

import com.axon.domain.models.SessionTransferData

interface SyncRepository {
    suspend fun sendSensorData(
        heartRate: Double,
        gyroX: Float,
        gyroY: Float,
        gyroZ: Float,
    )

    suspend fun sendSessionData(session: SessionTransferData): Boolean
}
