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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.curvedText
import com.axon.presentation.theme.AxonTheme
import dagger.hilt.android.AndroidEntryPoint

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

    Scaffold(
        vignette = {
            Vignette(vignettePosition = VignettePosition.TopAndBottom)
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            // Curved BPM text at top
            CurvedLayout(
                modifier = Modifier.fillMaxSize(),
                anchor = 270f,
                angularDirection = CurvedDirection.Angular.Normal
            ) {
                curvedRow {
                    curvedText(
                        text = if (uiState.heartRateBpm > 0) {
                            "♥ %.0f BPM".format(uiState.heartRateBpm)
                        } else {
                            "♥ -- BPM"
                        },
                        style = CurvedTextStyle(
                            fontSize = 16.sp,
                            color = Color(0xFFFF6B6B),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Curved Gyro text at bottom
            CurvedLayout(
                modifier = Modifier.fillMaxSize(),
                anchor = 90f, // Slightly higher from bottom
                angularDirection = CurvedDirection.Angular.Reversed
            ) {
                curvedRow {
                    curvedText(
                        text = String.format(
                            java.util.Locale.US,
                            "X:%+05.1f Y:%+05.1f Z:%+05.1f",
                            uiState.gyroscopeData[0],
                            uiState.gyroscopeData[1],
                            uiState.gyroscopeData[2]
                        ),
                        style = CurvedTextStyle(
                            fontSize = 13.sp,
                            color = Color(0xFF8BA4C7),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    )
                }
            }

            // Central content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                // Recording button
                CircularRecordingButton(
                    isRecording = uiState.isRecording,
                    isSyncing = uiState.isSyncing,
                    onStartRecording = { onIntent(MainIntent.StartRecording) },
                    onStopRecording = { onIntent(MainIntent.StopRecording) },
                )

                // Sync indicator
                if (uiState.isSyncing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            indicatorColor = MaterialTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Syncing...",
                            style = MaterialTheme.typography.caption2,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }

                // Error message
                uiState.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.caption3,
                        color = Color(0xFFFF6B6B),
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }

                // Recording status indicator
                if (uiState.isRecording) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Canvas(modifier = Modifier.size(8.dp)) {
                            drawCircle(color = Color(0xFFE53E3E))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REC",
                            style = MaterialTheme.typography.caption2,
                            color = Color(0xFFE53E3E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
    val buttonColor = if (isRecording) {
        Color(0xFFE53E3E) // Red for recording/stop
    } else {
        Color(0xFF38A169) // Green for start
    }

    Box(
        contentAlignment = Alignment.Center
    ) {
        // Outer ring indicator when recording
        if (isRecording) {
            CircularProgressIndicator(
                modifier = Modifier.size(96.dp),
                strokeWidth = 4.dp,
                indicatorColor = Color(0xFFE53E3E).copy(alpha = 0.5f),
                trackColor = Color.Transparent
            )
        }

        Button(
            onClick = {
                if (isRecording) onStopRecording() else onStartRecording()
            },
            enabled = !isSyncing,
            modifier = Modifier.size(72.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = buttonColor,
                disabledBackgroundColor = buttonColor.copy(alpha = 0.5f)
            ),
            shape = androidx.compose.foundation.shape.CircleShape,
        ) {
            if (isRecording) {
                StopIcon(
                    modifier = Modifier.size(28.dp),
                    color = Color.White,
                )
            } else {
                PlayIcon(
                    modifier = Modifier.size(28.dp),
                    color = Color.White,
                )
            }
        }
    }
}

