package com.axon.domain.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recording_sessions")
data class RecordingSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val isActive: Boolean = true,
    val isSynced: Boolean = false,
    val syncedAt: Long? = null
)
