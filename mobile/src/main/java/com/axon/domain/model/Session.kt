package com.axon.domain.model

import java.util.UUID

data class Session(
    val id: Long = 0, // Local Room auto-generated ID
    val firestoreId: String = UUID.randomUUID().toString(), // Unique Firestore document ID
    val userId: String = "",
    val startTime: Long,
    val endTime: Long,
    val receivedAt: Long = System.currentTimeMillis(),
    val dataPointCount: Int = 0,
)
