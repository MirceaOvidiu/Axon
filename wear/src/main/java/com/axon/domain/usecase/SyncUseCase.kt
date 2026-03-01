package com.axon.domain.usecase

import com.axon.data.health.HealthServicesDataSource
import com.axon.data.sensors.GyroDataSource
import com.axon.domain.repository.recording.RecordingRepository
import com.axon.domain.repository.sync.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// ============================================================================
// Result Data Classes
// ============================================================================

data class SyncResult(
    val sessionId: Long,
    val readingsCount: Int,
    val isSuccess: Boolean,
    val errorMessage: String? = null,
)

data class SyncAllResult(
    val syncedCount: Int,
    val totalCount: Int,
    val failedSessionIds: List<Long>,
    val totalReadingsSynced: Int,
    val isSuccess: Boolean,
    val errorMessage: String? = null,
)

@Singleton
class SyncUseCase
    @Inject
    constructor(
        private val recordingRepository: RecordingRepository,
        private val syncRepository: SyncRepository,
        private val gyroDataSource: GyroDataSource,
        private val healthDataSource: HealthServicesDataSource,
    ) {

    private var liveSendJob: Job? = null
        private var lastSendTime = 0L
        private val sendIntervalMs = 500L

        suspend fun syncSession(sessionId: Long): SyncResult {
            return try {
                val sessionData =
                    recordingRepository.prepareSessionForTransfer(sessionId)
                        ?: return SyncResult(
                            sessionId = sessionId,
                            readingsCount = 0,
                            isSuccess = false,
                            errorMessage = "Session not found or still active",
                        )

                val success = syncRepository.sendSessionData(sessionData)
                if (success) {
                    recordingRepository.markSessionAsSynced(sessionId)
                    SyncResult(
                        sessionId = sessionId,
                        readingsCount = sessionData.sensorReadings.size,
                        isSuccess = true,
                    )
                } else {
                    SyncResult(
                        sessionId = sessionId,
                        readingsCount = 0,
                        isSuccess = false,
                        errorMessage = "Failed to connect to phone",
                    )
                }
            } catch (e: Exception) {
                SyncResult(
                    sessionId = sessionId,
                    readingsCount = 0,
                    isSuccess = false,
                    errorMessage = e.message,
                )
            }
        }

        suspend fun syncAllSessions(): SyncAllResult =
            try {
                val unsyncedSessions = recordingRepository.getUnsyncedCompletedSessions()
                var syncedCount = 0
                var totalReadings = 0
                val failedIds = mutableListOf<Long>()

                for (session in unsyncedSessions) {
                    val sessionData = recordingRepository.prepareSessionForTransfer(session.id)
                    if (sessionData != null) {
                        val success = syncRepository.sendSessionData(sessionData)
                        if (success) {
                            recordingRepository.markSessionAsSynced(session.id)
                            syncedCount++
                            totalReadings += sessionData.sensorReadings.size
                        } else {
                            failedIds.add(session.id)
                        }
                    } else {
                        failedIds.add(session.id)
                    }
                }

                SyncAllResult(
                    syncedCount = syncedCount,
                    totalCount = unsyncedSessions.size,
                    failedSessionIds = failedIds,
                    totalReadingsSynced = totalReadings,
                    isSuccess = failedIds.isEmpty(),
                )
            } catch (e: Exception) {
                SyncAllResult(
                    syncedCount = 0,
                    totalCount = 0,
                    failedSessionIds = emptyList(),
                    totalReadingsSynced = 0,
                    isSuccess = false,
                    errorMessage = e.message,
                )
            }

    fun startLiveSending(
            scope: CoroutineScope,
            isRecordingProvider: () -> Boolean,
        ) {
            liveSendJob?.cancel()
            liveSendJob =
                scope.launch {
                    while (true) {
                        val currentTime = System.currentTimeMillis()
                        if (!isRecordingProvider() && currentTime - lastSendTime > sendIntervalMs) {
                            val gyro = gyroDataSource.values.value
                            syncRepository.sendSensorData(
                                heartRate = healthDataSource.heartRateBpm.value,
                                gyroX = gyro[0],
                                gyroY = gyro[1],
                                gyroZ = gyro[2],
                            )
                            lastSendTime = currentTime
                        }
                        delay(50)
                    }
                }
        }

        fun stopLiveSending() {
            liveSendJob?.cancel()
            liveSendJob = null
        }

}
