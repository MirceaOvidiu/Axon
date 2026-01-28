package com.axon.presentation.main

import androidx.health.services.client.data.Availability

data class MainUiState(
    // Sensor data
    val heartRateBpm: Double = 0.0,
    val heartRateAvailability: Availability? = null,
    val gyroscopeData: FloatArray = floatArrayOf(0f, 0f, 0f),
    // Recording state
    val isRecording: Boolean = false,
    val currentSessionId: Long? = null,
    val recordingDuration: Long = 0L,
    val dataPointsRecorded: Int = 0,
    // Sync state
    val isSyncing: Boolean = false,
    val lastSyncResult: String? = null,
    // Error state
    val errorMessage: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MainUiState

        if (heartRateBpm != other.heartRateBpm) return false
        if (heartRateAvailability != other.heartRateAvailability) return false
        if (!gyroscopeData.contentEquals(other.gyroscopeData)) return false
        if (isRecording != other.isRecording) return false
        if (currentSessionId != other.currentSessionId) return false
        if (recordingDuration != other.recordingDuration) return false
        if (dataPointsRecorded != other.dataPointsRecorded) return false
        if (isSyncing != other.isSyncing) return false
        if (lastSyncResult != other.lastSyncResult) return false
        if (errorMessage != other.errorMessage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = heartRateBpm.hashCode()
        result = 31 * result + (heartRateAvailability?.hashCode() ?: 0)
        result = 31 * result + gyroscopeData.contentHashCode()
        result = 31 * result + isRecording.hashCode()
        result = 31 * result + (currentSessionId?.hashCode() ?: 0)
        result = 31 * result + recordingDuration.hashCode()
        result = 31 * result + dataPointsRecorded.hashCode()
        result = 31 * result + isSyncing.hashCode()
        result = 31 * result + (lastSyncResult?.hashCode() ?: 0)
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        return result
    }
}

/**
 * Sealed interface for user intents/actions.
 * Makes all possible user actions explicit.
 */
sealed interface MainIntent {
    data object StartRecording : MainIntent

    data object StopRecording : MainIntent

    data object SyncAllSessions : MainIntent

    data object ClearError : MainIntent

    data object ClearSyncResult : MainIntent
}
