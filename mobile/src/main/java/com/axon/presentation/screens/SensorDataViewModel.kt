package com.axon.presentation.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.axon.data.WearableDataService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SensorDataViewModel(application: Application) : AndroidViewModel(application) {
    private val wearableDataService = WearableDataService(application)

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
