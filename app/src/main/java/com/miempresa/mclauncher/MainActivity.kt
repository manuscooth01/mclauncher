package com.miempresa.mclauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
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

// PALETA CRÍTICA CYBERPUNK PARA EVITAR TONOS GRISES GENÉRICOS
val NeonGreen = Color(0xFF00FF9F)
val CyberCyan = Color(0xFF00B8FF)
val CyberDark = Color(0xFF0A0B10)
val CyberSurface = Color(0xFF12131C)
val CyberPanel = Color(0xFF1A1C28)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LucyMcTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = CyberDark) {
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

    val navItems = remember {
        listOf(
            NavigationItem("versiones", "VERSIONS", Icons.Filled.List),
            NavigationItem("perfiles", "PROFILES", Icons.Filled.AccountBox),
            NavigationItem("mods", "MODS", Icons.Filled.Star),
            NavigationItem("cuenta", "ACCOUNT", Icons.Filled.Lock),
            NavigationItem("ajustes", "SETTINGS", Icons.Filled.Settings),
            NavigationItem("hardware", "HARDWARE", Icons.Filled.Build),
            NavigationItem("servidores", "SERVERS", Icons.Filled.Home)
        )
    }

    Row(modifier = Modifier.fillMaxSize().background(CyberDark)) {
        // BARRA LATERAL TÁCTICA MEJORADA
        NavigationRail(
            containerColor = CyberSurface,
            modifier = Modifier.fillMaxHeight().width(90.dp).padding(end = 2.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
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
                    icon = { Icon(imageVector = item.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
                    label = { Text(text = item.label, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = CyberDark,
                        selectedTextColor = NeonGreen,
                        indicatorColor = NeonGreen,
                        unselectedIconColor = CyberCyan.copy(alpha = 0.5f),
                        unselectedTextColor = CyberCyan.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        // VISOR CENTRAL DE MATRIZ
        Box(modifier = Modifier.fillMaxSize().weight(1f).background(CyberDark)) {
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
// 1. GESTOR DE VERSIONES (OPTIMIZADO Y NEÓN)
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
                conn.connectTimeout = 8000
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) sb.append(line)
                reader.close()

                val manifest = JSONObject(sb.toString())
                val versionsArray = manifest.getJSONArray("versions")
                val list = mutableListOf<Pair<String, String>>()
                for (i in 0 until minOf(versionsArray.length(), 40)) { // Filtro de optimización inicial
                    val v = versionsArray.getJSONObject(i)
                    list.add(v.getString("id") to v.getString("type"))
                }
                withContext(Dispatchers.Main) {
                    versions = list
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    status = "ERROR_CONEXIÓN_NÚCLEO"
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        containerColor = CyberDark,
        topBar = {
            TopAppBar(
                title = { Text("LucyMC // MANIFEST_CORE", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberDark, titleContentColor = NeonGreen)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen, strokeWidth = 2.dp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(versions, key = { it.first }) { (id, type) ->
                        VersionCard(id, type) {
                            scope.launch(Dispatchers.IO) {
                                downloadVersion(filesDir, id, type) { msg -> status = msg }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VersionCard(versionId: String, versionType: String, onDownload: () -> Unit) {
    val accent = if (versionType == "release") NeonGreen else CyberCyan
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(2.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = versionId, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = versionType.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = accent, letterSpacing = 1.sp)
            }
            Button(
                onClick = onDownload,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(1.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.4.dp)
            ) {
                Text("FETCH_JAR", fontSize = 9.sp, fontWeight = FontWeight.Black, color = CyberDark)
            }
        }
    }
}

// ==========================================
// 2. PERFILES (DISEÑO HORIZONTAL BI-PANEL)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen() {
    Scaffold(
        containerColor = CyberDark,
        topBar = {
            TopAppBar(
                title = { Text("LucyMC // INSTANCE_DIRECTOR", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberDark, titleContentColor = CyberCyan)
            )
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(modifier = Modifier.weight(1f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = CyberSurface), border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.2f)), shape = RoundedCornerShape(2.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("NÚCLEO ACTIVO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = CyberCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Fabric-Loader-1.20.1", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("ESTADO: EN LA LÍNEA", fontSize = 9.sp, color = NeonGreen)
                }
            }
            Card(modifier = Modifier.weight(1.2f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = CyberPanel), border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.15f)), shape = RoundedCornerShape(2.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("INJECT NUEVO PERFIL", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeonGreen)
                    OutlinedTextField(
                        value = "", onValueChange = {}, label = { Text("ALIAS PROD", color = CyberCyan.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, unfocusedBorderColor = CyberSurface),
                        modifier = Modifier.fillMaxWidth().height(48.dp), singleLine = true
                    )
                    Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = NeonGreen), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(1.dp)) {
                        Text("COMPILAR INSTANCIA", color = CyberDark, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. INYECTOR DE MODS (.JAR DETECTADOS)
// ==========================================
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
                items(mockMods, key = { it }) { mod ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CyberSurface), border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.1f)), shape = RoundedCornerShape(1.dp)) {
                        Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(mod, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            Text("LOADED", fontSize = 9.sp, fontWeight = FontWeight.Black, color = NeonGreen)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. IDENTIDAD DE CUENTA (ELIMINADO FANTASMA)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen() {
    var usernameInput by remember { mutableStateOf("") }
    var isLoggedIn by remember { mutableStateOf(false) }
    var activeUser by remember { mutableStateOf("INVITADO_X") }
    var sessionType by remember { mutableStateOf("NOT_FOUND") }

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
                    Icon(imageVector = Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(40.dp), tint = if (isLoggedIn) NeonGreen else CyberCyan.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = activeUser.uppercase(), fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                    Text(text = "AUTH: $sessionType", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isLoggedIn) NeonGreen else CyberCyan)
                }
            }
            Card(modifier = Modifier.weight(1.2f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = CyberPanel), border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.15f)), shape = RoundedCornerShape(2.dp)) {
                Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("AUTENTICAR LOG", fontSize = 10.sp, fontWeight = FontWeight.Black, color = CyberCyan)
                    OutlinedTextField(value = usernameInput, onValueChange = { usernameInput = it }, label = { Text("ALIAS") }, modifier = Modifier.fillMaxWidth().height(48.dp), singleLine = true)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(onClick = { if (usernameInput.isNotBlank()) { activeUser = usernameInput; sessionType = "LOCAL"; isLoggedIn = true; usernameInput = "" } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = CyberSurface), border = BorderStroke(1.dp, CyberCyan), shape = RoundedCornerShape(1.dp)) {
                            Text("LOCAL", fontSize = 9.sp, color = CyberCyan)
                        }
                        Button(onClick = { if (usernameInput.isNotBlank()) { activeUser = usernameInput; sessionType = "MICROSOFT"; isLoggedIn = true; usernameInput = "" } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen), shape = RoundedCornerShape(1.dp)) {
                            Text("MS_LOGIN", fontSize = 9.sp, color = CyberDark, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. AJUSTES (RUTAS FIJAS DE CONFIGURACIÓN)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
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
            OutlinedTextField(value = "/data/data/com.termux/files/home/.minecraft", onValueChange = {}, modifier = Modifier.fillMaxWidth(), readOnly = true)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("MODO DESARROLLADOR LOGS", fontSize = 12.sp, color = Color.White)
                Switch(checked = true, onCheckedChange = {}, colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen))
            }
        }
    }
}

// ==========================================
// 6. ASIGNACIÓN HARDWARE (JVM TUNING SLIDER)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwareScreen() {
    var ramAllocation by remember { mutableStateOf(3072f) }
    Scaffold(
        containerColor = CyberDark,
        topBar = {
            TopAppBar(
                title = { Text("LucyMC // ALLOC_RESOURCES", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberDark, titleContentColor = NeonGreen)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(14.dp), verticalArrangement = Arrangement.Center) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CyberSurface), border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f)), shape = RoundedCornerShape(2.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("ASIGNACIÓN LÍMITE DE RAM JVM (-Xmx)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = CyberCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${ramAllocation.toInt()} MB ALLOCATED", fontSize = 18.sp, fontWeight = FontWeight.Black, color = NeonGreen)
                    Slider(
                        value = ramAllocation, onValueChange = { ramAllocation = it }, valueRange = 1024f..8192f, steps = 7,
                        colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen, inactiveTrackColor = CyberSurface)
                    )
                }
            }
        }
    }
}

// ==========================================
// 7. MATRIX SERVERS (LISTA DE ENLACE)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen() {
    Scaffold(
        containerColor = CyberDark,
        topBar = {
            TopAppBar(
                title = { Text("LucyMC // SERVER_NODES", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberDark, titleContentColor = CyberCyan)
            )
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(modifier = Modifier.weight(1.1f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = CyberPanel), border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.2f)), shape = RoundedCornerShape(2.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("CREAR ENLACE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = CyberCyan)
                    OutlinedTextField(value = "", onValueChange = {}, label = { Text("NODE_IP") }, modifier = Modifier.fillMaxWidth().height(48.dp))
                    Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = CyberCyan), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(1.dp)) {
                        Text("BIND_SERVER", color = CyberDark, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                }
            }
            Card(modifier = Modifier.weight(1f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = CyberSurface), border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.2f)), shape = RoundedCornerShape(2.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("NODO GUARDADO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeonGreen)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("LucyMC Net", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("nodes.lucymc.net:25565", fontSize = 10.sp, color = CyberCyan)
                }
            }
        }
    }
}

// ==========================================
// DESCARGADOR DE BINARIOS COMPLETO (NATIVO)
// ==========================================
suspend fun downloadVersion(filesDir: File, versionId: String, versionType: String, onStatus: (String) -> Unit) {
    try {
        onStatus("CONNECTING_CORE...")
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
        if (versionUrl.isEmpty()) { onStatus("ERR_URL_NOT_FOUND"); return }

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

        val jarFile = File(dir, "$versionId.jar")
        URL(clientUrl).openStream().use { input -> jarFile.outputStream().use { output -> input.copyTo(output) } }
        onStatus("✅ MATRIX_$versionId_ONLINE")
    } catch (e: Exception) {
        onStatus("❌ CORE_FETCH_FAILURE")
    }
}
