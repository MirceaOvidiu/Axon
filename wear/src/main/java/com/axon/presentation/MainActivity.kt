package com.axon.presentation

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.axon.presentation.theme.AxonTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Permission result handled
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)

        setContent {
            WearApp(viewModel)
        }
    }
}

@Composable
fun WearApp(viewModel: MainViewModel) {
    val heartRateBpm by viewModel.heartRateBpm.collectAsState()
    val availability by viewModel.availability.collectAsState()
    val gyroscopeData by viewModel.gyroscopeData.collectAsState()

    AxonTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Gyroscope (rad/s):",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title3
            )
            Text(
                text = "X: %.2f".format(gyroscopeData[0]),
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Y: %.2f".format(gyroscopeData[1]),
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Z: %.2f".format(gyroscopeData[2]),
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Heart Rate:",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title3
            )
            Text(
                text = if (availability != null) {
                    "%.1f BPM".format(heartRateBpm)
                } else {
                    "Unavailable"
                },
                textAlign = TextAlign.Center,
            )
        }
    }
}