package com.axon.models

/**
 * Data class for transferring session data between watch and phone.
 * Used for JSON serialization via the Data Layer API.
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
    val skinTemperature: Double?,
    val gyroX: Float?,
    val gyroY: Float?,
    val gyroZ: Float?
)

/**
 * Extension function to convert SensorData to SensorReading for transfer
 */
fun SensorData.toSensorReading() = SensorReading(
    timestamp = timestamp,
    heartRate = heartRate,
    skinTemperature = skinTemperature,
    gyroX = gyroX,
    gyroY = gyroY,
    gyroZ = gyroZ
)
