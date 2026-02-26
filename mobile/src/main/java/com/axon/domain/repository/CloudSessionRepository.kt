package com.axon.domain.repository

import com.axon.domain.model.SensorData
import com.axon.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface CloudSessionRepository {
    suspend fun uploadSession(session: Session, sensorData: List<SensorData>): Boolean
    suspend fun downloadAllSessions(): List<Session>
    suspend fun downloadSession(firestoreId: String): Session?
    suspend fun downloadSensorData(firestoreId: String): List<SensorData>
    suspend fun deleteCloudSession(firestoreId: String): Boolean
    suspend fun syncSessionsWithCloud(): Boolean
    suspend fun cleanupOldNumericSessions(): Boolean  // Add cleanup method
    fun getUploadProgress(): Flow<Float>
    fun getDownloadProgress(): Flow<Float>
}
