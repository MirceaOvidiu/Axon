package com.axon.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axon.R
import com.axon.presentation.theme.AxonTheme

@Composable
fun WatchDataScreen(
    viewModel: SensorDataViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val heartRateBpm by viewModel.heartRateBpm.collectAsState()
    val gyroscopeX by viewModel.gyroscopeX.collectAsState()
    val gyroscopeY by viewModel.gyroscopeY.collectAsState()
    val gyroscopeZ by viewModel.gyroscopeZ.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()

    AxonTheme(darkTheme = true) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = backgroundDark,
            topBar = {
                WatchDataTopBar(
                    isConnected = isConnected,
                    onRefresh = { viewModel.refreshConnection() },
                    onNavigateBack = onNavigateBack
                )
            },
            bottomBar = { BottomNavigationBar(onNavigateToWatchData = onNavigateBack) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ConnectionStatusCard(isConnected = isConnected)

                HeartRateCard(heartRateBpm = heartRateBpm)

                GyroscopeCard(
                    gyroscopeX = gyroscopeX,
                    gyroscopeY = gyroscopeY,
                    gyroscopeZ = gyroscopeZ
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchDataTopBar(isConnected: Boolean, onRefresh: () -> Unit, onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_health_and_safety_24),
                    contentDescription = "Watch Data",
                    tint = primaryColor,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Watch Data",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_dashboard_24),
                    contentDescription = "Back",
                    tint = primaryColor
                )
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_display_settings_24),
                    contentDescription = "Refresh",
                    tint = if (isConnected) primaryColor else textMutedDark
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundDark
        )
    )
}

@Composable
fun ConnectionStatusCard(isConnected: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) cardDark else cardDark.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) primaryColor else Color.Red)
                )
                Column {
                    Text(
                        text = "Watch Connection",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isConnected) "Connected" else "Disconnected",
                        color = textMutedDark,
                        fontSize = 14.sp
                    )
                }
            }
            if (!isConnected) {
                Text(
                    text = "Check your watch",
                    color = Color(0xFFFF6B6B),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun HeartRateCard(heartRateBpm: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardDark)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_health_and_safety_24),
                    contentDescription = "Heart Rate",
                    tint = Color(0xFFFF6B6B),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Heart Rate",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (heartRateBpm > 0) "%.1f".format(heartRateBpm) else "--",
                    color = primaryColor,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "BPM",
                    color = textMutedDark,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (heartRateBpm == 0.0) {
                Text(
                    text = "Waiting for data from watch...",
                    color = textMutedDark,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun GyroscopeCard(gyroscopeX: Float, gyroscopeY: Float, gyroscopeZ: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardDark)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_display_settings_24),
                    contentDescription = "Gyroscope",
                    tint = Color(0xFF4A9DFF),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Gyroscope",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            GyroscopeAxis(
                label = "X-Axis",
                value = gyroscopeX,
                color = Color(0xFFFF6B6B)
            )

            GyroscopeAxis(
                label = "Y-Axis",
                value = gyroscopeY,
                color = Color(0xFF4A9DFF)
            )

            GyroscopeAxis(
                label = "Z-Axis",
                value = gyroscopeZ,
                color = primaryColor
            )

            if (gyroscopeX == 0f && gyroscopeY == 0f && gyroscopeZ == 0f) {
                Text(
                    text = "Waiting for data from watch...",
                    color = textMutedDark,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun GyroscopeAxis(label: String, value: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = textMutedDark,
                fontSize = 14.sp
            )
            Text(
                text = "%.2f rad/s".format(value),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(progressTrackColor, shape = RoundedCornerShape(4.dp))
        ) {
            val progress = (value.coerceIn(-5f, 5f) + 5f) / 10f
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .background(color, shape = RoundedCornerShape(4.dp))
            )
        }
    }
}

