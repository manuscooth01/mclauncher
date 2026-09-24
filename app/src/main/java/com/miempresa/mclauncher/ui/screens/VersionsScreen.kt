@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.miempresa.mclauncher.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miempresa.mclauncher.*
import com.miempresa.mclauncher.ui.components.DownloadProgressIndicator
import com.miempresa.mclauncher.ui.components.VersionGridCard
import com.miempresa.mclauncher.ui.theme.*
import kotlinx.coroutines.launch

private val FILTERS = listOf("ALL", "RELEASES", "SNAPSHOTS", "OLD_BETA", "OLD_ALPHA")

@Composable
fun VersionsScreen(viewModel: VersionsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is VersionsEffect.ShowSnackbar -> {
                    scope.launch { snackbarHostState.showSnackbar(effect.message) }
                }
            }
        }
    }

    val filteredVersions = remember(uiState.versions, uiState.selectedFilter, uiState.searchQuery) {
        var list = when (uiState.selectedFilter) {
            "RELEASES" -> uiState.versions.filter { it.second == "release" }
            "SNAPSHOTS" -> uiState.versions.filter { it.second == "snapshot" }
            "OLD_BETA" -> uiState.versions.filter { it.second == "old_beta" }
            "OLD_ALPHA" -> uiState.versions.filter { it.second == "old_alpha" }
            else -> uiState.versions
        }
        if (uiState.searchQuery.isNotBlank()) {
            list = list.filter { it.first.contains(uiState.searchQuery, ignoreCase = true) }
        }
        list.sortedByDescending { it.first }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp) // Reduced from 16.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp), // Reduced from 16.dp
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LUCYMC",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp, // Reduced from 22.sp
                    letterSpacing = 4.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(8.dp),
                        color = when {
                            uiState.isLoading -> StatusWarn
                            uiState.downloading -> StatusWarn
                            else -> StatusOk
                        },
                        shape = RoundedCornerShape(50)
                    ) {}
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when {
                            uiState.downloading -> "DESCARGANDO"
                            uiState.isLoading -> "CARGANDO"
                            else -> "EN LINEA"
                        },
                        fontSize = 9.sp, // Reduced from 10.sp
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = {
                    Text("Buscar version...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                leadingIcon = {
                    Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    AnimatedVisibility(visible = uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Filled.Clear, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp), // Reduced from 12.dp
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp)) // Reduced from 12.dp

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp) // Reduced from 8.dp
            ) {
                FILTERS.forEach { option ->
                    val isSelected = uiState.selectedFilter == option
                    val accent = when (option) {
                        "RELEASES" -> NeonGreen
                        "SNAPSHOTS" -> CyberCyan
                        "OLD_BETA" -> StatusWarn
                        "OLD_ALPHA" -> StatusError
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.updateFilter(option) },
                        label = {
                            Text(
                                text = option,
                                fontSize = 9.sp, // Reduced from 10.sp
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(6.dp) // Reduced from 8.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp)) // Reduced from 12.dp

            DownloadProgressIndicator(uiState.downloadProgress)

            Spacer(modifier = Modifier.height(8.dp)) // Reduced from 12.dp

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp), // Reduced from 10.dp
                    horizontalArrangement = Arrangement.spacedBy(6.dp) // Reduced from 10.dp
                ) {
                    items(filteredVersions, key = { it.first }) { (id, type) ->
                        VersionGridCard(
                            versionId = id,
                            versionType = type,
                            isInstalled = uiState.installedVersions.contains(id),
                            onCardClick = { viewModel.selectVersion(id) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.isBottomSheetOpen && uiState.selectedVersionId != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.cancelSelection() },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            LoaderSelectionSheet(
                versionId = uiState.selectedVersionId!!,
                selectedLoader = uiState.selectedLoader,
                selectedLoaderVersion = uiState.selectedLoaderVersion,
                availableLoaders = viewModel.availableLoaders,
                getLoaderVersions = { loader -> viewModel.getLoaderVersions(loader) },
                onLoaderSelected = { viewModel.updateLoader(it) },
                onLoaderVersionSelected = { viewModel.updateLoaderVersion(it) },
                onConfirm = { viewModel.confirmSelection() }
            )
        }
    }
}

@Composable
fun LoaderSelectionSheet(
    versionId: String,
    selectedLoader: String?,
    selectedLoaderVersion: String?,
    availableLoaders: List<String>,
    getLoaderVersions: (String) -> List<String>,
    onLoaderSelected: (String?) -> Unit,
    onLoaderVersionSelected: (String) -> Unit,
    onConfirm: () -> Unit
) {
    val loaderVersions = remember(selectedLoader) {
        if (selectedLoader != null && selectedLoader != "Vanilla") getLoaderVersions(selectedLoader)
        else emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = "Version $versionId",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("SELECCIONA LOADER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableLoaders.forEach { loader ->
                val isSelected = selectedLoader == loader
                FilterChip(
                    selected = isSelected,
                    onClick = { onLoaderSelected(if (isSelected) null else loader) },
                    label = { Text(loader, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        if (selectedLoader != null && selectedLoader != "Vanilla" && loaderVersions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("VERSIONES DE $selectedLoader", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                loaderVersions.forEach { version ->
                    val isSelected = selectedLoaderVersion == version
                    FilterChip(
                        selected = isSelected,
                        onClick = { onLoaderVersionSelected(version) },
                        label = { Text(version, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                "CONFIRMAR",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
