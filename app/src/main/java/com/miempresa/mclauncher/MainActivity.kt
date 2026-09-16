package com.miempresa.mclauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.miempresa.mclauncher.ui.screens.*
import com.miempresa.mclauncher.ui.theme.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Versions : Screen("versions", "VERSIONES", Icons.Filled.List)
    object Modpacks : Screen("modpacks", "MODPACKS", Icons.Filled.FolderOpen)
    object Mods : Screen("mods", "MODS", Icons.Filled.Widgets)
    object Account : Screen("account", "CUENTA", Icons.Filled.Person)
    object Settings : Screen("settings", "AJUSTES", Icons.Filled.Settings)
}

private val screens = listOf(Screen.Versions, Screen.Modpacks, Screen.Mods, Screen.Account, Screen.Settings)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LucyMcTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val settingsManager = remember { SettingsManager(applicationContext) }
                val versionManager = remember { VersionManager(filesDir, applicationContext) }

                val versionsViewModel: VersionsViewModel = viewModel(
                    factory = VersionsViewModelFactory(versionManager, applicationContext)
                )
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModelFactory(settingsManager)
                )

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp
                        ) {
                            screens.forEach { screen ->
                                val isSelected = currentRoute == screen.route
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(screen.icon, null, modifier = Modifier.size(22.dp))
                                    },
                                    label = {
                                        Text(
                                            screen.label,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        NavHost(navController, startDestination = Screen.Versions.route) {
                            composable(Screen.Versions.route) { VersionsScreen(versionsViewModel) }
                            composable(Screen.Modpacks.route) { ModpacksScreen() }
                            composable(Screen.Mods.route) { ModsScreen() }
                            composable(Screen.Account.route) { AccountScreen(settingsManager) }
                            composable(Screen.Settings.route) { SettingsScreen(settingsViewModel) }
                        }
                    }
                }
            }
        }
    }
}

class VersionsViewModelFactory(
    private val versionManager: VersionManager,
    private val context: android.content.Context
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VersionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VersionsViewModel(versionManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class SettingsViewModelFactory(
    private val settingsManager: SettingsManager
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
