package com.axon.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

val primaryColor = Color(0xFF13EC80)
val backgroundDark = Color(0xFF102219)
val cardDark = Color(0xFF1A2D23)
val textMutedDark = Color(0xFF92C9AD)
val progressTrackColor = Color(0xFF32674d)

@Composable
fun RecoveryDashboardScreen(onNavigateToWatchData: () -> Unit = {}) {
    AxonTheme(darkTheme = true) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = backgroundDark,
            bottomBar = { BottomNavigationBar(onNavigateToWatchData = onNavigateToWatchData) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                TopHeader()
                MainProgressCard()
                KeyMetricsSection()
                TodaysPlanSection()
                WeeklyProgressSection()
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_health_and_safety_24),
                contentDescription = "Health Icon",
                tint = primaryColor,
                modifier = Modifier.size(36.dp)
            )
            IconButton(onClick = { /* TODO: Profile action */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_person_24),
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Good Morning, Alex!",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Today is a new day for progress.",
            color = textMutedDark,
            fontSize = 14.sp
        )
    }
}
@Composable
fun MainProgressCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Today's Goal: 20 mins of activity", color = Color.White, fontSize = 16.sp)
                Text("75%", color = primaryColor, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(progressTrackColor, shape = RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(8.dp)
                        .background(primaryColor, shape = RoundedCornerShape(4.dp))
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "You're doing great! Just 5 more minutes to go.",
                color = textMutedDark,
                fontSize = 14.sp
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
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(title = "Active Time", value = "15 mins", change = "+5%", modifier = Modifier.weight(1f))
            MetricCard(title = "Repetitions", value = "150", change = "+10%", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))
        MetricCard(title = "Range of Motion", value = "85°", change = "+2° vs yesterday", modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun MetricCard(title: String, value: String, change: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardDark.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, progressTrackColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, color = Color.White, fontSize = 16.sp)
            Text(text = value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(text = change, color = Color(0xFF0BDA46), fontSize = 16.sp)
        }
    }
}

@Composable
fun TodaysPlanSection() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Today's Plan",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        PlanItem(
            iconRes = R.drawable.outline_arrow_top_right_24,
            title = "Wrist Extension",
            details = "2 sets of 15"
        )
        Spacer(modifier = Modifier.height(12.dp))
        PlanItem(
            iconRes = R.drawable.outline_arrow_top_left_24,
            title = "Object Grasping",
            details = "10 mins"
        )
    }
}

@Composable
fun PlanItem(iconRes: Int, title: String, details: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardDark)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(text = details, color = textMutedDark, fontSize = 14.sp)
                }
            }
            Button(
                onClick = { /* TODO: Start Exercise */ },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black)
            ) {
                Text("Start", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WeeklyProgressSection() {
    val progressData = mapOf(
        "Mon" to 0.4f, "Tue" to 0.6f, "Wed" to 0.5f, "Thu" to 0.8f,
        "Fri" to 0.7f, "Sat" to 0.9f, "Sun" to 0.75f
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Weekly Progress",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Active Time (minutes)", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    progressData.forEach { (day, progress) ->
                        Bar(day = day, progress = progress, isCurrentDay = day == "Sun")
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.Bar(day: String, progress: Float, isCurrentDay: Boolean) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .width(20.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(progress)
                    .width(20.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(if (isCurrentDay) primaryColor else primaryColor.copy(alpha = 0.3f))
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = day,
            color = if (isCurrentDay) primaryColor else textMutedDark,
            fontSize = 12.sp,
            fontWeight = if (isCurrentDay) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun BottomNavigationBar(onNavigateToWatchData: () -> Unit = {}) {
    NavigationBar(
        containerColor = backgroundDark.copy(alpha = 0.8f),
        tonalElevation = 0.dp,
    ) {
        NavigationBarItem(
            selected = true,
            onClick = { /* TODO */ },
            icon = { Icon(painterResource(R.drawable.outline_dashboard_24), contentDescription = "Dashboard") },
            label = { Text("Dashboard") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = primaryColor,
                selectedTextColor = primaryColor,
                unselectedIconColor = textMutedDark,
                unselectedTextColor = textMutedDark,
                indicatorColor = cardDark
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* TODO */ },
            icon = { Icon(painterResource(R.drawable.outline_exercise_24), contentDescription = "Exercises") },
            label = { Text("Exercises") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = primaryColor,
                selectedTextColor = primaryColor,
                unselectedIconColor = textMutedDark,
                unselectedTextColor = textMutedDark,
                indicatorColor = cardDark
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* TODO */ },
            icon = { Icon(painterResource(R.drawable.outline_bar_chart_24), contentDescription = "Progress") },
            label = { Text("Progress") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = primaryColor,
                selectedTextColor = primaryColor,
                unselectedIconColor = textMutedDark,
                unselectedTextColor = textMutedDark,
                indicatorColor = cardDark
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = onNavigateToWatchData,
            icon = { Icon(painterResource(R.drawable.outline_display_settings_24), contentDescription = "Watch Data") },
            label = { Text("Watch") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = primaryColor,
                selectedTextColor = primaryColor,
                unselectedIconColor = textMutedDark,
                unselectedTextColor = textMutedDark,
                indicatorColor = cardDark
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF102219)
@Composable
private fun RecoveryDashboardScreenPreview() {
    RecoveryDashboardScreen()
}
