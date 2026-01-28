package com.axon.data

import android.content.Context
import android.util.Log
import com.axon.database.AppDatabase
import com.axon.database.SessionDao
import com.axon.models.RecordingSession
import com.axon.models.SensorData
import com.axon.models.SessionTransferData
import com.axon.models.toSensorData
import kotlinx.coroutines.flow.Flow

class SessionRepository(context: Context) {
    private val database: AppDatabase = AppDatabase.getDatabase(context)
    private val sessionDao: SessionDao = database.sessionDao()

    companion object {
        private const val TAG = "SessionRepository"
    }

    /**
     * Save a complete session received from the watch
     */
    suspend fun saveSessionFromWatch(sessionData: SessionTransferData) {
        try {
            // Create the session record
            val session = RecordingSession(
                id = sessionData.sessionId,
                startTime = sessionData.startTime,
                endTime = sessionData.endTime,
                receivedAt = System.currentTimeMillis(),
                dataPointCount = sessionData.sensorReadings.size
            )
            sessionDao.insertSession(session)

            // Convert and save all sensor readings
            val sensorDataList = sessionData.sensorReadings.map { reading ->
                reading.toSensorData(sessionData.sessionId)
            }
            sessionDao.insertAllSensorData(sensorDataList)

            Log.d(TAG, "Saved session ${sessionData.sessionId} with ${sensorDataList.size} readings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save session from watch", e)
            throw e
        }
    }

    // Session queries
    fun getAllSessionsFlow(): Flow<List<RecordingSession>> {
        return sessionDao.getAllSessionsFlow()
    }

    suspend fun getAllSessions(): List<RecordingSession> {
        return sessionDao.getAllSessions()
    }

    suspend fun getSession(sessionId: Long): RecordingSession? {
        return sessionDao.getSession(sessionId)
    }

    fun getSessionFlow(sessionId: Long): Flow<RecordingSession?> {
        return sessionDao.getSessionFlow(sessionId)
    }

    // Sensor data queries
    suspend fun getSensorDataBySession(sessionId: Long): List<SensorData> {
        return sessionDao.getSensorDataBySession(sessionId)
    }

    fun getSensorDataBySessionFlow(sessionId: Long): Flow<List<SensorData>> {
        return sessionDao.getSensorDataBySessionFlow(sessionId)
    }

    // Aggregation queries
    suspend fun getSessionStats(sessionId: Long): SessionStats {
        return SessionStats(
            avgHeartRate = sessionDao.getAverageHeartRate(sessionId),
            maxHeartRate = sessionDao.getMaxHeartRate(sessionId),
            minHeartRate = sessionDao.getMinHeartRate(sessionId),
            avgSkinTemperature = sessionDao.getAverageSkinTemperature(sessionId),
            dataPointCount = sessionDao.getSensorDataCount(sessionId)
        )
    }

    suspend fun deleteSession(sessionId: Long) {
        sessionDao.deleteSensorDataBySession(sessionId)
        sessionDao.deleteSession(sessionId)
        Log.d(TAG, "Deleted session: $sessionId")
    }
}

data class SessionStats(
    val avgHeartRate: Double?,
    val maxHeartRate: Double?,
    val minHeartRate: Double?,
    val avgSkinTemperature: Double?,
    val dataPointCount: Int
)
