package com.axon.domain.repository

import com.axon.domain.model.SensorData
import com.axon.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface CloudSessionRepository {
    suspend fun uploadSession(session: Session, sensorData: List<SensorData>): Boolean
    suspend fun downloadAllSessions(): List<Session>
    suspend fun downloadSession(sessionId: String): Session?
    suspend fun downloadSensorData(sessionId: String): List<SensorData>
    suspend fun deleteCloudSession(sessionId: String): Boolean
    suspend fun syncSessionsWithCloud(): Boolean
    fun getUploadProgress(): Flow<Float>
    fun getDownloadProgress(): Flow<Float>
}
