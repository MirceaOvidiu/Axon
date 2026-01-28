package com.axon.models

import androidx.room.PrimaryKey
import androidx.room.Entity

data class RecordingSession(
    @Entity(tableName = "recording_sessions")
    val syncedAt: Long? = null,
    val isSynced: Boolean = false,
    val isActive: Boolean = true,
    val endTime: Long? = null,
    val startTime: Long,
    val id: Long = 0
    )