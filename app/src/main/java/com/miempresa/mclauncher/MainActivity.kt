package com.miempresa.mclauncher

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.miempresa.mclauncher.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

// ---------- RUTAS TIPADAS ----------
sealed class Screen(val route: String) {
    object Versions : Screen("versiones")
    object Modpacks : Screen("modpacks")
    object Mods : Screen("mods")
    object Account : Screen("cuenta")
    object Settings : Screen("ajustes")
}

data class NavigationItem(val screen: Screen, val label: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LucyMcTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = CyberDark) {
                    MainNavigationContainer(filesDir = filesDir, context = this)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationContainer(filesDir: File, context: Context) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val settingsManager = remember(context) { SettingsManager(context) }
    val versionManager = remember(filesDir, context) { VersionManager(filesDir, context) }

    val versionsViewModel: VersionsViewModel = viewModel(
        factory = VersionsViewModelFactory(versionManager)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(settingsManager)
    )

    val navItems = listOf(
        NavigationItem(Screen.Versions, "VERSIONES", Icons.Filled.List),
        NavigationItem(Screen.Modpacks, "MODPACKS", Icons.Filled.FolderOpen),
        NavigationItem(Screen.Mods, "MODS", Icons.Filled.Widgets),
        NavigationItem(Screen.Account, "CUENTA", Icons.Filled.Person),
        NavigationItem(Screen.Settings, "AJUSTES", Icons.Filled.Settings)
    )

    Scaffold(
        containerColor = CyberDark,
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurface,
                tonalElevation = 0.dp,
                modifier = Modifier.height(64.dp)
            ) {
                navItems.forEach { item ->
                    val isSelected = currentRoute == item.screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != item.screen.route) {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonGreen,
                            selectedTextColor = NeonGreen,
                            unselectedIconColor = CyberCyan.copy(alpha = 0.5f),
                            unselectedTextColor = CyberCyan.copy(alpha = 0.4f),
                            indicatorColor = NeonGreen.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Versions.route
            ) {
                composable(Screen.Versions.route) {
                    VersionsScreen(
                        viewModel = versionsViewModel,
                        versionManager = versionManager,
                        snackbarHostState = remember { SnackbarHostState() }
                    )
                }
                composable(Screen.Modpacks.route) { ModpacksScreen() }
                composable(Screen.Mods.route) { ModsScreen() }
                composable(Screen.Account.route) { AccountScreen(settingsManager) }
                composable(Screen.Settings.route) {
                    SettingsScreen(viewModel = settingsViewModel)
                }
            }
        }
    }
}

class VersionsViewModelFactory(private val versionManager: VersionManager) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VersionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VersionsViewModel(versionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class SettingsViewModelFactory(private val settingsManager: SettingsManager) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ============================================================================
//  PANTALLA DE VERSIONES (CON GRID, FOOTER Y BOTTOM SHEET)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionsScreen(
    viewModel: VersionsViewModel,
    versionManager: VersionManager,
    snackbarHostState: SnackbarHostState
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedVersion by viewModel.selectedVersion.collectAsState()
    val isBottomSheetOpen by viewModel.isBottomSheetOpen.collectAsState()
    val scope = rememberCoroutineScope()

    // Efectos (snackbars)
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is VersionsEffect.ShowSnackbar -> {
                    scope.launch { snackbarHostState.showSnackbar(effect.message) }
                }
            }
        }
    }

    // Filtrar versiones
    val filteredVersions = remember(uiState.versions, uiState.selectedFilter, uiState.searchQuery) {
        var list = when (uiState.selectedFilter) {
            "RELEASES" -> uiState.versions.filter { it.second == "release" }
            "SNAPSHOTS" -> uiState.versions.filter { it.second == "snapshot" }
            else -> uiState.versions
        }
        if (uiState.searchQuery.isNotBlank()) {
            list = list.filter { it.first.contains(uiState.searchQuery, ignoreCase = true) }
        }
        list.sortedByDescending { it.first }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent,
        modifier = Modifier.background(Brush.verticalGradient(listOf(CyberDark, CyberSurface))),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "LUCYMC",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 4.sp,
                        color = NeonGreen
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = NeonGreen
                ),
                actions = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                when {
                                    uiState.isLoading -> Color.Yellow
                                    uiState.downloading -> Color.Yellow
                                    else -> Color.Green
                                },
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            )
        },
        bottomBar = {
            LaunchFooter(
                selectedVersion = selectedVersion,
                onLaunch = { viewModel.launchGame() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
        ) {
            if (!uiState.isLoading) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    placeholder = {
                        Text(
                            "BUSCAR VERSIÓN...",
                            color = CyberCyan.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            null,
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    Icons.Filled.Clear,
                                    null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberSurface,
                        focusedContainerColor = CyberPanel,
                        unfocusedContainerColor = CyberPanel
                    ),
                    shape = RoundedCornerShape(2.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ALL", "RELEASES", "SNAPSHOTS").forEach { option ->
                        val isSelected = uiState.selectedFilter == option
                        val accent = when (option) {
                            "RELEASES" -> NeonGreen
                            "SNAPSHOTS" -> CyberCyan
                            else -> Color.White
                        }
                        Button(
                            onClick = { viewModel.updateFilter(option) },
                            modifier = Modifier.weight(1f).height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) accent else CyberSurface
                            ),
                            shape = RoundedCornerShape(1.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) accent else accent.copy(alpha = 0.2f)
                            ),
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = option,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) CyberDark else accent,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen, strokeWidth = 2.dp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredVersions, key = { it.first }) { (id, type) ->
                        VersionGridCard(
                            versionId = id,
                            versionType = type,
                            isInstalled = viewModel.isVersionInstalled(id),
                            onCardClick = { viewModel.selectVersion(id) }
                        )
                    }
                }
            }

            if (uiState.downloadStatus.isNotEmpty()) {
                Text(
                    text = uiState.downloadStatus,
                    fontSize = 10.sp,
                    color = NeonGreen,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }

    if (isBottomSheetOpen && selectedVersion != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.cancelSelection() },
            containerColor = CyberSurface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            tonalElevation = 0.dp
        ) {
            LoaderSelectionSheet(
                versionSelection = selectedVersion!!,
                availableLoaders = viewModel.availableLoaders,
                getLoaderVersions = { loader, mcVersion ->
                    viewModel.getLoaderVersions(loader, mcVersion)
                },
                onLoaderSelected = { loader -> viewModel.updateLoader(loader) },
                onLoaderVersionSelected = { version -> viewModel.updateLoaderVersion(version) },
                onConfirm = { viewModel.confirmSelection() }
            )
        }
    }
}

// ----- COMPONENTE: TARJETA DEL GRID -----
@Composable
fun VersionGridCard(
    versionId: String,
    versionType: String,
    isInstalled: Boolean,
    onCardClick: () -> Unit
) {
    val accent = if (versionType == "release") NeonGreen else CyberCyan
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(4.dp), clip = false)
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        ),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = versionId,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = versionType.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = accent,
                letterSpacing = 1.sp
            )
            if (isInstalled) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Instalado",
                        tint = NeonGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "INSTALADO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonGreen
                    )
                }
            }
        }
    }
}

// ----- COMPONENTE: FOOTER DE LANZAMIENTO -----
@Composable
fun LaunchFooter(
    selectedVersion: VersionSelection?,
    onLaunch: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.08f),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .height(72.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(4.dp), clip = false)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), shape = RoundedCornerShape(4.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (selectedVersion != null) selectedVersion.displayName() else "SELECCIONA UNA VERSIÓN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedVersion != null) Color.White else CyberCyan.copy(alpha = 0.5f)
                )
                if (selectedVersion != null) {
                    Text(
                        text = "Listo para jugar",
                        fontSize = 10.sp,
                        color = NeonGreen
                    )
                }
            }

            Button(
                onClick = onLaunch,
                enabled = selectedVersion != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    disabledContainerColor = CyberPanel
                ),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "▶ INICIAR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (selectedVersion != null) CyberDark else CyberCyan.copy(alpha = 0.4f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ----- COMPONENTE: BOTTOM SHEET DE LOADER -----
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoaderSelectionSheet(
    versionSelection: VersionSelection,
    availableLoaders: List<String>,
    getLoaderVersions: (String, String) -> List<String>,
    onLoaderSelected: (String?) -> Unit,
    onLoaderVersionSelected: (String) -> Unit,
    onConfirm: () -> Unit
) {
    var selectedLoader by remember { mutableStateOf(versionSelection.loader) }
    var selectedLoaderVersion by remember { mutableStateOf(versionSelection.loaderVersion) }

    val loaderVersions = if (selectedLoader != null && selectedLoader != "Vanilla") {
        getLoaderVersions(selectedLoader!!, versionSelection.versionId)
    } else {
        emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "VERSIÓN ${versionSelection.versionId}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text("SELECCIONA LOADER", fontSize = 10.sp, color = CyberCyan)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            availableLoaders.forEach { loader ->
                val isSelected = selectedLoader == loader
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedLoader = if (isSelected) null else loader
                        selectedLoaderVersion = null
                        onLoaderSelected(selectedLoader)
                    },
                    label = {
                        Text(
                            text = loader,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) CyberDark else CyberCyan
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonGreen,
                        containerColor = CyberSurface,
                        selectedLabelColor = CyberDark
                    ),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.border(
                        BorderStroke(
                            1.dp,
                            if (isSelected) NeonGreen else CyberCyan.copy(alpha = 0.3f)
                        ),
                        RoundedCornerShape(2.dp)
                    )
                )
            }
        }

        if (selectedLoader != null && selectedLoader != "Vanilla") {
            Spacer(modifier = Modifier.height(12.dp))
            Text("VERSIONES DE $selectedLoader", fontSize = 10.sp, color = CyberCyan)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                loaderVersions.forEach { version ->
                    val isSelected = selectedLoaderVersion == version
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedLoaderVersion = version
                            onLoaderVersionSelected(version)
                        },
                        label = {
                            Text(
                                text = version,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) CyberDark else CyberCyan
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan,
                            containerColor = CyberSurface,
                            selectedLabelColor = CyberDark
                        ),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.border(
                            BorderStroke(
                                1.dp,
                                if (isSelected) CyberCyan else CyberCyan.copy(alpha = 0.3f)
                            ),
                            RoundedCornerShape(2.dp)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = "CONFIRMAR",
                color = CyberDark,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )
        }
    }
}

// ============================================================================
//  RESTO DE PANTALLAS
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModpacksScreen() {
    Scaffold(
        containerColor = CyberDark,
        topBar = {
            TopAppBar(
                title = { Text("LucyMC // MODPACKS_ENGINE", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberDark, titleContentColor = CyberCyan)
            )
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(modifier = Modifier.weight(1f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = CyberSurface), border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.2f)), shape = RoundedCornerShape(2.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("MODPACK ACTIVO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = CyberCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Fabric-Loader-1.20.1", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("ESTADO: EN LA LÍNEA", fontSize = 9.sp, color = NeonGreen)
                }
            }
            Card(modifier = Modifier.weight(1.2f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = CyberPanel), border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.15f)), shape = RoundedCornerShape(2.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("CREAR NUEVO MODPACK", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeonGreen)
                    OutlinedTextField(
                        value = "", onValueChange = {}, label = { Text("NOMBRE", color = CyberCyan.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, unfocusedBorderColor = CyberSurface),
                        modifier = Modifier.fillMaxWidth().height(48.dp), singleLine = true
                    )
                    Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = NeonGreen), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(1.dp)) {
                        Text("GENERAR MODPACK", color = CyberDark, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModsScreen() {
    val mockMods = remember { listOf("Sodium-Fabric-1.20.1.jar", "Iris-Shaders-1.20.1.jar", "Lithium-Optimization.jar") }
    Scaffold(
        containerColor = CyberDark,
        topBar = {
            TopAppBar(
                title = { Text("LucyMC // MODS_INJECTOR", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberDark, titleContentColor = NeonGreen)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("ARCHIVOS .JAR EN /mods", fontSize = 11.sp, fontWeight = FontWeight.Black, color = CyberCyan)
                Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = CyberCyan), shape = RoundedCornerShape(1.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                    Text("ADD_MOD", fontSize = 10.sp, color = CyberDark, fontWeight = FontWeight.Black)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(mockMods) { mod ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyberSurface),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(mod, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            Text("LOADED", fontSize = 9.sp, fontWeight = FontWeight.Black, color = NeonGreen)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(settingsManager: SettingsManager) {
    var usernameInput by remember { mutableStateOf("") }
    var activeUser by remember { mutableStateOf(settingsManager.getActiveUser()) }
    var sessionType by remember { mutableStateOf(settingsManager.getSessionType()) }
    var isLoggedIn by remember { mutableStateOf(settingsManager.isLoggedIn()) }

    Scaffold(
        containerColor = CyberDark,
        topBar = {
            TopAppBar(
                title = { Text("LucyMC // OPERATOR_ID", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberDark, titleContentColor = NeonGreen)
            )
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(modifier = Modifier.weight(1f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = CyberSurface), border = BorderStroke(1.dp, if (isLoggedIn) NeonGreen.copy(alpha = 0.4f) else CyberCyan.copy(alpha = 0.2f)), shape = RoundedCornerShape(2.dp)) {
                Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.AccountCircle, null, modifier = Modifier.size(40.dp), tint = if (isLoggedIn) NeonGreen else CyberCyan.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = activeUser.uppercase(), fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                    Text(text = "AUTH: $sessionType", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isLoggedIn) NeonGreen else CyberCyan)
                }
            }
            Card(modifier = Modifier.weight(1.2f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = CyberPanel), border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.15f)), shape = RoundedCornerShape(2.dp)) {
                Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("AUTENTICAR LOG", fontSize = 10.sp, fontWeight = FontWeight.Black, color = CyberCyan)
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("ALIAS") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = {
                                if (usernameInput.isNotBlank()) {
                                    settingsManager.setActiveUser(usernameInput)
                                    settingsManager.setSessionType("LOCAL")
                                    settingsManager.setLoggedIn(true)
                                    activeUser = usernameInput
                                    sessionType = "LOCAL"
                                    isLoggedIn = true
                                    usernameInput = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                            border = BorderStroke(1.dp, CyberCyan),
                            shape = RoundedCornerShape(1.dp)
                        ) {
                            Text("LOCAL", fontSize = 9.sp, color = CyberCyan)
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
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(1.dp)
                        ) {
                            Text("MS_LOGIN", fontSize = 9.sp, color = CyberDark, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val ram by viewModel.ramAllocation.collectAsState()
    val isDevMode by viewModel.isDevMode.collectAsState()
    val gamePath by viewModel.gamePath.collectAsState()

    Scaffold(
        containerColor = CyberDark,
        topBar = {
            TopAppBar(
                title = { Text("LucyMC // SYSTEM_RUTAS", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberDark, titleContentColor = CyberCyan)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("MATRIZ DE ALMACENAMIENTO DE JUEGO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = CyberCyan)
            OutlinedTextField(value = gamePath, onValueChange = {}, modifier = Modifier.fillMaxWidth(), readOnly = true)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("MODO DESARROLLADOR LOGS", fontSize = 12.sp, color = Color.White)
                Switch(
                    checked = isDevMode,
                    onCheckedChange = { viewModel.toggleDevMode() },
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "ASIGNACIÓN LÍMITE DE RAM JVM (-Xmx)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${ram.toInt()} MB ALLOCATED",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonGreen
                    )
                    Slider(
                        value = ram,
                        onValueChange = viewModel::setRamAllocation,
                        valueRange = 1024f..8192f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonGreen,
                            activeTrackColor = NeonGreen,
                            inactiveTrackColor = CyberSurface
                        )
                    )
                }
            }
        }
    }
}
