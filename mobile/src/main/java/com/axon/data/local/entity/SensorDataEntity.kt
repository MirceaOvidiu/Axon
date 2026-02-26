package com.axon.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_data")
data class SensorDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val heartRate: Double?,
    val gyroX: Double?,
    val gyroY: Double?,
    val gyroZ: Double?,
)
