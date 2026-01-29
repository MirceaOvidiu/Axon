package com.axon.data.mapper

import com.axon.data.local.entity.SensorDataEntity
import com.axon.data.local.entity.SessionEntity
import com.axon.domain.model.SensorData
import com.axon.domain.model.Session
import com.axon.domain.model.SessionTransferData

fun SessionEntity.toDomain(): Session =
    Session(
        id = this.id,
        startTime = this.startTime,
        endTime = this.endTime,
        receivedAt = this.receivedAt,
        dataPointCount = this.dataPointCount,
    )

// Mapper: Domain -> Entity
fun Session.toEntity(): SessionEntity =
    SessionEntity(
        id = this.id,
        startTime = this.startTime,
        endTime = this.endTime,
        receivedAt = this.receivedAt,
        dataPointCount = this.dataPointCount,
    )

fun SessionTransferData.toDomainSession(): Session =
    Session(
        id = this.sessionId,
        startTime = this.startTime,
        endTime = this.endTime,
        receivedAt = System.currentTimeMillis(), // We set the receipt time now
        dataPointCount = this.sensorReadings.size,
    )

// Helper to map the list of readings inside the transfer data to SensorDataEntity for DB insertion
fun SessionTransferData.toSensorDataEntities(): List<SensorDataEntity> =
    this.sensorReadings.map { reading ->
        SensorDataEntity(
            id = 0, // 0 tells Room to auto-generate the ID
            sessionId = this.sessionId,
            timestamp = reading.timestamp,
            heartRate = reading.heartRate ?: 0.0,
            gyroX = reading.gyroX?.toDouble() ?: 0.0,
            gyroY = reading.gyroY?.toDouble() ?: 0.0,
            gyroZ = reading.gyroZ?.toDouble() ?: 0.0,
        )
    }
