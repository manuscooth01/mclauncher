package com.miempresa.mclauncher.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miempresa.mclauncher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModsScreen() {
    var mods by remember { mutableStateOf(listOf(
        "Sodium-Fabric-1.20.1.jar",
        "Iris-Shaders-1.20.1.jar",
        "Lithium-Optimization.jar",
        "Fabric-API-1.20.1.jar"
    )) }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MODS",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = {
                        val newMod = "Custom-Mod-${mods.size + 1}.jar"
                        mods = mods + newMod
                        scope.launch { snackbarHostState.showSnackbar("Mod agregado") }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("AGREGAR", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "${mods.size} ARCHIVOS .JAR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(mods, key = { it }) { mod ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mod, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("LOADED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StatusOk)
                            }
                            IconButton(
                                onClick = {
                                    mods = mods - mod
                                    scope.launch { snackbarHostState.showSnackbar("Mod eliminado") }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("X", color = StatusError, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
