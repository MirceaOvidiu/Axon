package com.axon.models

/**
 * Data class for receiving session data from the watch.
 * Used for JSON deserialization via the Data Layer API.
 */
data class SessionTransferData(
    val sessionId: Long,
    val startTime: Long,
    val endTime: Long,
    val sensorReadings: List<SensorReading>
)

data class SensorReading(
    val timestamp: Long,
    val heartRate: Double?,
    val gyroX: Float?,
    val gyroY: Float?,
    val gyroZ: Float?
)

/**
 * Extension function to convert SensorReading to SensorData for database storage
 */
fun SensorReading.toSensorData(sessionId: Long) = SensorData(
    sessionId = sessionId,
    timestamp = timestamp,
    heartRate = heartRate,
    gyroX = gyroX,
    gyroY = gyroY,
    gyroZ = gyroZ
)
