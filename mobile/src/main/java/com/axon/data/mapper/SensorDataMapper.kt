package com.axon.data.mapper

import com.axon.data.local.entity.SensorDataEntity
import com.axon.domain.model.SensorData

fun SensorDataEntity.toDomain(): SensorData =
    SensorData(
        id = this.id,
        sessionId = this.sessionId,
        timestamp = this.timestamp,
        heartRate = this.heartRate,
        gyroX = this.gyroX?.toFloat(),
        gyroY = this.gyroY?.toFloat(),
        gyroZ = this.gyroZ?.toFloat(),
    )

fun SensorData.toEntity(): SensorDataEntity =
    SensorDataEntity(
        id = this.id,
        sessionId = this.sessionId,
        timestamp = this.timestamp,
        heartRate = this.heartRate,
        gyroX = this.gyroX?.toDouble(),
        gyroY = this.gyroY?.toDouble(),
        gyroZ = this.gyroZ?.toDouble(),
    )
