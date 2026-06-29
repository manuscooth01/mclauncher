package com.miempresa.mclauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.miempresa.mclauncher.ui.theme.LucyMcTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LucyMcTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigationContainer(filesDir = filesDir)
                }
            }
        }
    }
}

data class NavigationItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun MainNavigationContainer(filesDir: File) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navItems = listOf(
        NavigationItem("versiones", "VERSIONS", Icons.Filled.List),
        NavigationItem("perfiles", "PROFILES", Icons.Filled.AccountBox),
        NavigationItem("mods", "MODS", Icons.Filled.Star),
        NavigationItem("cuenta", "ACCOUNT", Icons.Filled.Lock),
        NavigationItem("ajustes", "SETTINGS", Icons.Filled.Settings),
        NavigationItem("hardware", "HARDWARE", Icons.Filled.Build),
        NavigationItem("servidores", "SERVERS", Icons.Filled.Home)
    )

    Row(modifier = Modifier.fillMaxSize()) {
        // BARRA LATERAL TÁCTICA (Navigation Rail)
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight().width(85.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            navItems.forEach { item ->
                val isSelected = currentRoute == item.route
                NavigationRailItem(
                    selected = isSelected,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(imageVector = item.icon, contentDescription = item.label, modifier = Modifier.size(20.dp)) },
                    label = { Text(text = item.label, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.background,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }

        // CONTENEDOR PRINCIPAL DINÁMICO
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            NavHost(navController = navController, startDestination = "versiones") {
                composable("versiones") { VersionsScreen(filesDir) }
                composable("perfiles") { ProfilesScreen() }
                composable("mods") { ModsScreen() }
                composable("cuenta") { AccountScreen() }
                composable("ajustes") { SettingsScreen() }
                composable("hardware") { HardwareScreen() }
                composable("servidores") { ServersScreen() }
            }
        }
    }
}

// ==========================================
// 1. MÓDULO DE VERSIONES (MANIPULACIÓN DE RED)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionsScreen(filesDir: File) {
    val scope = rememberCoroutineScope()
    var versions by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var isLoading by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://launchermeta.mojang.com/mc/game/version_manifest.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) sb.append(line)
                reader.close()

                val manifest = JSONObject(sb.toString())
                val versionsArray = manifest.getJSONArray("versions")
                val list = mutableListOf<Pair<String, String>>()

                for (i in 0 until versionsArray.length()) {
                    val v = versionsArray.getJSONObject(i)
                    list.add(v.getString("id") to v.getString("type"))
                }

                withContext(Dispatchers.Main) {
                    versions = list
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    status = "Error: ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LucyMC // NÚCLEO_RED", fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 4.dp)) {
            if (status.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text(text = status, modifier = Modifier.padding(10.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("SINCRO_MANIFEST...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.5.sp)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(versions) { (id, type) ->
                        VersionCard(
                            versionId = id,
                            versionType = type,
                            onDownload = {
                                scope.launch(Dispatchers.IO) {
                                    downloadVersion(filesDir, id, type) { msg -> status = msg }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VersionCard(versionId: String, versionType: String, onDownload: () -> Unit) {
    val badgeColor = if (versionType == "release") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onDownload),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = versionId, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = versionType.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = badgeColor, letterSpacing = 1.sp)
            }
            Button(
                onClick = onDownload,
                colors = ButtonDefaults.buttonColors(containerColor = badgeColor, contentColor = MaterialTheme.colorScheme.onPrimary),
                shape = RoundedCornerShape(2.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text("DESCARGAR", fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

// ==========================================
// 2. MÓDULO DE PERFILES (INSTANCIAS DE JUEGO)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LucyMC // PERFILES_DIRECTOR", fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("PERFIL ACTIVO", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Minecraft Vanilla", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("VERSION: 1.20.1 // DEFAULT", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            Card(
                modifier = Modifier.weight(1.2f).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CREAR CONFIGURACIÓN", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                    OutlinedTextField(
                        value = "", onValueChange = {}, label = { Text("NOMBRE PERFIL", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth().height(52.dp), singleLine = true
                    )
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(2.dp)) {
                        Text("GUARDAR INSTANCIA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. MÓDULO DE MODS (GESTOR COMPONENTES)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModsScreen() {
    val mockMods = listOf("Sodium-Fabric-1.20.1.jar", "Iris-Shaders-1.20.1.jar", "Lithium-Optimization.jar")
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LucyMC // MODS_INJECTOR", fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("MODS DETECTADOS EN /mods", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                Button(onClick = {}, shape = RoundedCornerShape(2.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    Text("AÑADIR .JAR", fontSize = 10.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(mockMods) { mod ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                    ) {
                        Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(mod, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("ACTIVO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. MÓDULO DE CUENTAS (IDENTIDAD OPERADOR)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen() {
    var usernameInput by remember { mutableStateOf("") }
    var isLoggedIn by remember { mutableStateOf(false) }
    var activeUser by remember { mutableStateOf("INVITADO_X") }
    var sessionType by remember { mutableStateOf("NINGUNA") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LucyMC // ID_MANAGER", fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (isLoggedIn) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(48.dp), tint = if (isLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = activeUser.uppercase(), fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "ACCESO: $sessionType", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
                    if (isLoggedIn) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { isLoggedIn = false; activeUser = "INVITADO_X"; sessionType = "NINGUNA" }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(2.dp)) {
                            Text("LOGOUT", fontSize = 9.sp)
                        }
                    }
                }
            }
            Card(
                modifier = Modifier.weight(1.3f).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("REGISTRAR ACCESO", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                    OutlinedTextField(value = usernameInput, onValueChange = { usernameInput = it }, label = { Text("OPERADOR ALIAS", fontSize = 10.sp) }, modifier = Modifier.fillMaxWidth().height(50.dp), singleLine = true)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { if (usernameInput.isNotBlank()) { activeUser = usernameInput; sessionType = "OFFLINE"; isLoggedIn = true; usernameInput = "" } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary), shape = RoundedCornerShape(2.dp)) {
                            Text("LOCAL", fontSize = 10.sp)
                        }
                        Button(onClick = { if (usernameInput.isNotBlank()) { activeUser = usernameInput; sessionType = "MICROSOFT"; isLoggedIn = true; usernameInput = "" } }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(2.dp)) {
                            Text("LOGIN MS", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. MÓDULO DE AJUSTES (ESTRUCTURA RUTAS)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LucyMC // PANEL_CONFIG", fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("DIRECTORIOS INTERNOS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
            OutlinedTextField(value = "/data/data/com.termux/files/home/.minecraft", onValueChange = {}, label = { Text("RUTA DE JUEGO", fontSize = 10.sp) }, modifier = Modifier.fillMaxWidth(), readOnly = true)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Activar registros detallados (Logs)", fontSize = 13.sp)
                Switch(checked = true, onCheckedChange = {}, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary))
            }
        }
    }
}

// ==========================================
// 6. MÓDULO DE HARDWARE (ASIGNACIÓN DE RAM)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwareScreen() {
    var ramAllocation by remember { mutableStateOf(2048f) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LucyMC // HARDWARE_RESOURCE", fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.Center) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ASIGNACIÓN ASIGNADA DE MEMORIA JVM", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "${ramAllocation.toInt()} MB asignados para Minecraft (-Xmx)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Slider(
                        value = ramAllocation,
                        onValueChange = { ramAllocation = it },
                        valueRange = 1024f..8192f,
                        steps = 7,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )
                    Text("Rango seguro del sistema: 1GB a 8GB de asignación limpia.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ==========================================
// 7. MÓDULO SERVIDORES (CONEXIONES INTERNAS)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LucyMC // MATRIX_SERVERS", fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier.weight(1.2f).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("AÑADIR TERMINAL SERVER", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                    OutlinedTextField(value = "", onValueChange = {}, label = { Text("IP / DOMINIO", fontSize = 10.sp) }, modifier = Modifier.fillMaxWidth().height(48.dp))
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(2.dp)) {
                        Text("ENLAZAR SERVER", fontSize = 10.sp)
                    }
                }
            }
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("DIRECCIÓN GUARDADA", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("LucyMC Official", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("play.lucymc.net:25565", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ==========================================
// PROCESADOR DE DESCARGAS (NATIVO SIN CAMBIOS)
// ==========================================
suspend fun downloadVersion(filesDir: File, versionId: String, versionType: String, onStatus: (String) -> Unit) {
    try {
        onStatus("Buscando $versionId...")
        val manifestUrl = URL("https://launchermeta.mojang.com/mc/game/version_manifest.json")
        val conn = manifestUrl.openConnection() as HttpURLConnection
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) sb.append(line)
        reader.close()

        val manifest = JSONObject(sb.toString())
        val versions = manifest.getJSONArray("versions")
        var versionUrl = ""
        for (i in 0 until versions.length()) {
            val v = versions.getJSONObject(i)
            if (v.getString("id") == versionId) {
                versionUrl = v.getString("url")
                break
            }
        }
        if (versionUrl.isEmpty()) { onStatus("Error: URL no encontrada"); return }

        onStatus("Descargando JSON de $versionId...")
        val vConn = URL(versionUrl).openConnection() as HttpURLConnection
        val vReader = BufferedReader(InputStreamReader(vConn.inputStream))
        val vSb = StringBuilder()
        while (vReader.readLine().also { line = it } != null) vSb.append(line)
        vReader.close()

        val versionJson = vSb.toString()
        val dir = File(filesDir, "versions/$versionId")
        if (!dir.exists()) dir.mkdirs()
        FileWriter(File(dir, "$versionId.json")).use { it.write(versionJson) }

        val downloads = JSONObject(versionJson).getJSONObject("downloads")
        val client = downloads.getJSONObject("client")
        val clientUrl = client.getString("url")
        val clientSize = client.getLong("size")

        onStatus("Descargando cliente (${clientSize / 1024 / 1024}MB)...")
        val jarFile = File(dir, "$versionId.jar")
        URL(clientUrl).openStream().use { input -> jarFile.outputStream().use { output -> input.copyTo(output) } }
        onStatus("✅ $versionId listo! JSON + JAR guardados")
    } catch (e: Exception) {
        onStatus("❌ Error: ${e.message}")
    }
}
