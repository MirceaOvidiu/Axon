package com.axon.domain.repository

import com.axon.domain.model.SensorData
import com.axon.domain.model.Session
import com.axon.domain.model.SessionStats
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getAllSessions(): Flow<List<Session>>

    suspend fun getSession(sessionId: Long): Session?

    suspend fun insertSession(session: Session): Long

    suspend fun deleteSession(sessionId: Long)

    suspend fun getSensorDataBySession(sessionId: Long): List<SensorData>

    suspend fun getSessionStats(sessionId: Long): SessionStats?
}
