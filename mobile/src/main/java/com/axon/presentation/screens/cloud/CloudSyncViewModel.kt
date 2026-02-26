package com.axon.presentation.screens.cloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axon.domain.model.Session
import com.axon.domain.repository.AuthRepository
import com.axon.domain.repository.CloudSessionRepository
import com.axon.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudSyncViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val cloudSessionRepository: CloudSessionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CloudSyncUiState())
    val uiState: StateFlow<CloudSyncUiState> = _uiState.asStateFlow()

    val uploadProgress = cloudSessionRepository.getUploadProgress()
    val downloadProgress = cloudSessionRepository.getDownloadProgress()
    val currentUser = authRepository.currentUser

    init {
        loadLocalSessions()
        loadCloudSessions()
    }

    private fun loadLocalSessions() {
        viewModelScope.launch {
            try {
                sessionRepository.getAllSessions().collect { sessions ->
                    _uiState.value = _uiState.value.copy(localSessions = sessions)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load local sessions: ${e.message}"
                )
            }
        }
    }

    private fun loadCloudSessions() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoadingCloud = true)
                val cloudSessions = cloudSessionRepository.downloadAllSessions()
                _uiState.value = _uiState.value.copy(
                    cloudSessions = cloudSessions,
                    isLoadingCloud = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load cloud sessions: ${e.message}",
                    isLoadingCloud = false
                )
            }
        }
    }

    fun uploadSession(sessionId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isUploading = true)

                val session = sessionRepository.getSession(sessionId)
                val sensorData = sessionRepository.getSensorDataBySession(sessionId)

                if (session != null) {
                    val success = cloudSessionRepository.uploadSession(session, sensorData)
                    if (success) {
                        // Refresh cloud sessions
                        loadCloudSessions()
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Session uploaded successfully"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Failed to upload session"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Session not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Upload failed: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isUploading = false)
            }
        }
    }

    fun downloadSession(sessionId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isDownloading = true)

                val session = cloudSessionRepository.downloadSession(sessionId)
                val sensorData = cloudSessionRepository.downloadSensorData(sessionId)

                if (session != null) {
                    // Save to local database
                    sessionRepository.insertSession(session)
                    // Note: You'd need to add a method to insert sensor data

                    _uiState.value = _uiState.value.copy(
                        successMessage = "Session downloaded successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to download session"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Download failed: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isDownloading = false)
            }
        }
    }

    fun uploadAllSessions() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSyncing = true)

                val localSessions = _uiState.value.localSessions
                var uploaded = 0
                var failed = 0

                for (session in localSessions) {
                    try {
                        val sensorData = sessionRepository.getSensorDataBySession(session.id)
                        val success = cloudSessionRepository.uploadSession(session, sensorData)
                        if (success) {
                            uploaded++
                        } else {
                            failed++
                        }
                    } catch (e: Exception) {
                        failed++
                    }
                }

                // Refresh cloud sessions
                loadCloudSessions()

                _uiState.value = _uiState.value.copy(
                    successMessage = "Sync complete: $uploaded uploaded, $failed failed"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Sync failed: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isSyncing = false)
            }
        }
    }

    fun deleteCloudSession(sessionId: String) {
        viewModelScope.launch {
            try {
                val success = cloudSessionRepository.deleteCloudSession(sessionId)
                if (success) {
                    loadCloudSessions()
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Session deleted from cloud"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to delete session from cloud"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Delete failed: ${e.message}"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}

data class CloudSyncUiState(
    val localSessions: List<Session> = emptyList(),
    val cloudSessions: List<Session> = emptyList(),
    val isUploading: Boolean = false,
    val isDownloading: Boolean = false,
    val isSyncing: Boolean = false,
    val isLoadingCloud: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
