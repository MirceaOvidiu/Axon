package com.axon.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "recording_sessions",
    indices = [Index(value = ["userId"]), Index(value = ["firestoreId"], unique = true)]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    val firestoreId: String = UUID.randomUUID().toString(), // Unique Firestore document ID
    val userId: String = "",
    var startTime: Long = 0,
    var endTime: Long = 0,
    val receivedAt: Long = System.currentTimeMillis(),
    val dataPointCount: Int = 0,
    val sparcScore: Double? = null,
    val ldljScore: Double? = null,
    val sparcResults: List<com.axon.domain.model.SessionRepResult>? = null,
    val ldljResults: List<com.axon.domain.model.SessionRepResult>? = null,
    val sparcPlotUrl: String? = null,
    val ldljPlotUrl: String? = null,
    val hrvScore: Double? = null,
    val hrvSdnn: Double? = null,
    val hrvMeanHr: Double? = null,
    val hrvPlotUrl: String? = null,
)
