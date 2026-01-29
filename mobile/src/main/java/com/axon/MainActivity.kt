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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.axon.presentation.screens.SensorDataViewModel
import com.axon.presentation.screens.WatchDataScreen
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

                Scaffold(
                    bottomBar = {
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
                    },
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable("dashboard") {
                            RecoveryDashboardScreen()
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
