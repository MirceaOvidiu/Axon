package com.axon.domain.repository

import com.axon.domain.model.SensorData
import com.axon.domain.model.Session
import com.axon.domain.model.SessionStats
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    /**
     * Get all sessions for the current authenticated user.
     * Returns an empty flow if user is not authenticated.
     */
    fun getAllSessions(): Flow<List<Session>>

    /**
     * Get a specific session by ID (must belong to current user).
     */
    suspend fun getSession(sessionId: Long): Session?

    /**
     * Insert a new session for the current user.
     * The session will be associated with the current authenticated user.
     */
    suspend fun insertSession(session: Session): Long

    /**
     * Insert sensor data for a session.
     */
    suspend fun insertSensorData(sensorDataList: List<SensorData>)

    /**
     * Delete a session (must belong to current user).
     */
    suspend fun deleteSession(sessionId: Long)

    /**
     * Get sensor data for a specific session.
     */
    suspend fun getSensorDataBySession(sessionId: Long): List<SensorData>

    /**
     * Get statistics for a specific session.
     */
    suspend fun getSessionStats(sessionId: Long): SessionStats?

    /**
     * Delete all local sessions for a user (for account deletion/logout).
     */
    suspend fun deleteAllSessionsForUser(userId: String)

    /**
     * Sync sessions from Firestore to local database for current user.
     */
    suspend fun syncSessionsFromCloud()
}
