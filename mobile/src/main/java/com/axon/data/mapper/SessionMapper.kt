package com.axon.data.mapper

import com.axon.data.local.entity.SensorDataEntity
import com.axon.data.local.entity.SessionEntity
import com.axon.domain.model.Session
import com.axon.domain.model.SessionTransferData

fun SessionEntity.toDomain(): Session =
    Session(
        id = this.id,
        firestoreId = this.firestoreId,
        userId = this.userId,
        startTime = this.startTime,
        endTime = this.endTime,
        receivedAt = this.receivedAt,
        dataPointCount = this.dataPointCount,
    )

fun Session.toEntity(): SessionEntity =
    SessionEntity(
        id = this.id,
        firestoreId = this.firestoreId,
        userId = this.userId,
        startTime = this.startTime,
        endTime = this.endTime,
        receivedAt = this.receivedAt,
        dataPointCount = this.dataPointCount,
    )

fun SessionTransferData.toDomainSession(userId: String): Session =
    Session(
        id = this.sessionId,
        userId = userId,
        startTime = this.startTime,
        endTime = this.endTime,
        receivedAt = System.currentTimeMillis(),
        dataPointCount = this.sensorReadings.size,
    )

fun SessionTransferData.toSensorDataEntities(sessionId: Long): List<SensorDataEntity> =
    this.sensorReadings.map { reading ->
        SensorDataEntity(
            id = 0,
            sessionId = sessionId,
            timestamp = reading.timestamp,
            heartRate = reading.heartRate,
            gyroX = reading.gyroX?.toDouble(),
            gyroY = reading.gyroY?.toDouble(),
            gyroZ = reading.gyroZ?.toDouble(),
        )
    }
