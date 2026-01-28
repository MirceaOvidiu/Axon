package com.axon.presentation.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axon.data.source.health.HealthServicesDataSource
import com.axon.data.source.sensors.GyroDataSource
import com.axon.domain.usecase.RecordingUseCase
import com.axon.domain.usecase.SyncUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val recordingUseCase: RecordingUseCase,
    private val syncUseCase: SyncUseCase,
    private val gyroDataSource: GyroDataSource,
    private val healthDataSource: HealthServicesDataSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var sensorCollectionJob: Job? = null
    private var durationUpdateJob: Job? = null
    private var recordingStartTime = 0L

    init {
        startSensors()
        checkForActiveSession()
        startLiveSensorSync()
    }

    // -------------------------------------------------------------------------
    // Intent Handler - Single entry point for all user actions
    // -------------------------------------------------------------------------

    fun onIntent(intent: MainIntent) {
        when (intent) {
            MainIntent.StartRecording -> startRecording()
            MainIntent.StopRecording -> stopRecording()
            MainIntent.SyncAllSessions -> syncAllSessions()
            MainIntent.ClearError -> clearError()
            MainIntent.ClearSyncResult -> clearSyncResult()
        }
    }

    // -------------------------------------------------------------------------
    // Sensor Management
    // -------------------------------------------------------------------------

    private fun startSensors() {
        healthDataSource.register()
        gyroDataSource.start()
        collectSensorData()
    }

    private fun collectSensorData() {
        sensorCollectionJob?.cancel()
        sensorCollectionJob = viewModelScope.launch {
            // Collect heart rate
            launch {
                healthDataSource.heartRateBpm.collect { bpm ->
                    _uiState.update { it.copy(heartRateBpm = bpm) }
                }
            }
            // Collect availability
            launch {
                healthDataSource.availability.collect { availability ->
                    _uiState.update { it.copy(heartRateAvailability = availability) }
                }
            }
            // Collect gyroscope with throttling for UI
            launch {
                var lastUpdate = 0L
                gyroDataSource.values.collect { values ->
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 50) { // 20 FPS for UI
                        _uiState.update { it.copy(gyroscopeData = values) }
                        lastUpdate = now
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Recording
    // -------------------------------------------------------------------------

    private fun checkForActiveSession() {
        viewModelScope.launch {
            val activeSession = recordingUseCase.getActiveSession()
            if (activeSession != null) {
                _uiState.update {
                    it.copy(
                        isRecording = true,
                        currentSessionId = activeSession.id,
                    )
                }
                recordingStartTime = activeSession.startTime
                startDurationUpdates()
                startDataCollection()
            }
        }
    }

    private fun startRecording() {
        viewModelScope.launch {
            val result = recordingUseCase.startRecording()
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isRecording = true,
                        currentSessionId = result.sessionId,
                        dataPointsRecorded = 0,
                    )
                }
                recordingStartTime = result.startTime
                startDurationUpdates()
                startDataCollection()
                Log.d("MainViewModel", "Started recording session: ${result.sessionId}")
            } else {
                _uiState.update { it.copy(errorMessage = result.errorMessage) }
            }
        }
    }

    private fun stopRecording() {
        viewModelScope.launch {
            val sessionId = _uiState.value.currentSessionId ?: return@launch

            // Stop jobs first
            durationUpdateJob?.cancel()

            val result = recordingUseCase.stopRecording(sessionId, recordingStartTime)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isRecording = false,
                        currentSessionId = null,
                        recordingDuration = 0L,
                        dataPointsRecorded = 0,
                    )
                }
                Log.d("MainViewModel", "Stopped recording session: $sessionId")
                // Auto-sync after recording
                syncSession(sessionId)
            } else {
                _uiState.update { it.copy(errorMessage = result.errorMessage) }
            }
        }
    }

    private fun startDurationUpdates() {
        durationUpdateJob?.cancel()
        durationUpdateJob = viewModelScope.launch {
            while (_uiState.value.isRecording) {
                _uiState.update {
                    it.copy(recordingDuration = System.currentTimeMillis() - recordingStartTime)
                }
                delay(1000)
            }
        }
    }

    private fun startDataCollection() {
        viewModelScope.launch {
            val recordingInterval = 20L // 50Hz
            while (_uiState.value.isRecording) {
                val sessionId = _uiState.value.currentSessionId ?: break
                try {
                    val gyro = _uiState.value.gyroscopeData
                    val heartRate = _uiState.value.heartRateBpm.takeIf { it > 0 }

                    recordingUseCase.saveSensorData(
                        sessionId = sessionId,
                        heartRate = heartRate,
                        gyroX = gyro[0],
                        gyroY = gyro[1],
                        gyroZ = gyro[2],
                    )

                    // Update count less frequently
                    if (System.currentTimeMillis() % 500 < recordingInterval) {
                        val count = recordingUseCase.getSensorDataCount(sessionId)
                        _uiState.update { it.copy(dataPointsRecorded = count) }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to save sensor data", e)
                }
                delay(recordingInterval)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Sync
    // -------------------------------------------------------------------------

    private fun syncSession(sessionId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, lastSyncResult = null) }

            val result = syncUseCase.syncSession(sessionId)
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    lastSyncResult = if (result.isSuccess) {
                        "Synced ${result.readingsCount} readings"
                    } else {
                        result.errorMessage ?: "Sync failed"
                    },
                )
            }
        }
    }

    private fun syncAllSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, lastSyncResult = null) }

            val result = syncUseCase.syncAllSessions()
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    lastSyncResult = if (result.isSuccess) {
                        "Synced ${result.syncedCount} of ${result.totalCount} sessions"
                    } else if (result.syncedCount > 0) {
                        "Synced ${result.syncedCount} of ${result.totalCount}, ${result.failedSessionIds.size} failed"
                    } else {
                        result.errorMessage ?: "Sync failed"
                    },
                )
            }
        }
    }

    private fun startLiveSensorSync() {
        syncUseCase.startLiveSending(viewModelScope) { _uiState.value.isRecording }
    }

    // -------------------------------------------------------------------------
    // Error Handling
    // -------------------------------------------------------------------------

    private fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun clearSyncResult() {
        _uiState.update { it.copy(lastSyncResult = null) }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            healthDataSource.unregister()
        }
        gyroDataSource.stop()
        syncUseCase.stopLiveSending()
        sensorCollectionJob?.cancel()
        durationUpdateJob?.cancel()
    }
}
