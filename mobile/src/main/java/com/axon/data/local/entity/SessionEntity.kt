package com.axon.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recording_sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var startTime: Long = 0,
    var endTime: Long = 0,
    val receivedAt: Long = System.currentTimeMillis(),
    val dataPointCount: Int = 0,
)
