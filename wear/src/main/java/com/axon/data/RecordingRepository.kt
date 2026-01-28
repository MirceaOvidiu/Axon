package com.axon.data

import android.content.Context
import android.util.Log
import com.axon.database.AppDatabase
import com.axon.database.SensorDao
import com.axon.models.RecordingSession
import com.axon.models.SensorData
import com.axon.models.SessionTransferData
import com.axon.models.toSensorReading
import kotlinx.coroutines.flow.Flow

class RecordingRepository(context: Context) {
    private val database: AppDatabase = AppDatabase.getDatabase(context)
    private val sensorDao: SensorDao = database.sensorDao()

    companion object {
        private const val TAG = "RecordingRepository"
    }

    // Session operations
    suspend fun startNewSession(): Long {
        val session = RecordingSession(
            startTime = System.currentTimeMillis(),
            isActive = true
        )
        val sessionId = sensorDao.insertSession(session)
        Log.d(TAG, "Started new recording session: $sessionId")
        return sessionId
    }

    suspend fun endSession(sessionId: Long) {
        sensorDao.endSession(sessionId, System.currentTimeMillis())
        Log.d(TAG, "Ended recording session: $sessionId")
    }

    suspend fun getActiveSession(): RecordingSession? {
        return sensorDao.getActiveSession()
    }

    suspend fun getSession(sessionId: Long): RecordingSession? {
        return sensorDao.getSession(sessionId)
    }

    fun getAllSessionsFlow(): Flow<List<RecordingSession>> {
        return sensorDao.getAllSessionsFlow()
    }

    suspend fun getAllSessions(): List<RecordingSession> {
        return sensorDao.getAllSessions()
    }

    // Sensor data operations
    suspend fun saveSensorData(
        sessionId: Long,
        heartRate: Double?,
        gyroX: Float?,
        gyroY: Float?,
        gyroZ: Float?
    ): Long {
        val sensorData = SensorData(
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            heartRate = heartRate,
            gyroX = gyroX,
            gyroY = gyroY,
            gyroZ = gyroZ
        )
        return sensorDao.insertSensorData(sensorData)
    }

    suspend fun getSensorDataBySession(sessionId: Long): List<SensorData> {
        return sensorDao.getSensorDataBySession(sessionId)
    }

    fun getSensorDataBySessionFlow(sessionId: Long): Flow<List<SensorData>> {
        return sensorDao.getSensorDataBySessionFlow(sessionId)
    }

    suspend fun getSensorDataCount(sessionId: Long): Int {
        return sensorDao.getSensorDataCount(sessionId)
    }

    // Sync operations
    suspend fun getUnsyncedCompletedSessions(): List<RecordingSession> {
        return sensorDao.getUnsyncedCompletedSessions()
    }

    suspend fun markSessionAsSynced(sessionId: Long) {
        sensorDao.markSessionAsSynced(sessionId, System.currentTimeMillis())
        sensorDao.markSessionDataAsSynced(sessionId)
        Log.d(TAG, "Marked session $sessionId as synced")
    }

    suspend fun prepareSessionForTransfer(sessionId: Long): SessionTransferData? {
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
            sensorReadings = sensorData.map { it.toSensorReading() }
        )
    }

    suspend fun deleteSession(sessionId: Long) {
        sensorDao.deleteSensorDataBySession(sessionId)
        sensorDao.deleteSession(sessionId)
        Log.d(TAG, "Deleted session: $sessionId")
    }
}
