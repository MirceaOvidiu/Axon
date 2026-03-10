package com.axon.presentation.screens.sessions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axon.domain.model.SensorData
import com.axon.domain.model.Session
import com.axon.domain.model.SessionStats
import com.axon.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel
    @Inject
    constructor(
        private val sessionRepository: SessionRepository,
    ) : ViewModel() {
        companion object {
            private const val TAG = "SessionViewModel"
        }

        // This will hold the session with the latest updates from the cloud
        private val _cloudSession = MutableStateFlow<Session?>(null)
        val cloudSession: StateFlow<Session?> = _cloudSession.asStateFlow()

        private val _isPolling = MutableStateFlow(false)
        val isPolling: StateFlow<Boolean> = _isPolling.asStateFlow()

        init {
            // Sync cloud sessions when the view model is created
            viewModelScope.launch {
                try {
                    sessionRepository.syncSessionsFromCloud()
                } catch (e: Exception) {
                    Log.e(TAG, "Cloud sync failed", e)
                }
            }
        }

        // All sessions list
        val sessions: StateFlow<List<Session>> =
            sessionRepository
                .getAllSessions()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )

        // Selected session details
        private val _selectedSession = MutableStateFlow<Session?>(null)
        val selectedSession: StateFlow<Session?> = _selectedSession.asStateFlow()

        private val _selectedSessionData = MutableStateFlow<List<SensorData>>(emptyList())
        val selectedSessionData: StateFlow<List<SensorData>> = _selectedSessionData.asStateFlow()

        private val _selectedSessionStats = MutableStateFlow<SessionStats?>(null)
        val selectedSessionStats: StateFlow<SessionStats?> = _selectedSessionStats.asStateFlow()

        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

        private val _deletingSessionId = MutableStateFlow<Long?>(null)
        val deletingSessionId: StateFlow<Long?> = _deletingSessionId.asStateFlow()

        fun loadSessionDetails(sessionId: Long) {
            viewModelScope.launch {
                _isLoading.value = true
                _isPolling.value = false // Reset polling state
                _cloudSession.value = null // Reset cloud session state
                try {
                    // First, load and display whatever data is available locally
                    val localSession = sessionRepository.getSession(sessionId)
                    _selectedSession.value = localSession
                    _selectedSessionData.value = sessionRepository.getSensorDataBySession(sessionId)
                    _selectedSessionStats.value = sessionRepository.getSessionStats(sessionId)
                    Log.d(TAG, "Loaded initial session details for $sessionId")

                    // If the local session already has scores, update the cloud session state
                    if (localSession?.sparcScore != null || localSession?.hrvScore != null) {
                        _cloudSession.value = localSession
                    } else {
                        // Otherwise, start polling for cloud scores in a non-blocking way
                        pollForAnalysisScores(sessionId)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load session details", e)
                } finally {
                    _isLoading.value = false
                }
            }
        }

        private fun pollForAnalysisScores(sessionId: Long) {
            viewModelScope.launch {
                _isPolling.value = true
                try {
                    // Initial sync before polling
                    sessionRepository.syncSessionsFromCloud()
                    var localSession = sessionRepository.getSession(sessionId)
                    if (localSession != null) _cloudSession.value = localSession

                    // Polling loop — always update UI on every tick, break early only when all
                    // movement scores (SPARC + LDLJ) are present. HRV may not exist for every session.
                    for (i in 0..9) {
                        delay(2000) // 2-second interval = 20 seconds max
                        sessionRepository.syncSessionsFromCloud()
                        localSession = sessionRepository.getSession(sessionId)
                        if (localSession != null) _cloudSession.value = localSession
                        if (localSession?.sparcScore != null && localSession?.ldljScore != null) {
                            Log.d(TAG, "All movement scores found after ${i + 1} poll(s).")
                            break
                        }
                    }
                    Log.d(TAG, "Polling finished.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error while polling for scores", e)
                } finally {
                    _isPolling.value = false
                }
            }
        }

        fun deleteSession(sessionId: Long) {
            viewModelScope.launch {
                _deletingSessionId.value = sessionId
                try {
                    sessionRepository.deleteSession(sessionId)
                    Log.d(TAG, "Deleted session: $sessionId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete session", e)
                } finally {
                    _deletingSessionId.value = null
                }
            }
        }

        fun clearSelectedSession() {
            _selectedSession.value = null
            _selectedSessionData.value = emptyList()
            _selectedSessionStats.value = null
            _cloudSession.value = null
            _isPolling.value = false
        }
    }
