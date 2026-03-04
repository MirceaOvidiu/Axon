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
        sparcScore = this.sparcScore,
        ldljScore = this.ldljScore,
        sparcResults = this.sparcResults,
        ldljResults = this.ldljResults,
        sparcPlotUrl = this.sparcPlotUrl,
        ldljPlotUrl = this.ldljPlotUrl,
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
        sparcScore = this.sparcScore,
        ldljScore = this.ldljScore,
        sparcResults = this.sparcResults,
        ldljResults = this.ldljResults,
        sparcPlotUrl = this.sparcPlotUrl,
        ldljPlotUrl = this.ldljPlotUrl,
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
