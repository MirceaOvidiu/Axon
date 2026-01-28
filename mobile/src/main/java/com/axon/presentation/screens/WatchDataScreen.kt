package com.axon.presentation.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axon.R
import com.axon.presentation.screens.dashboard.BottomNavigationBar
import com.axon.presentation.screens.dashboard.backgroundDark
import com.axon.presentation.screens.dashboard.cardDark
import com.axon.presentation.screens.dashboard.primaryColor
import com.axon.presentation.screens.dashboard.textMutedDark
import com.axon.presentation.theme.AxonTheme

@Composable
fun WatchDataScreen(
    viewModel: SensorDataViewModel,
    onNavigateBack: () -> Unit = {}
) {

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
            bottomBar = {
                BottomNavigationBar(
                    onNavigateToWatchData = {},
                    onNavigateToSessions = {})
            }
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
                    color = Color(0xFF90A4AE),
                    fontSize = 12.sp
                )
            }
        }
    }
}

