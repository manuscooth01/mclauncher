package com.miempresa.mclauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.miempresa.mclauncher.ui.screens.AccountScreen
import com.miempresa.mclauncher.ui.screens.ModpacksScreen
import com.miempresa.mclauncher.ui.screens.ModsScreen
import com.miempresa.mclauncher.ui.screens.SettingsScreen
import com.miempresa.mclauncher.ui.screens.VersionsScreen
import com.miempresa.mclauncher.ui.theme.LucyMcTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LucyMcTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val versionsVm: VersionsViewModel = viewModel()
                val settingsVm: SettingsViewModel = viewModel()

                var selectedTab by remember { mutableStateOf(0) }
                val tabs = listOf("VERSIONES", "MODPACKS", "MODS", "CUENTA", "AJUSTES")
                val routes = listOf("versions", "modpacks", "mods", "account", "settings")
                val icons = listOf(Icons.Filled.Home, Icons.Filled.FolderOpen, Icons.Filled.Widgets, Icons.Filled.Person, Icons.Filled.Settings)

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = Color(0xFF1E1E1E)
                        ) {
                            tabs.forEachIndexed { index, label ->
                                val selected = selectedTab == index
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        selectedTab = index
                                        navController.navigate(routes[index]) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(icons[index], null, modifier = Modifier.size(24.dp), tint = if (selected) Color(0xFF00FF00) else Color(0xFF888888)) },
                                    label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selected) Color(0xFF00FF00) else Color(0xFF888888)) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    NavHost(navController, startDestination = "versions") {
                        composable("versions") { VersionsScreen(versionsVm) }
                        composable("modpacks") { ModpacksScreen() }
                        composable("mods") { ModsScreen() }
                        composable("account") { AccountScreen(settingsVm.settingsManager) }
                        composable("settings") { SettingsScreen(settingsVm) }
                    }
                }
            }
        }
    }
}