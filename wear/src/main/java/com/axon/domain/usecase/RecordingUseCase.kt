package com.axon.domain.usecase

import com.axon.domain.entity.RecordingSession
import com.axon.domain.repository.recording.RecordingRepository
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

// ============================================================================
// Recording Use Case - Handles all recording-related operations
// ============================================================================

@Singleton
class RecordingUseCase
@Inject
constructor(
    private val recordingRepository: RecordingRepository,
) {


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

    suspend fun saveSensorData(
        sessionId: Long,
        heartRate: Double?,
        gyroX: Float,
        gyroY: Float,
        gyroZ: Float,
    ): Long = recordingRepository.saveSensorData(sessionId, heartRate, gyroX, gyroY, gyroZ)

    suspend fun getActiveSession(): RecordingSession? = recordingRepository.getActiveSession()

    suspend fun getSensorDataCount(sessionId: Long): Int =
        recordingRepository.getSensorDataCount(sessionId)

}
