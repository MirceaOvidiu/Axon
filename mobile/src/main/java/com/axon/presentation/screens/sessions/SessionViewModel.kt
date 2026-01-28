package com.axon.presentation.screens.sessions

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.axon.data.DataLayerEvents
import com.axon.data.SessionRepository
import com.axon.data.SessionStats
import com.axon.models.RecordingSession
import com.axon.models.SensorData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionRepository = SessionRepository(application)

    companion object {
        private const val TAG = "SessionViewModel"
    }

    // All sessions list
    val sessions: StateFlow<List<RecordingSession>> = sessionRepository.getAllSessionsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Selected session details
    private val _selectedSession = MutableStateFlow<RecordingSession?>(null)
    val selectedSession: StateFlow<RecordingSession?> = _selectedSession.asStateFlow()

    private val _selectedSessionData = MutableStateFlow<List<SensorData>>(emptyList())
    val selectedSessionData: StateFlow<List<SensorData>> = _selectedSessionData.asStateFlow()

    private val _selectedSessionStats = MutableStateFlow<SessionStats?>(null)
    val selectedSessionStats: StateFlow<SessionStats?> = _selectedSessionStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Listen for new sessions received from watch via SharedFlow
        viewModelScope.launch {
            DataLayerEvents.sessionReceivedEvents.collect { event ->
                Log.d(TAG, "Received session event: sessionId=${event.sessionId}, dataPoints=${event.dataPointCount}")
                // The Flow will automatically update when new data is added to the database
            }
        }
    }

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
