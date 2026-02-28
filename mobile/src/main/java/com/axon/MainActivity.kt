package com.axon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.axon.domain.model.AuthState
import com.axon.presentation.screens.SensorDataViewModel
import com.axon.presentation.screens.WatchDataScreen
import com.axon.presentation.screens.auth.AuthScreen
import com.axon.presentation.screens.auth.AuthViewModel
import com.axon.presentation.screens.cloud.CloudSyncScreen
import com.axon.presentation.screens.cloud.CloudSyncViewModel
import com.axon.presentation.screens.dashboard.RecoveryDashboardScreen
import com.axon.presentation.screens.sessions.SessionDetailScreen
import com.axon.presentation.screens.sessions.SessionListScreen
import com.axon.presentation.screens.sessions.SessionViewModel
import com.axon.presentation.theme.AxonTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AxonTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.authState.collectAsState()

                // Determine start destination based on auth state
                val startDestination = when (authState) {
                    AuthState.AUTHENTICATED -> "dashboard"
                    AuthState.NOT_AUTHENTICATED -> "auth"
                    else -> "auth"
                }

                Scaffold(
                    bottomBar = {
                        // Only show bottom bar when authenticated and not on auth screen
                        if (authState == AuthState.AUTHENTICATED && currentRoute != "auth") {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentRoute == "dashboard",
                                    onClick = {
                                        navController.navigate("dashboard") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.outline_dashboard_24),
                                            contentDescription = "Dashboard",
                                        )
                                    },
                                    label = { Text("Dashboard") },
                                    colors =
                                        NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color(0xFF2196F3),
                                            selectedTextColor = Color(0xFF2196F3),
                                            unselectedIconColor = Color(0xFF8BA4C7),
                                            unselectedTextColor = Color(0xFF8BA4C7),
                                            indicatorColor = Color.Transparent,
                                        ),
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "sessions",
                                    onClick = {
                                        navController.navigate("sessions") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            painterResource(R.drawable.pulse),
                                            contentDescription = "Sessions",
                                        )
                                    },
                                    label = { Text("Sessions") },
                                    colors =
                                        NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color(0xFF2196F3),
                                            selectedTextColor = Color(0xFF2196F3),
                                            unselectedIconColor = Color(0xFF8BA4C7),
                                            unselectedTextColor = Color(0xFF8BA4C7),
                                            indicatorColor = Color.Transparent,
                                        ),
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "cloud_sync",
                                    onClick = {
                                        navController.navigate("cloud_sync") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            painterResource(R.drawable.outline_arrow_top_right_24),
                                            contentDescription = "Cloud",
                                        )
                                    },
                                    label = { Text("Cloud") },
                                    colors =
                                        NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color(0xFF2196F3),
                                            selectedTextColor = Color(0xFF2196F3),
                                            unselectedIconColor = Color(0xFF8BA4C7),
                                            unselectedTextColor = Color(0xFF8BA4C7),
                                            indicatorColor = Color.Transparent,
                                        ),
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "watch",
                                    onClick = {
                                        navController.navigate("watch") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            painterResource(R.drawable.outline_display_settings_24),
                                            contentDescription = "Watch",
                                        )
                                    },
                                    label = { Text("Watch") },
                                    colors =
                                        NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color(0xFF2196F3),
                                            selectedTextColor = Color(0xFF2196F3),
                                            unselectedIconColor = Color(0xFF8BA4C7),
                                            unselectedTextColor = Color(0xFF8BA4C7),
                                            indicatorColor = Color.Transparent,
                                        ),
                                )
                            }
                        }
                    },
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable("auth") {
                            AuthScreen(
                                viewModel = authViewModel,
                                onNavigateToDashboard = {
                                    navController.navigate("dashboard") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("dashboard") {
                            RecoveryDashboardScreen(
                                authViewModel = authViewModel,
                                onNavigateToAuth = {
                                    navController.navigate("auth") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("cloud_sync") {
                            val viewModel: CloudSyncViewModel = hiltViewModel()
                            CloudSyncScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("watch") {
                            val viewModel: SensorDataViewModel = hiltViewModel()
                            WatchDataScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                            )
                        }

                        composable("sessions") {
                            val viewModel: SessionViewModel = hiltViewModel()
                            SessionListScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onSessionClick = { sessionId ->
                                    navController.navigate("session_detail/$sessionId")
                                },
                            )
                        }

                        composable("session_detail/{sessionId}") { backStackEntry ->
                            val sessionId =
                                backStackEntry.arguments?.getString("sessionId")?.toLongOrNull()
                            val viewModel: SessionViewModel = hiltViewModel()
                            sessionId?.let {
                                SessionDetailScreen(
                                    viewModel = viewModel,
                                    sessionId = it,
                                    onNavigateBack = {
                                        viewModel.clearSelectedSession()
                                        navController.popBackStack()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
