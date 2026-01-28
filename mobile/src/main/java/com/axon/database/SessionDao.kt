package com.axon.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.axon.models.RecordingSession
import com.axon.models.SensorData
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    // Session operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: RecordingSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSensorData(sensorDataList: List<SensorData>)

    @Query("SELECT * FROM recording_sessions ORDER BY startTime DESC")
    fun getAllSessionsFlow(): Flow<List<RecordingSession>>

    @Query("SELECT * FROM recording_sessions ORDER BY startTime DESC")
    suspend fun getAllSessions(): List<RecordingSession>

    @Query("SELECT * FROM recording_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: Long): RecordingSession?

    @Query("SELECT * FROM recording_sessions WHERE id = :sessionId")
    fun getSessionFlow(sessionId: Long): Flow<RecordingSession?>

    @Query("DELETE FROM recording_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    // Sensor data operations
    @Query("SELECT * FROM sensor_data WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getSensorDataBySession(sessionId: Long): List<SensorData>

    @Query("SELECT * FROM sensor_data WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getSensorDataBySessionFlow(sessionId: Long): Flow<List<SensorData>>

    @Query("DELETE FROM sensor_data WHERE sessionId = :sessionId")
    suspend fun deleteSensorDataBySession(sessionId: Long)

    @Query("SELECT COUNT(*) FROM sensor_data WHERE sessionId = :sessionId")
    suspend fun getSensorDataCount(sessionId: Long): Int

    // Aggregation queries for charts
    @Query("SELECT AVG(heartRate) FROM sensor_data WHERE sessionId = :sessionId AND heartRate IS NOT NULL")
    suspend fun getAverageHeartRate(sessionId: Long): Double?

    @Query("SELECT MAX(heartRate) FROM sensor_data WHERE sessionId = :sessionId AND heartRate IS NOT NULL")
    suspend fun getMaxHeartRate(sessionId: Long): Double?

    @Query("SELECT MIN(heartRate) FROM sensor_data WHERE sessionId = :sessionId AND heartRate IS NOT NULL")
    suspend fun getMinHeartRate(sessionId: Long): Double?
}
