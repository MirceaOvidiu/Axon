package com.axon.domain.usecase

import com.axon.data.source.health.HealthServicesDataSource
import com.axon.data.source.sensors.GyroDataSource
import com.axon.domain.entity.RecordingSession
import com.axon.domain.repository.recording.RecordingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class StartRecordingResult(
    val sessionId: Long,
    val startTime: Long,
    val isSuccess: Boolean,
    val errorMessage: String? = null,
)

data class StopRecordingResult(
    val sessionId: Long,
    val duration: Long,
    val dataPointsCount: Int,
    val isSuccess: Boolean,
    val errorMessage: String? = null,
)

data class RecordingState(
    val isRecording: Boolean = false,
    val sessionId: Long? = null,
    val startTime: Long = 0L,
    val duration: Long = 0L,
    val dataPointsCount: Int = 0,
    val errorMessage: String? = null,
)

// ============================================================================
// Recording Use Case - Handles all recording-related operations
// ============================================================================

@Singleton
class RecordingUseCase
    @Inject
    constructor(
        private val recordingRepository: RecordingRepository,
        private val gyroDataSource: GyroDataSource,
        private val healthDataSource: HealthServicesDataSource,
    ) {
        private val _state = MutableStateFlow(RecordingState())
        val state: StateFlow<RecordingState> = _state.asStateFlow()

        private var durationJob: Job? = null
        private var recordingJob: Job? = null

        private val recordingIntervalMs = 20L // 50Hz

        suspend fun initialize() {
            val activeSession = getActiveSession()
            if (activeSession != null) {
                _state.value =
                    RecordingState(
                        isRecording = true,
                        sessionId = activeSession.id,
                        startTime = activeSession.startTime,
                    )
            }
        }

        suspend fun startRecording(): StartRecordingResult =
            try {
                val sessionId = recordingRepository.startNewSession()
                StartRecordingResult(
                    sessionId = sessionId,
                    startTime = System.currentTimeMillis(),
                    isSuccess = true,
                )
            } catch (e: Exception) {
                StartRecordingResult(
                    sessionId = -1,
                    startTime = 0,
                    isSuccess = false,
                    errorMessage = e.message,
                )
            }

        fun startRecordingWithState(scope: CoroutineScope) {
            scope.launch {
                val result = startRecording()
                if (result.isSuccess) {
                    _state.value =
                        RecordingState(
                            isRecording = true,
                            sessionId = result.sessionId,
                            startTime = result.startTime,
                        )
                    startDurationUpdates(scope)
                    startDataCollection(scope)
                } else {
                    _state.value = _state.value.copy(errorMessage = result.errorMessage)
                }
            }
        }

        suspend fun stopRecording(
            sessionId: Long,
            startTime: Long,
        ): StopRecordingResult =
            try {
                recordingRepository.endSession(sessionId)
                val dataPointsCount = recordingRepository.getSensorDataCount(sessionId)
                val duration = System.currentTimeMillis() - startTime
                StopRecordingResult(
                    sessionId = sessionId,
                    duration = duration,
                    dataPointsCount = dataPointsCount,
                    isSuccess = true,
                )
            } catch (e: Exception) {
                StopRecordingResult(
                    sessionId = sessionId,
                    duration = 0,
                    dataPointsCount = 0,
                    isSuccess = false,
                    errorMessage = e.message,
                )
            }

        fun stopRecordingWithState(scope: CoroutineScope): Job {
            return scope.launch {
                val sessionId = _state.value.sessionId ?: return@launch
                val startTime = _state.value.startTime

                durationJob?.cancel()
                recordingJob?.cancel()

                val result = stopRecording(sessionId, startTime)
                _state.value =
                    RecordingState(
                        isRecording = false,
                        sessionId = null,
                        startTime = 0L,
                        duration = 0L,
                        dataPointsCount = 0,
                        errorMessage = if (!result.isSuccess) result.errorMessage else null,
                    )
            }
        }

        suspend fun saveSensorData(
            sessionId: Long,
            heartRate: Double?,
            gyroX: Float,
            gyroY: Float,
            gyroZ: Float,
        ): Long = recordingRepository.saveSensorData(sessionId, heartRate, gyroX, gyroY, gyroZ)

        suspend fun getActiveSession(): RecordingSession? = recordingRepository.getActiveSession()

        suspend fun getSensorDataCount(sessionId: Long): Int = recordingRepository.getSensorDataCount(sessionId)

        private fun startDurationUpdates(scope: CoroutineScope) {
            durationJob?.cancel()
            durationJob =
                scope.launch {
                    while (_state.value.isRecording) {
                        _state.value =
                            _state.value.copy(
                                duration = System.currentTimeMillis() - _state.value.startTime,
                            )
                        delay(1000)
                    }
                }
        }

        private fun startDataCollection(scope: CoroutineScope) {
            recordingJob?.cancel()
            recordingJob =
                scope.launch {
                    while (_state.value.isRecording) {
                        val sessionId = _state.value.sessionId ?: break
                        try {
                            val gyro = gyroDataSource.values.value
                            val heartRate = healthDataSource.heartRateBpm.value.takeIf { it > 0 }

                            saveSensorData(
                                sessionId = sessionId,
                                heartRate = heartRate,
                                gyroX = gyro[0],
                                gyroY = gyro[1],
                                gyroZ = gyro[2],
                            )

                            // Update count less frequently to reduce overhead
                            if (System.currentTimeMillis() % 500 < recordingIntervalMs) {
                                val count = getSensorDataCount(sessionId)
                                _state.value = _state.value.copy(dataPointsCount = count)
                            }
                        } catch (e: Exception) {
                            _state.value = _state.value.copy(errorMessage = e.message)
                        }
                        delay(recordingIntervalMs)
                    }
                }
        }

        fun clearError() {
            _state.value = _state.value.copy(errorMessage = null)
        }
    }
