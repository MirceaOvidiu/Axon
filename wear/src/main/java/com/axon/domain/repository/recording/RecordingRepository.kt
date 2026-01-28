package com.axon.domain.repository.recording

import android.content.Context
import com.axon.domain.entity.RecordingSession
import com.axon.domain.models.SessionTransferData

class RecordingRepository(
    context: Context,
) : RecordingRepositoryContract {
    private val impl = RecordingRepositoryContractImplementation(context)

    override suspend fun startNewSession(): Long = impl.startNewSession()

    override suspend fun endSession(sessionId: Long) = impl.endSession(sessionId)

    override suspend fun getActiveSession(): RecordingSession? = impl.getActiveSession()

    override suspend fun saveSensorData(
        sessionId: Long,
        heartRate: Double?,
        gyroX: Float?,
        gyroY: Float?,
        gyroZ: Float?,
    ): Long = impl.saveSensorData(sessionId, heartRate, gyroX, gyroY, gyroZ)

    override suspend fun getSensorDataCount(sessionId: Long): Int = impl.getSensorDataCount(sessionId)

    override suspend fun prepareSessionForTransfer(sessionId: Long): SessionTransferData? = impl.prepareSessionForTransfer(sessionId)

    override suspend fun markSessionAsSynced(sessionId: Long) = impl.markSessionAsSynced(sessionId)

    override suspend fun getUnsyncedCompletedSessions(): List<RecordingSession> = impl.getUnsyncedCompletedSessions()
}
