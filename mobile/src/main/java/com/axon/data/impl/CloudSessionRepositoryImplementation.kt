package com.axon.data.impl

import android.util.Log
import com.axon.domain.model.SensorData
import com.axon.domain.model.Session
import com.axon.domain.repository.AuthRepository
import com.axon.domain.repository.CloudSessionRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSessionRepositoryImplementation @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : CloudSessionRepository {

    private val _uploadProgress = MutableStateFlow(0f)
    private val _downloadProgress = MutableStateFlow(0f)

    companion object {
        private const val TAG = "CloudSessionRepository"
        private const val SESSIONS_COLLECTION = "sessions"
        private const val SENSOR_DATA_COLLECTION = "sensor_data"
    }

    override suspend fun uploadSession(session: Session, sensorData: List<SensorData>): Boolean {
        return try {
            val currentUser = authRepository.getCurrentUser() ?: return false

            _uploadProgress.value = 0f

            // Prepare session document with user ID
            val sessionDoc = mapOf(
                "id" to session.id,
                "userId" to currentUser.uid,
                "startTime" to session.startTime,
                "endTime" to session.endTime,
                "receivedAt" to session.receivedAt,
                "dataPointCount" to session.dataPointCount,
                "uploadedAt" to System.currentTimeMillis()
            )

            // Upload session document
            val sessionRef = firestore.collection(SESSIONS_COLLECTION)
                .document("${currentUser.uid}_${session.id}")

            sessionRef.set(sessionDoc).await()

            _uploadProgress.value = 0.3f

            // Upload sensor data in batches
            val batchSize = 500 // Firestore batch limit
            val batches = sensorData.chunked(batchSize)

            for ((index, batch) in batches.withIndex()) {
                val batchWrite = firestore.batch()

                batch.forEach { data ->
                    val sensorDataDoc = mapOf(
                        "sessionId" to session.id,
                        "userId" to currentUser.uid,
                        "timestamp" to data.timestamp,
                        "heartRate" to data.heartRate,
                        "gyroX" to data.gyroX,
                        "gyroY" to data.gyroY,
                        "gyroZ" to data.gyroZ
                    )

                    val docRef = firestore.collection(SENSOR_DATA_COLLECTION)
                        .document("${currentUser.uid}_${session.id}_${data.timestamp}")

                    batchWrite.set(docRef, sensorDataDoc)
                }

                batchWrite.commit().await()

                val progress = 0.3f + (0.7f * (index + 1) / batches.size)
                _uploadProgress.value = progress
            }

            _uploadProgress.value = 1f

            Log.d(TAG, "Successfully uploaded session ${session.id} with ${sensorData.size} data points")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload session", e)
            false
        }
    }

    override suspend fun downloadAllSessions(): List<Session> {
        return try {
            val currentUser = authRepository.getCurrentUser() ?: return emptyList()

            _downloadProgress.value = 0f

            val querySnapshot = firestore.collection(SESSIONS_COLLECTION)
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get()
                .await()

            _downloadProgress.value = 1f

            querySnapshot.documents.mapNotNull { doc ->
                try {
                    Session(
                        id = doc.getLong("id") ?: 0L,
                        startTime = doc.getLong("startTime") ?: 0L,
                        endTime = doc.getLong("endTime") ?: 0L,
                        receivedAt = doc.getLong("receivedAt") ?: System.currentTimeMillis(),
                        dataPointCount = doc.getLong("dataPointCount")?.toInt() ?: 0
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse session document", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download sessions", e)
            emptyList()
        }
    }

    override suspend fun downloadSession(sessionId: String): Session? {
        return try {
            val currentUser = authRepository.getCurrentUser() ?: return null

            val doc = firestore.collection(SESSIONS_COLLECTION)
                .document("${currentUser.uid}_$sessionId")
                .get()
                .await()

            if (doc.exists()) {
                Session(
                    id = doc.getLong("id") ?: 0L,
                    startTime = doc.getLong("startTime") ?: 0L,
                    endTime = doc.getLong("endTime") ?: 0L,
                    receivedAt = doc.getLong("receivedAt") ?: System.currentTimeMillis(),
                    dataPointCount = doc.getLong("dataPointCount")?.toInt() ?: 0
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download session", e)
            null
        }
    }

    override suspend fun downloadSensorData(sessionId: String): List<SensorData> {
        return try {
            val currentUser = authRepository.getCurrentUser() ?: return emptyList()

            _downloadProgress.value = 0f

            val querySnapshot = firestore.collection(SENSOR_DATA_COLLECTION)
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("sessionId", sessionId.toLongOrNull())
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            _downloadProgress.value = 1f

            querySnapshot.documents.mapNotNull { doc ->
                try {
                    SensorData(
                        sessionId = doc.getLong("sessionId") ?: 0L,
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        heartRate = doc.getDouble("heartRate"),
                        gyroX = doc.getDouble("gyroX")?.toFloat(),
                        gyroY = doc.getDouble("gyroY")?.toFloat(),
                        gyroZ = doc.getDouble("gyroZ")?.toFloat()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse sensor data document", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download sensor data", e)
            emptyList()
        }
    }

    override suspend fun deleteCloudSession(sessionId: String): Boolean {
        return try {
            val currentUser = authRepository.getCurrentUser() ?: return false

            val batch = firestore.batch()

            // Delete session document
            val sessionRef = firestore.collection(SESSIONS_COLLECTION)
                .document("${currentUser.uid}_$sessionId")
            batch.delete(sessionRef)

            // Delete associated sensor data
            val sensorDataQuery = firestore.collection(SENSOR_DATA_COLLECTION)
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("sessionId", sessionId.toLongOrNull())
                .get()
                .await()

            sensorDataQuery.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            batch.commit().await()

            Log.d(TAG, "Successfully deleted cloud session $sessionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete cloud session", e)
            false
        }
    }

    override suspend fun syncSessionsWithCloud(): Boolean {
        return try {
            // This would involve comparing local and cloud sessions
            // and syncing the differences. For now, we'll just return success
            // This would be implemented based on your specific sync strategy
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync sessions with cloud", e)
            false
        }
    }

    override fun getUploadProgress(): Flow<Float> = _uploadProgress.asStateFlow()

    override fun getDownloadProgress(): Flow<Float> = _downloadProgress.asStateFlow()
}
