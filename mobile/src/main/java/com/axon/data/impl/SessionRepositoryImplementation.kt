package com.axon.data.impl

import android.util.Log
import com.axon.data.local.dao.SessionDao
import com.axon.data.mapper.toDomain
import com.axon.data.mapper.toEntity
import com.axon.data.remote.model.SensorDataFirestore
import com.axon.data.remote.model.SessionFirestore
import com.axon.domain.model.SensorData
import com.axon.domain.model.Session
import com.axon.domain.model.SessionStats
import com.axon.domain.repository.SessionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SessionRepositoryImplementation
    @Inject
    constructor(
        private val sessionDao: SessionDao,
        private val firestore: FirebaseFirestore,
        private val firebaseAuth: FirebaseAuth,
    ) : SessionRepository {
        
        companion object {
            private const val TAG = "SessionRepository"
            private const val USERS_COLLECTION = "users"
            private const val SESSIONS_COLLECTION = "sessions"
            private const val SENSOR_DATA_COLLECTION = "sensorData"
        }

        private val currentUserId: String?
            get() = firebaseAuth.currentUser?.uid

        // ── Public API ────────────────────────────────────────────────

        override fun getAllSessions(): Flow<List<Session>> {
            val userId = currentUserId ?: return emptyFlow()
            return sessionDao.getAllSessionsForUserFlow(userId).map { entities ->
                entities.map { it.toDomain() }
            }
        }

        override suspend fun getSession(sessionId: Long): Session? {
            val userId = currentUserId ?: return null
            return sessionDao.getSessionForUser(sessionId, userId)?.toDomain()
        }

        override suspend fun insertSession(session: Session): Long {
            val userId = currentUserId ?: throw IllegalStateException("User not authenticated")

            val sessionWithUser = session.copy(userId = userId)
            val localId = sessionDao.insertSession(sessionWithUser.toEntity())
            val savedSession = sessionWithUser.copy(id = localId)

            uploadSessionToFirestore(savedSession)

            return localId
        }

        override suspend fun insertSensorData(sensorDataList: List<SensorData>) {
            if (sensorDataList.isEmpty()) return

            // Save locally
            sessionDao.insertAllSensorData(sensorDataList.map { it.toEntity() })

            // Find the firestoreId for the session so we know the Firestore path
            val sessionId = sensorDataList.first().sessionId
            val sessionEntity = sessionDao.getSession(sessionId)
            val firestoreId = sessionEntity?.firestoreId

            if (firestoreId != null) {
                uploadSensorDataToFirestore(firestoreId, sensorDataList)
            } else {
                Log.w(TAG, "Could not find firestoreId for local session $sessionId — sensor data saved locally only")
            }
        }

        override suspend fun deleteSession(sessionId: Long) {
            val userId = currentUserId ?: return
            val sessionEntity = sessionDao.getSessionForUser(sessionId, userId) ?: return

            deleteSessionFromFirestore(sessionEntity.firestoreId)
            sessionDao.deleteSensorDataBySession(sessionId)
            sessionDao.deleteSessionForUser(sessionId, userId)
        }

        override suspend fun getSensorDataBySession(sessionId: Long): List<SensorData> =
            sessionDao.getSensorDataBySession(sessionId).map { it.toDomain() }

        override suspend fun getSessionStats(sessionId: Long): SessionStats? {
            val userId = currentUserId ?: return null
            val session = sessionDao.getSessionForUser(sessionId, userId) ?: return null
            val avgHr = sessionDao.getAverageHeartRate(sessionId)
            val maxHr = sessionDao.getMaxHeartRate(sessionId)
            val minHr = sessionDao.getMinHeartRate(sessionId)
            val dataPointCount = sessionDao.getSensorDataCount(sessionId)
            val duration = session.endTime - session.startTime

            return SessionStats(
                averageHeartRate = avgHr,
                maxHeartRate = maxHr,
                minHeartRate = minHr,
                duration = duration,
                dataPointCount = dataPointCount,
            )
        }

        override suspend fun deleteAllSessionsForUser(userId: String) {
            val sessions = sessionDao.getAllSessionsForUser(userId)
            sessions.forEach { sessionDao.deleteSensorDataBySession(it.id) }
            sessionDao.deleteAllSessionsForUser(userId)
        }

        // ── Cloud Sync ────────────────────────────────────────────────

        override suspend fun syncSessionsFromCloud() {
            try {
                val userId = currentUserId ?: return
                Log.d(TAG, "Starting cloud sync for user $userId")

                val snapshot = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(SESSIONS_COLLECTION)
                    .get()
                    .await()

                var synced = 0
                for (doc in snapshot.documents) {
                    val sessionFirestore = doc.toObject(SessionFirestore::class.java) ?: continue
                    val firestoreId = doc.id

                    // Skip if we already have this session locally
                    if (sessionDao.getSessionByFirestoreId(firestoreId) != null) continue

                    // Insert session locally (id = 0 lets Room auto-generate)
                    val session = Session(
                        id = 0,
                        firestoreId = firestoreId,
                        userId = userId,
                        startTime = sessionFirestore.startTime,
                        endTime = sessionFirestore.endTime,
                        receivedAt = sessionFirestore.receivedAt,
                        dataPointCount = sessionFirestore.dataPointCount,
                    )
                    val localId = sessionDao.insertSession(session.toEntity())

                    // Sync sensor data for this session
                    syncSensorDataFromCloud(firestoreId, localId)
                    synced++
                }

                Log.d(TAG, "Cloud sync complete — downloaded $synced new sessions")
            } catch (e: Exception) {
                Log.e(TAG, "Cloud sync failed: ${e.message}", e)
            }
        }

        private suspend fun syncSensorDataFromCloud(firestoreId: String, localSessionId: Long) {
            try {
                val userId = currentUserId ?: return

                val snapshot = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(SESSIONS_COLLECTION)
                    .document(firestoreId)
                    .collection(SENSOR_DATA_COLLECTION)
                    .get()
                    .await()

                val entities = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(SensorDataFirestore::class.java)?.let { data ->
                        SensorData(
                            sessionId = localSessionId,
                            timestamp = data.timestamp,
                            heartRate = data.heartRate,
                            gyroX = data.gyroX,
                            gyroY = data.gyroY,
                            gyroZ = data.gyroZ,
                        )
                    }
                }

                if (entities.isNotEmpty()) {
                    sessionDao.insertAllSensorData(entities.map { it.toEntity() })
                    Log.d(TAG, "Synced ${entities.size} sensor data points for session $firestoreId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync sensor data for session $firestoreId: ${e.message}", e)
            }
        }

        // ── Firestore Upload ──────────────────────────────────────────

        private suspend fun uploadSessionToFirestore(session: Session) {
            try {
                val userId = currentUserId ?: run {
                    Log.w(TAG, "Cannot upload session — user not authenticated")
                    return
                }

                val data = SessionFirestore(
                    userId = userId,
                    localId = session.id,
                    startTime = session.startTime,
                    endTime = session.endTime,
                    receivedAt = session.receivedAt,
                    dataPointCount = session.dataPointCount,
                )

                // Path: users/{userId}/sessions/{firestoreId}
                firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(SESSIONS_COLLECTION)
                    .document(session.firestoreId)
                    .set(data)
                    .await()

                Log.d(TAG, "Uploaded session ${session.firestoreId} to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload session: ${e.message}", e)
            }
        }

        private suspend fun uploadSensorDataToFirestore(firestoreSessionId: String, sensorDataList: List<SensorData>) {
            try {
                val userId = currentUserId ?: run {
                    Log.w(TAG, "Cannot upload sensor data — user not authenticated")
                    return
                }

                Log.d(TAG, "Uploading ${sensorDataList.size} sensor data points for session $firestoreSessionId")

                sensorDataList.chunked(500).forEachIndexed { batchIndex, chunk ->
                    val batch: WriteBatch = firestore.batch()

                    chunk.forEach { data ->
                        val docRef = firestore
                            .collection(USERS_COLLECTION)
                            .document(userId)
                            .collection(SESSIONS_COLLECTION)
                            .document(firestoreSessionId)
                            .collection(SENSOR_DATA_COLLECTION)
                            .document(data.timestamp.toString())

                        batch.set(docRef, SensorDataFirestore(
                            firestoreSessionId = firestoreSessionId,
                            timestamp = data.timestamp,
                            heartRate = data.heartRate,
                            gyroX = data.gyroX,
                            gyroY = data.gyroY,
                            gyroZ = data.gyroZ,
                        ))
                    }

                    batch.commit().await()
                    Log.d(TAG, "Uploaded sensor batch ${batchIndex + 1} (${chunk.size} points)")
                }

                Log.d(TAG, "All ${sensorDataList.size} sensor data points uploaded for $firestoreSessionId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload sensor data: ${e.message}", e)
            }
        }

        // ── Firestore Delete ──────────────────────────────────────────

        private suspend fun deleteSessionFromFirestore(firestoreId: String) {
            try {
                val userId = currentUserId ?: return

                val sensorCol = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(SESSIONS_COLLECTION)
                    .document(firestoreId)
                    .collection(SENSOR_DATA_COLLECTION)

                var deleted: Int
                do {
                    val snap = sensorCol.limit(500).get().await()
                    val batch = firestore.batch()
                    snap.documents.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                    deleted = snap.size()
                } while (deleted >= 500)

                firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(SESSIONS_COLLECTION)
                    .document(firestoreId)
                    .delete()
                    .await()

                Log.d(TAG, "Deleted session $firestoreId from Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete session from Firestore: ${e.message}", e)
            }
        }
    }
