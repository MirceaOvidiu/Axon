package com.axon.data.sync

import com.axon.domain.repository.SessionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

}

