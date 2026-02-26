package com.axon.data.remote.model

import com.google.firebase.firestore.DocumentId

data class SensorDataFirestore(
    @DocumentId
    val id: String = "",
    val firestoreSessionId: String = "",
    val timestamp: Long = 0,
    val heartRate: Double? = null,
    val gyroX: Float? = null,
    val gyroY: Float? = null,
    val gyroZ: Float? = null,
)
