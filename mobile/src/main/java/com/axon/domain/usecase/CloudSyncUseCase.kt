package com.axon.domain.usecase

import com.axon.domain.model.Session
import com.axon.domain.repository.AuthRepository
import com.axon.domain.repository.CloudSessionRepository
import com.axon.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val cloudSessionRepository: CloudSessionRepository,
    private val authRepository: AuthRepository
) {

    suspend fun syncSessionToCloud(sessionId: Long): CloudSyncResult {
        return try {
            val session = sessionRepository.getSession(sessionId)
                ?: return CloudSyncResult.Error("Session not found")

            val sensorData = sessionRepository.getSensorDataBySession(sessionId)

            val success = cloudSessionRepository.uploadSession(session, sensorData)

            if (success) {
                CloudSyncResult.Success("Session uploaded successfully")
            } else {
                CloudSyncResult.Error("Failed to upload session")
            }
        } catch (e: Exception) {
            CloudSyncResult.Error("Upload failed: ${e.message}")
        }
    }

    suspend fun syncAllLocalSessions(): CloudSyncResult {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return CloudSyncResult.Error("User not authenticated")

            var uploadedCount = 0
            var failedCount = 0

            sessionRepository.getAllSessions().collect { sessions ->
                sessions.forEach { session ->
                    try {
                        val sensorData = sessionRepository.getSensorDataBySession(session.id)
                        val success = cloudSessionRepository.uploadSession(session, sensorData)
                        if (success) {
                            uploadedCount++
                        } else {
                            failedCount++
                        }
                    } catch (_: Exception) {
                        failedCount++
                    }
                }
            }

            CloudSyncResult.Success("Sync complete: $uploadedCount uploaded, $failedCount failed")
        } catch (e: Exception) {
            CloudSyncResult.Error("Sync failed: ${e.message}")
        }
    }

    suspend fun downloadSessionFromCloud(firestoreId: String): CloudSyncResult {
        return try {
            val session = cloudSessionRepository.downloadSession(firestoreId)
                ?: return CloudSyncResult.Error("Session not found in cloud")

            val sensorData = cloudSessionRepository.downloadSensorData(firestoreId)

            // Save to local database
            val localSessionId = sessionRepository.insertSession(session)

            // Update sensor data with local session ID
            val updatedSensorData = sensorData.map { it.copy(sessionId = localSessionId) }
            sessionRepository.insertSensorData(updatedSensorData)

            CloudSyncResult.Success("Session downloaded successfully")
        } catch (e: Exception) {
            CloudSyncResult.Error("Download failed: ${e.message}")
        }
    }

    fun getUploadProgress(): Flow<Float> = cloudSessionRepository.getUploadProgress()

    fun getDownloadProgress(): Flow<Float> = cloudSessionRepository.getDownloadProgress()

    suspend fun getUserCloudSessions(): Flow<List<Session>> = flow {
        try {
            val sessions = cloudSessionRepository.downloadAllSessions()
            emit(sessions)
        } catch (_: Exception) {
            emit(emptyList())
        }
    }
}

sealed class CloudSyncResult {
    data class Success(val message: String) : CloudSyncResult()
    data class Error(val message: String) : CloudSyncResult()
}
