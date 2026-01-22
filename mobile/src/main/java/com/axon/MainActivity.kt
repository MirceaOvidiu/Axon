package com.axon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.axon.presentation.RecoveryDashboardScreen
import com.axon.presentation.theme.AxonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AxonTheme {
                RecoveryDashboardScreen()
            }
        }
    }
}
