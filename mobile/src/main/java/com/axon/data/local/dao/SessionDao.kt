package com.axon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.axon.data.local.entity.SensorDataEntity
import com.axon.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    // Session operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSensorData(sensorDataList: List<SensorDataEntity>)

    @Query("SELECT * FROM recording_sessions ORDER BY startTime DESC")
    fun getAllSessionsFlow(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM recording_sessions ORDER BY startTime DESC")
    suspend fun getAllSessions(): List<SessionEntity>

    @Query("SELECT * FROM recording_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: Long): SessionEntity?

    @Query("SELECT * FROM recording_sessions WHERE id = :sessionId")
    fun getSessionFlow(sessionId: Long): Flow<SessionEntity?>

    @Query("DELETE FROM recording_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    // Sensor data operations
    @Query("SELECT * FROM sensor_data WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getSensorDataBySession(sessionId: Long): List<SensorDataEntity>

    @Query("SELECT * FROM sensor_data WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getSensorDataBySessionFlow(sessionId: Long): Flow<List<SensorDataEntity>>

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
