package com.axon.presentation.screens.cloud

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axon.R
import com.axon.domain.model.Session
import com.axon.presentation.screens.dashboard.backgroundDark
import com.axon.presentation.screens.dashboard.cardDark
import com.axon.presentation.screens.dashboard.primaryColor
import com.axon.presentation.screens.dashboard.textMutedDark
import com.axon.presentation.theme.AxonTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(
    viewModel: CloudSyncViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState(initial = 0f)
    val downloadProgress by viewModel.downloadProgress.collectAsState(initial = 0f)
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    AxonTheme(darkTheme = true) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = backgroundDark,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.outline_arrow_top_right_24),
                                contentDescription = "Cloud Sync",
                                tint = primaryColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Cloud Sync",
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundDark
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // User info
                currentUser?.let { user ->
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
                                text = "Signed in as:",
                                color = textMutedDark,
                                fontSize = 14.sp
                            )
                            Text(
                                text = user.displayName ?: user.email,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = user.email,
                                color = textMutedDark,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress indicators
                if (uiState.isUploading || uploadProgress > 0f) {
                    ProgressCard(
                        title = "Uploading...",
                        progress = uploadProgress,
                        isIndeterminate = uiState.isUploading && uploadProgress == 0f
                    )
                }

                if (uiState.isDownloading || downloadProgress > 0f) {
                    ProgressCard(
                        title = "Downloading...",
                        progress = downloadProgress,
                        isIndeterminate = uiState.isDownloading && downloadProgress == 0f
                    )
                }

                if (uiState.isSyncing) {
                    ProgressCard(
                        title = "Syncing all sessions...",
                        progress = 0f,
                        isIndeterminate = true
                    )
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.uploadAllSessions() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        enabled = !uiState.isSyncing && !uiState.isUploading
                    ) {
                        Text("Upload All")
                    }

                    Button(
                        onClick = { /* Refresh cloud sessions */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Refresh")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tabs
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = cardDark,
                    contentColor = primaryColor
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Local Sessions (${uiState.localSessions.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Cloud Sessions (${uiState.cloudSessions.size})") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Session lists
                when (selectedTab) {
                    0 -> LocalSessionsList(
                        sessions = uiState.localSessions,
                        onUploadSession = viewModel::uploadSession,
                        isUploading = uiState.isUploading
                    )
                    1 -> CloudSessionsList(
                        sessions = uiState.cloudSessions,
                        onDownloadSession = viewModel::downloadSession,
                        onDeleteSession = viewModel::deleteCloudSession,
                        isLoading = uiState.isLoadingCloud
                    )
                }

                // Messages
                uiState.errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = message,
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                uiState.successMessage?.let { message ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.Green.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = message,
                            color = Color.Green,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(
    title: String,
    progress: Float,
    isIndeterminate: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = cardDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isIndeterminate) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = primaryColor
                )
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = primaryColor,
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = textMutedDark,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun LocalSessionsList(
    sessions: List<Session>,
    onUploadSession: (Long) -> Unit,
    isUploading: Boolean
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sessions) { session ->
            SessionCard(
                session = session,
                actionText = "Upload",
                onAction = { onUploadSession(session.id) },
                actionEnabled = !isUploading,
                showDelete = false,
                onDelete = {}
            )
        }

        if (sessions.isEmpty()) {
            item {
                EmptyStateCard("No local sessions found")
            }
        }
    }
}

@Composable
private fun CloudSessionsList(
    sessions: List<Session>,
    onDownloadSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = primaryColor)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sessions) { session ->
                SessionCard(
                    session = session,
                    actionText = "Download",
                    onAction = { onDownloadSession(session.id.toString()) },
                    actionEnabled = true,
                    showDelete = true,
                    onDelete = { onDeleteSession(session.id.toString()) }
                )
            }

            if (sessions.isEmpty()) {
                item {
                    EmptyStateCard("No cloud sessions found")
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: Session,
    actionText: String,
    onAction: () -> Unit,
    actionEnabled: Boolean,
    showDelete: Boolean,
    onDelete: () -> Unit
) {
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatDateTime(session.startTime),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Duration: ${formatDuration(session.endTime - session.startTime)}",
                        color = textMutedDark,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${session.dataPointCount} data points",
                        color = textMutedDark,
                        fontSize = 14.sp
                    )
                }

                Row {
                    Button(
                        onClick = onAction,
                        enabled = actionEnabled,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text(actionText)
                    }

                    if (showDelete) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_delete_24),
                                contentDescription = "Delete",
                                tint = Color.Red
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardDark)
    ) {
        Text(
            text = message,
            color = textMutedDark,
            modifier = Modifier.padding(32.dp),
            textAlign = TextAlign.Center
        )
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
