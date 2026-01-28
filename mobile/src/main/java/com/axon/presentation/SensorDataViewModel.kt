package com.axon.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.axon.data.WearableDataService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SensorDataViewModel(application: Application) : AndroidViewModel(application) {
    private val wearableDataService = WearableDataService(application)

    val heartRateBpm: StateFlow<Double> = wearableDataService.heartRateBpm
    val gyroscopeX: StateFlow<Float> = wearableDataService.gyroscopeX
    val gyroscopeY: StateFlow<Float> = wearableDataService.gyroscopeY
    val gyroscopeZ: StateFlow<Float> = wearableDataService.gyroscopeZ
    val isConnected: StateFlow<Boolean> = wearableDataService.isConnected

    init {
        wearableDataService.startListening()
        viewModelScope.launch {
            wearableDataService.requestDataSync()
        }
    }

    fun refreshConnection() {
        viewModelScope.launch {
            wearableDataService.requestDataSync()
        }
    }

    override fun onCleared() {
        super.onCleared()
        wearableDataService.stopListening()
    }
}
