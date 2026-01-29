package com.axon.presentation.screens.sessions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axon.domain.model.SensorData
import com.axon.domain.model.Session
import com.axon.domain.model.SessionStats
import com.axon.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

        fun loadSessionDetails(sessionId: Long) {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    _selectedSession.value = sessionRepository.getSession(sessionId)
                    _selectedSessionData.value = sessionRepository.getSensorDataBySession(sessionId)
                    _selectedSessionStats.value = sessionRepository.getSessionStats(sessionId)
                    Log.d(TAG, "Loaded session $sessionId with ${_selectedSessionData.value.size} data points")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load session details", e)
                } finally {
                    _isLoading.value = false
                }
            }
        }

        fun deleteSession(sessionId: Long) {
            viewModelScope.launch {
                try {
                    sessionRepository.deleteSession(sessionId)
                    Log.d(TAG, "Deleted session: $sessionId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete session", e)
                }
            }
        }

        fun clearSelectedSession() {
            _selectedSession.value = null
            _selectedSessionData.value = emptyList()
            _selectedSessionStats.value = null
        }
    }
