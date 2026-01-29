package com.axon.data.dto

data class SensorDataDto(
    val heartRate: Double,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
)

data class SessionReceivedDto(
    val sessionId: Long,
    val dataPointCount: Int,
)
