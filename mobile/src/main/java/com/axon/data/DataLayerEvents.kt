package com.axon.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object DataLayerEvents {

    private val _sensorDataEvents = MutableSharedFlow<SensorDataEvent>(extraBufferCapacity = 1)
    val sensorDataEvents: SharedFlow<SensorDataEvent> = _sensorDataEvents.asSharedFlow()

    private val _sessionReceivedEvents = MutableSharedFlow<SessionReceivedEvent>(extraBufferCapacity = 1)
    val sessionReceivedEvents: SharedFlow<SessionReceivedEvent> = _sessionReceivedEvents.asSharedFlow()

    fun emitSensorData(heartRate: Double, gyroX: Float, gyroY: Float, gyroZ: Float) {
        _sensorDataEvents.tryEmit(SensorDataEvent(heartRate, gyroX, gyroY, gyroZ))
    }

    fun emitSessionReceived(sessionId: Long, dataPointCount: Int) {
        _sessionReceivedEvents.tryEmit(SessionReceivedEvent(sessionId, dataPointCount))
    }
}

data class SensorDataEvent(
    val heartRate: Double,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float
)

data class SessionReceivedEvent(
    val sessionId: Long,
    val dataPointCount: Int
)
