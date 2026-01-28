package com.axon.domain.repository.recording

import com.axon.domain.entity.RecordingSession
import com.axon.domain.models.SessionTransferData

interface RecordingRepositoryContract {
    suspend fun startNewSession(): Long

    suspend fun endSession(sessionId: Long)

    suspend fun getActiveSession(): RecordingSession?

    suspend fun saveSensorData(
        sessionId: Long,
        heartRate: Double?,
        gyroX: Float?,
        gyroY: Float?,
        gyroZ: Float?,
    ): Long

    suspend fun getSensorDataCount(sessionId: Long): Int

    suspend fun prepareSessionForTransfer(sessionId: Long): SessionTransferData?

    suspend fun getUnsyncedCompletedSessions(): List<RecordingSession>

    suspend fun markSessionAsSynced(sessionId: Long)
}
