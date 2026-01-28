package com.axon.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.axon.models.RecordingSession
import com.axon.models.SensorData
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDao {
    // Sensor Data operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSensorData(sensorData: SensorData): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSensorData(sensorDataList: List<SensorData>)

    @Query("SELECT * FROM sensor_data WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getSensorDataBySession(sessionId: Long): List<SensorData>

    @Query("SELECT * FROM sensor_data WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getSensorDataBySessionFlow(sessionId: Long): Flow<List<SensorData>>

    @Query("UPDATE sensor_data SET isSynced = 1 WHERE sessionId = :sessionId")
    suspend fun markSessionDataAsSynced(sessionId: Long)

    @Query("DELETE FROM sensor_data WHERE sessionId = :sessionId")
    suspend fun deleteSensorDataBySession(sessionId: Long)

    // Recording Session operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: RecordingSession): Long

    @Update
    suspend fun updateSession(session: RecordingSession)

    @Query("SELECT * FROM recording_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: Long): RecordingSession?

    @Query("SELECT * FROM recording_sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): RecordingSession?

    @Query("SELECT * FROM recording_sessions ORDER BY startTime DESC")
    fun getAllSessionsFlow(): Flow<List<RecordingSession>>

    @Query("SELECT * FROM recording_sessions ORDER BY startTime DESC")
    suspend fun getAllSessions(): List<RecordingSession>

    @Query("SELECT * FROM recording_sessions WHERE isSynced = 0 AND isActive = 0 ORDER BY startTime ASC")
    suspend fun getUnsyncedCompletedSessions(): List<RecordingSession>

    @Query("UPDATE recording_sessions SET isSynced = 1, syncedAt = :syncedAt WHERE id = :sessionId")
    suspend fun markSessionAsSynced(sessionId: Long, syncedAt: Long)

    @Query("UPDATE recording_sessions SET isActive = 0, endTime = :endTime WHERE id = :sessionId")
    suspend fun endSession(sessionId: Long, endTime: Long)

    @Query("DELETE FROM recording_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("SELECT COUNT(*) FROM sensor_data WHERE sessionId = :sessionId")
    suspend fun getSensorDataCount(sessionId: Long): Int
}
