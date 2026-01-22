/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.axon.presentation

import android.os.Bundle
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.axon.presentation.theme.AxonTheme
import com.axon.senzors.SensorViewModel

class MainActivity : ComponentActivity() {
    private val sensorViewModel: SensorViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted! The ViewModel's sensor listener
            // will now start receiving actual data.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        requestPermissionLauncher.launch(android.Manifest.permission.BODY_SENSORS)

        setContent {
            WearApp(sensorViewModel)
        }
    }
}

@Composable
fun WearApp(sensorViewModel: SensorViewModel) {
    val gyroscopeData by sensorViewModel.gyroscopeData.collectAsState()
    val heartRateData by sensorViewModel.heartRateData.collectAsState()

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
                text = "%.1f BPM".format(heartRateData),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
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
                text = "X: 0.00",
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Y: 0.00",
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Z: 0.00",
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Heart Rate:",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title3
            )
            Text(
                text = "0.0 BPM",
                textAlign = TextAlign.Center,
            )
        }
    }
}