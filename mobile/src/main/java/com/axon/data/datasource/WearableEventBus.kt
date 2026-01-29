package com.axon.data.datasource

import com.axon.data.dto.SensorDataDto
import com.axon.data.dto.SessionReceivedDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearableEventBus
    @Inject
    constructor() {
        private val _sensorDataEvents = MutableSharedFlow<SensorDataDto>(extraBufferCapacity = 1)
        val sensorDataEvents: SharedFlow<SensorDataDto> = _sensorDataEvents.asSharedFlow()

        private val _sessionReceivedEvents =
            MutableSharedFlow<SessionReceivedDto>(extraBufferCapacity = 1)
        val sessionReceivedEvents: SharedFlow<SessionReceivedDto> =
            _sessionReceivedEvents.asSharedFlow()

        fun emitSensorData(data: SensorDataDto) {
            _sensorDataEvents.tryEmit(data)
        }

        fun emitSessionReceived(data: SessionReceivedDto) {
            _sessionReceivedEvents.tryEmit(data)
        }
    }
