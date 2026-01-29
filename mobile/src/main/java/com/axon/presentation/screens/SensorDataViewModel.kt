package com.axon.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axon.domain.repository.WearableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SensorDataViewModel
    @Inject
    constructor(
        private val wearableRepository: WearableRepository,
    ) : ViewModel() {
        val isConnected: StateFlow<Boolean> =
            wearableRepository
                .isDeviceConnected()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = false,
                )

        val connectedModelName: StateFlow<String?> =
            wearableRepository
                .getConnectedNodeName()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )

        init {
            refreshConnection()
        }

        fun refreshConnection() {
            viewModelScope.launch {
                wearableRepository.checkConnection()
            }
        }
    }
