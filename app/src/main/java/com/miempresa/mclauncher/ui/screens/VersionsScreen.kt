package com.miempresa.mclauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miempresa.mclauncher.VersionsViewModel
import com.miempresa.mclauncher.VersionManager
import kotlinx.coroutines.launch

private val FILTERS = listOf("TODAS", "RELEASE", "SNAPSHOT", "OLD_BETA", "OLD_ALPHA")
private val FILTER_COLORS = listOf(Color(0xFF888888), Color(0xFF00FF00), Color(0xFF00FFFF), Color(0xFFFFCC00), Color(0xFFFF4444))

@Composable
fun VersionsScreen(viewModel: VersionsViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(0) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedVersion by remember { mutableStateOf<String?>(null) }
    var selectedLoader by remember { mutableStateOf<String?>(null) }
    var selectedLoaderVer by remember { mutableStateOf<String?>(null) }

    val versions = viewModel.versions
    val installedVersions = viewModel.installedVersions
    val isLoading = viewModel.isLoading
    val downloading = viewModel.downloading
    val downloadProgress = viewModel.downloadProgress

    val filtered = remember(versions, selectedFilter, searchQuery) {
        var list = versions
        when (selectedFilter) {
            1 -> list = list.filter { it.second == "release" }
            2 -> list = list.filter { it.second == "snapshot" }
            3 -> list = list.filter { it.second == "old_beta" }
            4 -> list = list.filter { it.second == "old_alpha" }
        }
        if (searchQuery.isNotBlank()) {
            list = list.filter { it.first.contains(searchQuery, ignoreCase = true) }
        }
        list.sortedByDescending { it.first }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("LUCYMC", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF00FF00), letterSpacing = 2.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isLoading || downloading) Color(0xFFFFCC00) else Color(0xFF00FF00), shape = RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                )
                Text(if (downloading) "DESCARGANDO" else if (isLoading) "CARGANDO" else "ONLINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888888))
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it; viewModel.updateSearch(it) },
            placeholder = { Text("Buscar...", color = Color(0xFF666666)) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color(0xFF666666)) },
            trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = ""; viewModel.updateSearch("") }) { Icon(Icons.Filled.Clear, null, tint = Color(0xFF666666)) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFF333333),
                focusedBorderColor = Color(0xFF00FF00),
                unfocusedContainerColor = Color(0xFF1A1A1A),
                focusedContainerColor = Color(0xFF1A1A1A),
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White
            )
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FILTERS.forEachIndexed { i, f ->
                val selected = selectedFilter == i
                val c = FILTER_COLORS[i]
                androidx.compose.material3.FilterChip(
                    selected = selected,
                    onClick = { selectedFilter = i; viewModel.updateFilter(i) },
                    label = { Text(f, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.Black else Color.White) },
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = c,
                        containerColor = Color(0xFF2A2A2A)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        if (downloading) {
            val prog = downloadProgress
            val progress = if (prog != null && prog.total > 0) prog.current.toFloat() / prog.total else 0f
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = Color(0xFF00FF00),
                    trackColor = Color(0xFF222222)
                )
                Text("[${prog?.phase}] ${prog?.detail ?: ""}", fontSize = 10.sp, color = Color(0xFF888888))
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00FF00), strokeWidth = 3.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it.first }) { (id, type) ->
                    val installed = installedVersions.contains(id)
                    val accent = when (type) {
                        "release" -> Color(0xFF00FF00)
                        "snapshot" -> Color(0xFF00FFFF)
                        "old_beta" -> Color(0xFFFFCC00)
                        "old_alpha" -> Color(0xFFFF4444)
                        else -> Color(0xFF666666)
                    }
                    VersionCard(
                        id = id,
                        type = type.uppercase(),
                        installed = installed,
                        accent = accent,
                        onClick = {
                            if (viewModel.versionManager.isVersionInstalled(id)) {
                                selectedVersion = id
                            } else {
                                selectedVersion = id
                                selectedLoader = null
                                selectedLoaderVer = null
                                showBottomSheet = true
                            }
                        }
                    )
                }
            }
        }
    }

    if (showBottomSheet && selectedVersion != null) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Versión $selectedVersion", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("SELECCIONA LOADER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888888))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Vanilla", "Fabric", "Forge", "OptiFine", "Quilt").forEach { loader ->
                        val sel = selectedLoader == loader
                        androidx.compose.material3.FilterChip(
                            selected = sel,
                            onClick = { selectedLoader = if (sel) null else loader; selectedLoaderVer = null },
                            label = { Text(loader, fontSize = 11.sp, color = if (sel) Color.Black else Color.White) },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00FF00),
                                containerColor = Color(0xFF2A2A2A)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
                if (selectedLoader != null && selectedLoader != "Vanilla") {
                    val vers = viewModel.getLoaderVersions(selectedLoader!!)
                    if (vers.isNotEmpty()) {
                        Text("VERSIONES DE $selectedLoader", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888888))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            vers.forEach { v ->
                                val sel = selectedLoaderVer == v
                                androidx.compose.material3.FilterChip(
                                    selected = sel,
                                    onClick = { selectedLoaderVer = v },
                                    label = { Text(v, fontSize = 11.sp, color = if (sel) Color.Black else Color.White) },
                                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00FFFF),
                                        containerColor = Color(0xFF2A2A2A)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        if (selectedVersion != null) {
                            viewModel.downloadVersion(selectedVersion!!)
                            showBottomSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF00), contentColor = Color.Black)
                ) {
                    Text("DESCARGAR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun VersionCard(
    id: String,
    type: String,
    installed: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (installed) accent.copy(alpha = 0.3f) else Color(0xFF333333))
    ) {
        Column(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(id, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 1.dp)
                    .background(accent.copy(alpha = 0.15f), shape = RoundedCornerShape(2.dp))
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
            ) {
                Text(type, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = accent, letterSpacing = 0.5.sp)
            }
            if (installed) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF00FF00), modifier = Modifier.size(12.dp))
                    Text("INSTALADA", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FF00))
                }
            }
        }
    }
}