package com.axon.presentation

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.axon.data.RecordingRepository
import com.axon.data.WearableDataSender
import com.axon.senzors.HealthServicesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {
    private val healthServicesManager = HealthServicesManager(application)
    private val sensorManager: SensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val wearableDataSender = WearableDataSender(application)
    private val recordingRepository = RecordingRepository(application)

    val heartRateBpm = healthServicesManager.heartRateBpm
    val availability = healthServicesManager.availability
    val skinTemperature = healthServicesManager.skinTemperature
    val skinTemperatureAvailable = healthServicesManager.skinTemperatureAvailable

    private val _gyroscopeData = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val gyroscopeData = _gyroscopeData.asStateFlow()

    // Recording state
    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration = _recordingDuration.asStateFlow()

    private val _dataPointsRecorded = MutableStateFlow(0)
    val dataPointsRecorded = _dataPointsRecorded.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _lastSyncResult = MutableStateFlow<String?>(null)
    val lastSyncResult = _lastSyncResult.asStateFlow()

    private val gyroscopeSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    private var lastSendTime = 0L
    private val sendInterval = 500L // Send data every 500ms

    private var lastUiUpdate = 0L
    private val uiUpdateInterval = 50L // Update UI every 50ms (20 FPS)

    private var recordingStartTime = 0L
    private var durationUpdateJob: Job? = null
    private var recordingJob: Job? = null

    init {
        healthServicesManager.registerForHeartRateData()
        healthServicesManager.startSkinTemperatureMonitoring()
        startGyroscope()

        // Check for any active session from previous app instance
        viewModelScope.launch {
            val activeSession = recordingRepository.getActiveSession()
            if (activeSession != null) {
                _currentSessionId.value = activeSession.id
                _isRecording.value = true
                recordingStartTime = activeSession.startTime
                startDurationUpdates()
                startRecordingDataCollection()
            }
        }
    }

    private fun startGyroscope() {
        gyroscopeSensor?.let {
            val supported = sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            Log.d("MainViewModel", "Gyroscope sensor registration: $supported")
        }
    }

    private fun stopGyroscope() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                val currentTime = System.currentTimeMillis()

                // Throttle UI updates to prevent excessive recomposition
                if (currentTime - lastUiUpdate > uiUpdateInterval) {
                    _gyroscopeData.value = event.values.clone()
                    lastUiUpdate = currentTime
                }

                // Send real-time data to phone periodically (when not recording)
                if (!_isRecording.value && currentTime - lastSendTime > sendInterval) {
                    sendDataToPhone(heartRateBpm.value)
                    lastSendTime = currentTime
                }
            }
        }
    }

    // Recording functions
    fun startRecording() {
        viewModelScope.launch {
            try {
                val sessionId = recordingRepository.startNewSession()
                _currentSessionId.value = sessionId
                _isRecording.value = true
                _dataPointsRecorded.value = 0
                recordingStartTime = System.currentTimeMillis()

                startDurationUpdates()
                startRecordingDataCollection()

                Log.d("MainViewModel", "Started recording session: $sessionId")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to start recording", e)
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                val sessionId = _currentSessionId.value ?: return@launch

                // Stop recording first
                _isRecording.value = false
                durationUpdateJob?.cancel()
                recordingJob?.cancel()

                // End the session in database
                recordingRepository.endSession(sessionId)

                Log.d("MainViewModel", "Stopped recording session: $sessionId")

                // Automatically sync to phone after recording ends
                syncSessionToPhone(sessionId)

                // Reset state
                _currentSessionId.value = null
                _recordingDuration.value = 0L
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to stop recording", e)
            }
        }
    }

    private fun startDurationUpdates() {
        durationUpdateJob = viewModelScope.launch {
            while (_isRecording.value) {
                _recordingDuration.value = System.currentTimeMillis() - recordingStartTime
                delay(1000)
            }
        }
    }

    private fun startRecordingDataCollection() {
        recordingJob = viewModelScope.launch {
            while (_isRecording.value) {
                val sessionId = _currentSessionId.value ?: break

                try {
                    val gyro = _gyroscopeData.value
                    recordingRepository.saveSensorData(
                        sessionId = sessionId,
                        heartRate = heartRateBpm.value.takeIf { it > 0 },
                        skinTemperature = skinTemperature.value,
                        gyroX = gyro[0],
                        gyroY = gyro[1],
                        gyroZ = gyro[2]
                    )
                    _dataPointsRecorded.value = recordingRepository.getSensorDataCount(sessionId)
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to save sensor data", e)
                }

                delay(500) // Record data every 500ms
            }
        }
    }

    private suspend fun syncSessionToPhone(sessionId: Long) {
        _isSyncing.value = true
        _lastSyncResult.value = null

        try {
            val sessionData = recordingRepository.prepareSessionForTransfer(sessionId)
            if (sessionData == null) {
                _lastSyncResult.value = "Session not found or still active"
                return
            }

            val success = wearableDataSender.sendSessionData(sessionData)
            if (success) {
                recordingRepository.markSessionAsSynced(sessionId)
                _lastSyncResult.value = "Synced ${sessionData.sensorReadings.size} readings"
                Log.d("MainViewModel", "Session $sessionId synced successfully")
            } else {
                _lastSyncResult.value = "Failed to connect to phone"
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to sync session", e)
            _lastSyncResult.value = "Sync failed: ${e.message}"
        } finally {
            _isSyncing.value = false
        }
    }

    fun syncAllUnsyncedSessions() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val unsyncedSessions = recordingRepository.getUnsyncedCompletedSessions()
                var syncedCount = 0

                for (session in unsyncedSessions) {
                    val sessionData = recordingRepository.prepareSessionForTransfer(session.id)
                    if (sessionData != null) {
                        val success = wearableDataSender.sendSessionData(sessionData)
                        if (success) {
                            recordingRepository.markSessionAsSynced(session.id)
                            syncedCount++
                        }
                    }
                }

                _lastSyncResult.value = "Synced $syncedCount of ${unsyncedSessions.size} sessions"
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to sync sessions", e)
                _lastSyncResult.value = "Sync failed: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun sendDataToPhone(heartRate: Double) {
        val gyro = _gyroscopeData.value
        viewModelScope.launch {
            wearableDataSender.sendSensorData(
                heartRate = heartRate,
                skinTemperature = skinTemperature.value,
                gyroX = gyro[0],
                gyroY = gyro[1],
                gyroZ = gyro[2]
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("MainViewModel", "onAccuracyChanged: sensor = ${sensor?.name}, accuracy = $accuracy")
    }

    override fun onCleared() {
        super.onCleared()
        healthServicesManager.unregisterForHeartRateData()
        healthServicesManager.stopSkinTemperatureMonitoring()
        stopGyroscope()
        durationUpdateJob?.cancel()
        recordingJob?.cancel()
    }
}
