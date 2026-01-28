package com.axon.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.axon.models.RecordingSession
import com.axon.models.SensorData

@Dao
interface SensorDao {
    // Sensor Data operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSensorData(sensorData: SensorData): Long

    @Query("SELECT * FROM sensor_data WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getSensorDataBySession(sessionId: Long): List<SensorData>

    @Query("UPDATE sensor_data SET isSynced = 1 WHERE sessionId = :sessionId")
    suspend fun markSessionDataAsSynced(sessionId: Long)

    // Recording Session operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: RecordingSession): Long

    @Query("SELECT * FROM recording_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: Long): RecordingSession?

    @Query("SELECT * FROM recording_sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): RecordingSession?

    @Query("SELECT * FROM recording_sessions WHERE isSynced = 0 AND isActive = 0 ORDER BY startTime ASC")
    suspend fun getUnsyncedCompletedSessions(): List<RecordingSession>

    @Query("UPDATE recording_sessions SET isSynced = 1, syncedAt = :syncedAt WHERE id = :sessionId")
    suspend fun markSessionAsSynced(sessionId: Long, syncedAt: Long)

    @Query("UPDATE recording_sessions SET isActive = 0, endTime = :endTime WHERE id = :sessionId")
    suspend fun endSession(sessionId: Long, endTime: Long)

    @Query("SELECT COUNT(*) FROM sensor_data WHERE sessionId = :sessionId")
    suspend fun getSensorDataCount(sessionId: Long): Int
}
