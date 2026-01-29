package com.axon.domain.repository.sync

import android.content.Context
import com.axon.data.datalayer.WearableDataSender
import com.axon.domain.models.SessionTransferData

class SyncRepositoryImplementation(
    context: Context,
) : SyncRepository {
    private val sender = WearableDataSender(context)

    override suspend fun sendSensorData(
        heartRate: Double,
        gyroX: Float,
        gyroY: Float,
        gyroZ: Float,
    ) {
        sender.sendSensorData(heartRate, gyroX, gyroY, gyroZ)
    }

    override suspend fun sendSessionData(session: SessionTransferData): Boolean = sender.sendSessionData(session)
}
