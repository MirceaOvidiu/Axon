package com.axon.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axon.R
import com.axon.models.SensorData
import com.axon.presentation.theme.AxonTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    viewModel: SessionViewModel,
    sessionId: Long,
    onNavigateBack: () -> Unit
) {
    val session by viewModel.selectedSession.collectAsState()
    val sensorData by viewModel.selectedSessionData.collectAsState()
    val stats by viewModel.selectedSessionStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.loadSessionDetails(sessionId)
    }

    AxonTheme(darkTheme = true) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = backgroundDark,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Session Details",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundDark
                    )
                )
            }
        ) { innerPadding ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = primaryColor)
                }
            } else if (session == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Session not found",
                        color = textMutedDark,
                        fontSize = 16.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Session info card
                    SessionInfoCard(
                        startTime = session!!.startTime,
                        endTime = session!!.endTime,
                        dataPointCount = session!!.dataPointCount
                    )

                    // Stats cards
                    stats?.let { sessionStats ->
                        StatsCard(stats = sessionStats)
                    }

                    // Heart rate chart
                    if (sensorData.isNotEmpty()) {
                        ChartCard(
                            title = "Heart Rate",
                            unit = "BPM",
                            data = sensorData.mapNotNull { it.heartRate?.toFloat() },
                            color = Color(0xFFFF6B6B)
                        )

                        // Skin temperature chart
                        val tempData = sensorData.mapNotNull { it.skinTemperature?.toFloat() }
                        if (tempData.isNotEmpty()) {
                            ChartCard(
                                title = "Skin Temperature",
                                unit = "°C",
                                data = tempData,
                                color = Color(0xFFFFA94D)
                            )
                        }

                        // Gyroscope chart
                        GyroscopeChartCard(sensorData = sensorData)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun SessionInfoCard(
    startTime: Long,
    endTime: Long,
    dataPointCount: Int
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val duration = endTime - startTime

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Start",
                        color = textMutedDark,
                        fontSize = 12.sp
                    )
                    Text(
                        text = dateFormat.format(Date(startTime)),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "End",
                        color = textMutedDark,
                        fontSize = 12.sp
                    )
                    Text(
                        text = dateFormat.format(Date(endTime)),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Duration",
                        color = textMutedDark,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatDuration(duration),
                        color = primaryColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Data Points",
                        color = textMutedDark,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "$dataPointCount",
                        color = primaryColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StatsCard(stats: com.axon.data.SessionStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Statistics",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Avg HR",
                    value = stats.avgHeartRate?.let { "%.0f".format(it) } ?: "--",
                    unit = "BPM",
                    color = Color(0xFFFF6B6B)
                )
                StatItem(
                    label = "Max HR",
                    value = stats.maxHeartRate?.let { "%.0f".format(it) } ?: "--",
                    unit = "BPM",
                    color = Color(0xFFFF6B6B)
                )
                StatItem(
                    label = "Min HR",
                    value = stats.minHeartRate?.let { "%.0f".format(it) } ?: "--",
                    unit = "BPM",
                    color = Color(0xFFFF6B6B)
                )
            }

            stats.avgSkinTemperature?.let { temp ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    StatItem(
                        label = "Avg Skin Temp",
                        value = "%.1f".format(temp),
                        unit = "°C",
                        color = Color(0xFFFFA94D)
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = textMutedDark,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = unit,
            color = textMutedDark,
            fontSize = 10.sp
        )
    }
}

@Composable
fun ChartCard(
    title: String,
    unit: String,
    data: List<Float>,
    color: Color
) {
    if (data.isEmpty()) return

    val minValue = data.minOrNull() ?: 0f
    val maxValue = data.maxOrNull() ?: 100f
    val range = (maxValue - minValue).coerceAtLeast(1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "%.0f - %.0f $unit".format(minValue, maxValue),
                    color = textMutedDark,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val width = size.width
                val height = size.height
                val stepX = width / (data.size - 1).coerceAtLeast(1)

                val path = Path()
                data.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = height - ((value - minValue) / range * height)

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )

                // Draw points
                data.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = height - ((value - minValue) / range * height)
                    drawCircle(
                        color = color,
                        radius = 4f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

@Composable
fun GyroscopeChartCard(sensorData: List<SensorData>) {
    val gyroX = sensorData.mapNotNull { it.gyroX }
    val gyroY = sensorData.mapNotNull { it.gyroY }
    val gyroZ = sensorData.mapNotNull { it.gyroZ }

    if (gyroX.isEmpty() && gyroY.isEmpty() && gyroZ.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Gyroscope",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = Color(0xFF4ECDC4), label = "X")
                LegendItem(color = Color(0xFFFFE66D), label = "Y")
                LegendItem(color = Color(0xFFFF6B6B), label = "Z")
            }

            Spacer(modifier = Modifier.height(12.dp))

            val allValues = gyroX + gyroY + gyroZ
            val minValue = allValues.minOrNull() ?: -1f
            val maxValue = allValues.maxOrNull() ?: 1f
            val range = (maxValue - minValue).coerceAtLeast(0.1f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val width = size.width
                val height = size.height
                val dataSize = maxOf(gyroX.size, gyroY.size, gyroZ.size)
                val stepX = width / (dataSize - 1).coerceAtLeast(1)

                // Draw X axis
                drawGyroLine(gyroX, Color(0xFF4ECDC4), stepX, height, minValue, range)
                // Draw Y axis
                drawGyroLine(gyroY, Color(0xFFFFE66D), stepX, height, minValue, range)
                // Draw Z axis
                drawGyroLine(gyroZ, Color(0xFFFF6B6B), stepX, height, minValue, range)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGyroLine(
    data: List<Float>,
    color: Color,
    stepX: Float,
    height: Float,
    minValue: Float,
    range: Float
) {
    if (data.isEmpty()) return

    val path = Path()
    data.forEachIndexed { index, value ->
        val x = index * stepX
        val y = height - ((value - minValue) / range * height)

        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2f, cap = StrokeCap.Round)
    )
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = label,
            color = textMutedDark,
            fontSize = 12.sp
        )
    }
}

private fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (hours > 0) {
        "%dh %02dm %02ds".format(hours, minutes, seconds)
    } else if (minutes > 0) {
        "%dm %02ds".format(minutes, seconds)
    } else {
        "%ds".format(seconds)
    }
}
