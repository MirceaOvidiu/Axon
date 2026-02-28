package com.axon.data.sync

import android.util.Log
import com.axon.domain.repository.SessionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Background service to sync local sessions to Firestore.
 * This ensures all sessions are backed up to the cloud.
 */
@Singleton
class SessionSyncManager
    @Inject
    constructor(
        private val sessionRepository: SessionRepository,
        private val firestore: FirebaseFirestore,
        private val firebaseAuth: FirebaseAuth,
    ) {
        companion object {
            private const val TAG = "SessionSyncManager"
            private const val SESSIONS_COLLECTION = "sessions"
        }

        /**
         * Sync all local sessions to Firestore that haven't been uploaded yet.
         * This is useful for uploading existing sessions after implementing cloud sync.
         */
        suspend fun syncAllSessions() {
            withContext(Dispatchers.IO) {
                try {
                    val userId = firebaseAuth.currentUser?.uid
                    if (userId == null) {
                        Log.w(TAG, "No user logged in, skipping sync")
                        return@withContext
                    }

                    // Get all local sessions
                    val sessions = sessionRepository.getAllSessions().first()

                    Log.d(TAG, "Starting sync of ${sessions.size} sessions")

                    var uploadedCount = 0
                    var skippedCount = 0

                    for (session in sessions) {
                        try {
                            val sessionDocId = "${userId}_${session.id}"

                            // Check if session already exists in Firestore
                            val doc = firestore.collection(SESSIONS_COLLECTION)
                                .document(sessionDocId)
                                .get()
                                .await()

                            if (!doc.exists()) {
                                // Session doesn't exist in Firestore, trigger upload
                                // by re-inserting (this will trigger the upload logic)
                                sessionRepository.insertSession(session)

                                // Also upload sensor data
                                val sensorData = sessionRepository.getSensorDataBySession(session.id)
                                if (sensorData.isNotEmpty()) {
                                    sessionRepository.insertSensorData(sensorData)
                                }

                                uploadedCount++
                                Log.d(TAG, "Uploaded session ${session.id} with ${sensorData.size} data points")
                            } else {
                                skippedCount++
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to sync session ${session.id}", e)
                        }
                    }

                    Log.d(TAG, "Sync complete: uploaded=$uploadedCount, skipped=$skippedCount")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync sessions", e)
                }
            }
        }
    }

