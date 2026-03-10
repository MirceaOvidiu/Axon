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
    val sparcScore: Double? = null,
    val ldljScore: Double? = null,
    val sparcResults: List<SessionRepResult>? = null,
    val ldljResults: List<SessionRepResult>? = null,
    val sparcPlotUrl: String? = null,
    val ldljPlotUrl: String? = null,
    val hrvScore: Double? = null,
    val hrvSdnn: Double? = null,
    val hrvMeanHr: Double? = null,
    val hrvPlotUrl: String? = null,
)
