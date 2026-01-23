package com.axon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.axon.presentation.RecoveryDashboardScreen
import com.axon.presentation.SensorDataViewModel
import com.axon.presentation.WatchDataScreen
import com.axon.presentation.theme.AxonTheme

class MainActivity : ComponentActivity() {
    private val sensorDataViewModel: SensorDataViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AxonTheme {
                var currentScreen by remember { mutableStateOf("dashboard") }

                when (currentScreen) {
                    "dashboard" -> RecoveryDashboardScreen(
                        onNavigateToWatchData = { currentScreen = "watch" }
                    )
                    "watch" -> WatchDataScreen(
                        viewModel = sensorDataViewModel,
                        onNavigateBack = { currentScreen = "dashboard" }
                    )
                }
            }
        }
    }
}
