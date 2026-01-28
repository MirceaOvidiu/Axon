package com.axon.presentation.screens.dashboard

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axon.R
import com.axon.presentation.theme.AxonTheme

// Deep blue, black, and white color palette
val primaryColor = Color(0xFF2196F3) // Deep blue (primary accent)
val backgroundDark = Color(0xFF0A0E14) // Near black background
val cardDark = Color(0xFF151C25) // Dark blue-grey for cards
val textMutedDark = Color(0xFF8BA4C7) // Muted blue-grey for secondary text
val progressTrackColor = Color(0xFF1E3A5F) // Dark blue for progress tracks

@Suppress("ktlint:standard:function-naming")
@Composable
fun RecoveryDashboardScreen() {
    AxonTheme(darkTheme = true) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = backgroundDark,
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
            ) {
                TopHeader()
                MainProgressCard()
                KeyMetricsSection()
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun TopHeader() {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_health_and_safety_24),
                contentDescription = "Health Icon",
                tint = primaryColor,
                modifier = Modifier.size(36.dp),
            )
            IconButton(onClick = { /* TODO: Profile action */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_person_24),
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Good Morning Varu'",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Today is a new day for progress.",
            color = textMutedDark,
            fontSize = 14.sp,
        )
    }
}

@Composable
fun MainProgressCard() {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardDark),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Today's Goal: 20 mins of activity", color = Color.White, fontSize = 16.sp)
                Text("75%", color = primaryColor, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(progressTrackColor, shape = RoundedCornerShape(4.dp)),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.75f)
                            .height(8.dp)
                            .background(primaryColor, shape = RoundedCornerShape(4.dp)),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "You're doing great! Just 5 more minutes to go.",
                color = textMutedDark,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
fun KeyMetricsSection() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Key Metrics",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MetricCard(
                title = "Active Time",
                value = "15 mins",
                change = "+5%",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                title = "Repetitions",
                value = "150",
                change = "+10%",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        MetricCard(
            title = "Range of Motion",
            value = "85°",
            change = "+2° vs yesterday",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    change: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardDark.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, progressTrackColor),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, color = Color.White, fontSize = 16.sp)
            Text(text = value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(text = change, color = Color(0xFF64B5F6), fontSize = 16.sp)
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Preview(showBackground = true, backgroundColor = 0xFF102219)
@Composable
private fun RecoveryDashboardScreenPreview() {
    RecoveryDashboardScreen()
}
