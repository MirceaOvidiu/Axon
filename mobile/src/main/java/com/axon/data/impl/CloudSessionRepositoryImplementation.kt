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

/**
 * Firestore schema (nested subcollections):
 *
 * users/
 *   {uid}/
 *     sessions/
 *       {sessionId}/          ← session metadata document
 *         sensor_data/
 *           {timestamp}/      ← one document per sensor reading
 */
@Singleton
class CloudSessionRepositoryImplementation @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : CloudSessionRepository {

    private val _uploadProgress = MutableStateFlow(0f)
    private val _downloadProgress = MutableStateFlow(0f)

    companion object {
        private const val TAG = "CloudSessionRepository"
        private const val USERS_COLLECTION = "users"
        private const val SESSIONS_COLLECTION = "sessions"
        private const val SENSOR_DATA_COLLECTION = "sensor_data"
    }

    // ------------------------------------------------------------------
    // Path helpers
    // ------------------------------------------------------------------

    /** users/{uid}/sessions */
    private fun sessionsRef(uid: String) =
        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .collection(SESSIONS_COLLECTION)

    /** users/{uid}/sessions/{sessionId} */
    private fun sessionDocRef(uid: String, sessionId: Long) =
        sessionsRef(uid).document(sessionId.toString())

    /** users/{uid}/sessions/{sessionId}/sensor_data */
    private fun sensorDataRef(uid: String, sessionId: Long) =
        sessionDocRef(uid, sessionId).collection(SENSOR_DATA_COLLECTION)

    // ------------------------------------------------------------------
    // Upload
    // ------------------------------------------------------------------

    override suspend fun uploadSession(session: Session, sensorData: List<SensorData>): Boolean {
        return try {
            val uid = authRepository.getCurrentUser()?.uid ?: return false

            _uploadProgress.value = 0f

            // 1. Write the session metadata document
            val sessionDoc = mapOf(
                "id"             to session.id,
                "startTime"      to session.startTime,
                "endTime"        to session.endTime,
                "receivedAt"     to session.receivedAt,
                "dataPointCount" to session.dataPointCount,
                "uploadedAt"     to System.currentTimeMillis()
            )
            sessionDocRef(uid, session.id).set(sessionDoc).await()

            _uploadProgress.value = 0.2f

            // 2. Write sensor_data subcollection in batches of 500 (Firestore limit)
            val batches = sensorData.chunked(500)
            batches.forEachIndexed { index, batch ->
                val batchWrite = firestore.batch()

                batch.forEach { data ->
                    val doc = mapOf(
                        "timestamp" to data.timestamp,
                        "heartRate" to data.heartRate,
                        "gyroX"     to data.gyroX,
                        "gyroY"     to data.gyroY,
                        "gyroZ"     to data.gyroZ
                    )
                    val docRef = sensorDataRef(uid, session.id)
                        .document(data.timestamp.toString())
                    batchWrite.set(docRef, doc)
                }

                batchWrite.commit().await()
                _uploadProgress.value = 0.2f + 0.8f * (index + 1) / batches.size
            }

            _uploadProgress.value = 1f
            Log.d(TAG, "Uploaded session ${session.id} — ${sensorData.size} readings")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload session", e)
            false
        }
    }

    // ------------------------------------------------------------------
    // Download — all sessions (metadata only, no sensor data)
    // ------------------------------------------------------------------

    override suspend fun downloadAllSessions(): List<Session> {
        return try {
            val uid = authRepository.getCurrentUser()?.uid ?: return emptyList()

            _downloadProgress.value = 0f
            val snapshot = sessionsRef(uid)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get()
                .await()
            _downloadProgress.value = 1f

            snapshot.documents.mapNotNull { it.toSession() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download sessions", e)
            emptyList()
        }
    }

    // ------------------------------------------------------------------
    // Download — single session metadata
    // ------------------------------------------------------------------

    override suspend fun downloadSession(sessionId: String): Session? {
        return try {
            val uid = authRepository.getCurrentUser()?.uid ?: return null
            sessionDocRef(uid, sessionId.toLong())
                .get()
                .await()
                .toSession()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download session $sessionId", e)
            null
        }
    }

    // ------------------------------------------------------------------
    // Download — sensor_data subcollection of a session
    // ------------------------------------------------------------------

    override suspend fun downloadSensorData(sessionId: String): List<SensorData> {
        return try {
            val uid = authRepository.getCurrentUser()?.uid ?: return emptyList()
            val sessionIdLong = sessionId.toLong()

            _downloadProgress.value = 0f
            val snapshot = sensorDataRef(uid, sessionIdLong)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
            _downloadProgress.value = 1f

            snapshot.documents.mapNotNull { doc ->
                try {
                    SensorData(
                        sessionId  = sessionIdLong,
                        timestamp  = doc.getLong("timestamp") ?: 0L,
                        heartRate  = doc.getDouble("heartRate"),
                        gyroX      = doc.getDouble("gyroX")?.toFloat(),
                        gyroY      = doc.getDouble("gyroY")?.toFloat(),
                        gyroZ      = doc.getDouble("gyroZ")?.toFloat()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse sensor reading", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download sensor data for session $sessionId", e)
            emptyList()
        }
    }

    // ------------------------------------------------------------------
    // Delete — removes session doc + entire sensor_data subcollection
    // ------------------------------------------------------------------

    override suspend fun deleteCloudSession(sessionId: String): Boolean {
        return try {
            val uid = authRepository.getCurrentUser()?.uid ?: return false
            val sessionIdLong = sessionId.toLong()

            // Delete all sensor_data documents first
            val sensorDocs = sensorDataRef(uid, sessionIdLong).get().await()
            val batchSize = 500
            sensorDocs.documents.chunked(batchSize).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }

            // Delete the session document itself
            sessionDocRef(uid, sessionIdLong).delete().await()

            Log.d(TAG, "Deleted cloud session $sessionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete cloud session $sessionId", e)
            false
        }
    }

    override suspend fun syncSessionsWithCloud(): Boolean = true

    override fun getUploadProgress(): Flow<Float> = _uploadProgress.asStateFlow()
    override fun getDownloadProgress(): Flow<Float> = _downloadProgress.asStateFlow()

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun com.google.firebase.firestore.DocumentSnapshot.toSession(): Session? =
        try {
            Session(
                id             = getLong("id") ?: 0L,
                startTime      = getLong("startTime") ?: 0L,
                endTime        = getLong("endTime") ?: 0L,
                receivedAt     = getLong("receivedAt") ?: System.currentTimeMillis(),
                dataPointCount = getLong("dataPointCount")?.toInt() ?: 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse session document $id", e)
            null
        }
}
