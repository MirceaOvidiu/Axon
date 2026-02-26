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
)
