package com.axon.domain.model

data class SensorData(
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val heartRate: Double?,
    val gyroX: Float?,
    val gyroY: Float?,
    val gyroZ: Float?,
)
