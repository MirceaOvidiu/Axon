package com.axon.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recording_sessions")
data class RecordingSession(
    @PrimaryKey
    val id: Long, // Use the same ID from the watch
    val startTime: Long,
    val endTime: Long,
    val receivedAt: Long = System.currentTimeMillis(),
    val dataPointCount: Int = 0
)
