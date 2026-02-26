package com.axon.presentation.main

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.axon.presentation.theme.AxonTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
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
    val uiState by viewModel.uiState.collectAsState()

    AxonTheme {
        MainScreen(
            uiState = uiState,
            onIntent = viewModel::onIntent,
        )
    }
}

@Composable
fun MainScreen(
    uiState: MainUiState,
    onIntent: (MainIntent) -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        // BPM at top of circle
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text =
                    if (uiState.heartRateAvailability != null && uiState.heartRateBpm > 0) {
                        "%.0f".format(uiState.heartRateBpm)
                    } else {
                        "--"
                    },
                style = MaterialTheme.typography.display1,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "BPM",
                style = MaterialTheme.typography.caption1,
                textAlign = TextAlign.Center,
            )
        }

        // Gyro data at bottom of circle - using monospace and fixed width formatting
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = String.format(java.util.Locale.US, "X:%+6.2f", uiState.gyroscopeData[0]),
                style = MaterialTheme.typography.caption2,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            )
            Text(
                text = String.format(java.util.Locale.US, "Y:%+6.2f", uiState.gyroscopeData[1]),
                style = MaterialTheme.typography.caption2,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            )
            Text(
                text = String.format(java.util.Locale.US, "Z:%+6.2f", uiState.gyroscopeData[2]),
                style = MaterialTheme.typography.caption2,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            )
        }

        // Central recording button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularRecordingButton(
                isRecording = uiState.isRecording,
                isSyncing = uiState.isSyncing,
                onStartRecording = { onIntent(MainIntent.StartRecording) },
                onStopRecording = { onIntent(MainIntent.StopRecording) },
            )

            // Show recording duration when recording
            if (uiState.isRecording) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDuration(uiState.recordingDuration),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption1,
                )
            }

            // Show sync status
            if (uiState.isSyncing) {
                Spacer(modifier = Modifier.height(4.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
            }

            // Show errors
            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption3,
                    color = Color.Red,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
fun PlayIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
) {
    Canvas(modifier = modifier) {
        val path =
            Path().apply {
                val width = size.width
                val height = size.height
                moveTo(width * 0.2f, height * 0.15f)
                lineTo(width * 0.85f, height * 0.5f)
                lineTo(width * 0.2f, height * 0.85f)
                close()
            }
        drawPath(path, color)
    }
}

@Composable
fun StopIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
) {
    Canvas(modifier = modifier) {
        drawRect(
            color = color,
            topLeft = Offset(size.width * 0.2f, size.height * 0.2f),
            size = Size(size.width * 0.6f, size.height * 0.6f),
        )
    }
}

@Composable
fun CircularRecordingButton(
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
        modifier = Modifier.size(80.dp),
        colors =
            if (isRecording) {
                ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE53E3E)) // Red for stop
            } else {
                ButtonDefaults.buttonColors(backgroundColor = Color(0xFF38A169)) // Green for start
            },
        shape = androidx.compose.foundation.shape.CircleShape,
    ) {
        if (isRecording) {
            StopIcon(
                modifier = Modifier.size(24.dp),
                color = Color.White,
            )
        } else {
            PlayIcon(
                modifier = Modifier.size(24.dp),
                color = Color.White,
            )
        }
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
