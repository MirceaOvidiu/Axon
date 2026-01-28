package com.axon.domain.repository.recording

import android.content.Context
import android.util.Log
import com.axon.data.source.database.AppDatabase
import com.axon.data.source.database.SensorDao
import com.axon.domain.entity.RecordingSession
import com.axon.domain.entity.SensorData
import com.axon.domain.models.SessionTransferData
import com.axon.domain.models.toSensorReading

class RecordingRepositoryContractImplementation(
    context: Context,
) : RecordingRepositoryContract {
    private val database: AppDatabase = AppDatabase.getDatabase(context)
    private val sensorDao: SensorDao = database.sensorDao()

    companion object {
        private const val TAG = "RecordingRepository"
    }

    // Session operations
    override suspend fun startNewSession(): Long {
        val session =
            RecordingSession(
                startTime = System.currentTimeMillis(),
                isActive = true,
            )
        val sessionId = sensorDao.insertSession(session)
        Log.d(TAG, "Started new recording session: $sessionId")
        return sessionId
    }

    override suspend fun endSession(sessionId: Long) {
        sensorDao.endSession(sessionId, System.currentTimeMillis())
        Log.d(TAG, "Ended recording session: $sessionId")
    }

    override suspend fun getActiveSession(): RecordingSession? = sensorDao.getActiveSession()

    // Sensor data operations
    override suspend fun saveSensorData(
        sessionId: Long,
        heartRate: Double?,
        gyroX: Float?,
        gyroY: Float?,
        gyroZ: Float?,
    ): Long {
        val sensorData =
            SensorData(
                sessionId = sessionId,
                timestamp = System.currentTimeMillis(),
                heartRate = heartRate,
                gyroX = gyroX,
                gyroY = gyroY,
                gyroZ = gyroZ,
            )
        return sensorDao.insertSensorData(sensorData)
    }

    override suspend fun getSensorDataCount(sessionId: Long): Int = sensorDao.getSensorDataCount(sessionId)

    // Sync operations
    override suspend fun getUnsyncedCompletedSessions(): List<RecordingSession> = sensorDao.getUnsyncedCompletedSessions()

    override suspend fun markSessionAsSynced(sessionId: Long) {
        sensorDao.markSessionAsSynced(sessionId, System.currentTimeMillis())
        sensorDao.markSessionDataAsSynced(sessionId)
        Log.d(TAG, "Marked session $sessionId as synced")
    }

    override suspend fun prepareSessionForTransfer(sessionId: Long): SessionTransferData? {
        val session = sensorDao.getSession(sessionId) ?: return null
        if (session.endTime == null) {
            Log.w(TAG, "Cannot transfer active session: $sessionId")
            return null
        }

        val sensorData = sensorDao.getSensorDataBySession(sessionId)
        return SessionTransferData(
            sessionId = session.id,
            startTime = session.startTime,
            endTime = session.endTime,
            sensorReadings = sensorData.map { it.toSensorReading() },
        )
    }
}
