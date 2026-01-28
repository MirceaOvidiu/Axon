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
import com.axon.presentation.SessionDetailScreen
import com.axon.presentation.SessionListScreen
import com.axon.presentation.SessionViewModel
import com.axon.presentation.WatchDataScreen
import com.axon.presentation.theme.AxonTheme

class MainActivity : ComponentActivity() {
    private val sensorDataViewModel: SensorDataViewModel by viewModels()
    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AxonTheme {
                var currentScreen by remember { mutableStateOf("dashboard") }
                var selectedSessionId by remember { mutableStateOf<Long?>(null) }

                when (currentScreen) {
                    "dashboard" -> RecoveryDashboardScreen(
                        onNavigateToWatchData = { currentScreen = "watch" },
                        onNavigateToSessions = { currentScreen = "sessions" }
                    )
                    "watch" -> WatchDataScreen(
                        viewModel = sensorDataViewModel,
                        onNavigateBack = { currentScreen = "dashboard" }
                    )
                    "sessions" -> SessionListScreen(
                        viewModel = sessionViewModel,
                        onNavigateBack = { currentScreen = "dashboard" },
                        onSessionClick = { sessionId ->
                            selectedSessionId = sessionId
                            currentScreen = "session_detail"
                        }
                    )
                    "session_detail" -> selectedSessionId?.let { sessionId ->
                        SessionDetailScreen(
                            viewModel = sessionViewModel,
                            sessionId = sessionId,
                            onNavigateBack = {
                                sessionViewModel.clearSelectedSession()
                                currentScreen = "sessions"
                            }
                        )
                    }
                }
            }
        }
    }
}
