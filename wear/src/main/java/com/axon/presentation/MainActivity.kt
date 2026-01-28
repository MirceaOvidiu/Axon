package com.axon.presentation

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.axon.presentation.theme.AxonTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            // Permission result handled
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Request body sensors permission
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.BODY_SENSORS,
                Manifest.permission.BODY_SENSORS_BACKGROUND,
            ),
        )

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

    // Recording state
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDuration by viewModel.recordingDuration.collectAsState()
    val dataPointsRecorded by viewModel.dataPointsRecorded.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastSyncResult by viewModel.lastSyncResult.collectAsState()

    AxonTheme {
        MainScreen(
            heartRateBpm = heartRateBpm,
            availability = availability,
            gyroscopeData = gyroscopeData,
            isRecording = isRecording,
            recordingDuration = recordingDuration,
            dataPointsRecorded = dataPointsRecorded,
            isSyncing = isSyncing,
            lastSyncResult = lastSyncResult,
            onStartRecording = { viewModel.startRecording() },
            onStopRecording = { viewModel.stopRecording() },
            onSyncAll = { viewModel.syncAllUnsyncedSessions() },
        )
    }
}

@Composable
fun MainScreen(
    heartRateBpm: Double,
    availability: androidx.health.services.client.data.Availability?,
    gyroscopeData: FloatArray,
    isRecording: Boolean,
    recordingDuration: Long,
    dataPointsRecorded: Int,
    isSyncing: Boolean,
    lastSyncResult: String?,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onSyncAll: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Recording Control Button
        RecordingButton(
            isRecording = isRecording,
            isSyncing = isSyncing,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording,
        )

        // Recording Status
        if (isRecording) {
            Text(
                text = formatDuration(recordingDuration),
                textAlign = TextAlign.Center,
                color = Color.Red,
                style = MaterialTheme.typography.title2,
            )
            Text(
                text = "$dataPointsRecorded readings",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.caption2,
            )
        }
        // Sync Status
        if (isSyncing) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = "Syncing...",
                    style = MaterialTheme.typography.caption2,
                )
            }
        }

        lastSyncResult?.let { result ->
            Text(
                text = result,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.caption2,
                color = if (result.contains("failed", ignoreCase = true)) Color.Red else Color.Green,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Heart Rate
        Text(
            text = "Heart Rate:",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.title3,
        )
        Text(
            text =
                if (availability != null && heartRateBpm > 0) {
                    "%.0f BPM".format(heartRateBpm)
                } else {
                    "-- BPM"
                },
            textAlign = TextAlign.Center,
        )

        // Gyroscope
        Text(
            text = "Gyroscope:",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.title3,
        )
        Text(
            text =
                "X:%.2f Y:%.2f Z:%.2f".format(
                    gyroscopeData[0],
                    gyroscopeData[1],
                    gyroscopeData[2],
                ),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption2,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Manual sync button
        if (!isRecording && !isSyncing) {
            Button(
                onClick = onSyncAll,
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = ButtonDefaults.secondaryButtonColors(),
            ) {
                Text("Sync All")
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun RecordingButton(
    isRecording: Boolean,
    isSyncing: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
) {
    Button(
        onClick = {
            if (isRecording) onStopRecording() else onStartRecording()
        },
        enabled = !isSyncing,
        modifier = Modifier.fillMaxWidth(0.8f),
        colors =
            if (isRecording) {
                ButtonDefaults.buttonColors(backgroundColor = Color.Red)
            } else {
                ButtonDefaults.primaryButtonColors()
            },
    ) {
        Text(
            text = if (isRecording) "Stop Recording" else "Start Recording",
            textAlign = TextAlign.Center,
        )
    }
}

fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
