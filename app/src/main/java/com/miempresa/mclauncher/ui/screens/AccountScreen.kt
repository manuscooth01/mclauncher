package com.miempresa.mclauncher.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miempresa.mclauncher.SettingsManager
import com.miempresa.mclauncher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(settingsManager: SettingsManager) {
    var usernameInput by remember { mutableStateOf("") }
    var activeUser by remember { mutableStateOf(settingsManager.getActiveUser()) }
    var sessionType by remember { mutableStateOf(settingsManager.getSessionType()) }
    var isLoggedIn by remember { mutableStateOf(settingsManager.isLoggedIn()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "CUENTA",
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Profile card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLoggedIn) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    1.dp,
                    if (isLoggedIn) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (isLoggedIn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(activeUser, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "AUTH: $sessionType",
                            fontSize = 11.sp,
                            color = if (isLoggedIn) StatusOk else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("INICIAR SESIÓN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                placeholder = { Text("Tu nombre de usuario") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (usernameInput.isNotBlank()) {
                            settingsManager.setActiveUser(usernameInput)
                            settingsManager.setSessionType("LOCAL")
                            settingsManager.setLoggedIn(true)
                            activeUser = usernameInput
                            sessionType = "LOCAL"
                            isLoggedIn = true
                            usernameInput = ""
                            scope.launch { snackbarHostState.showSnackbar("Sesión local activa") }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("LOCAL", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                }
                Button(
                    onClick = {
                        if (usernameInput.isNotBlank()) {
                            settingsManager.setActiveUser(usernameInput)
                            settingsManager.setSessionType("MICROSOFT")
                            settingsManager.setLoggedIn(true)
                            activeUser = usernameInput
                            sessionType = "MICROSOFT"
                            isLoggedIn = true
                            usernameInput = ""
                            scope.launch { snackbarHostState.showSnackbar("Sesión Microsoft activa") }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("MICROSOFT", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}
