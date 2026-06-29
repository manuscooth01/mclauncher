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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.miempresa.mclauncher.ui.theme.LucyMcTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
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

// Estructura de datos interna para las pantallas del HUD táctico
data class NavigationItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun MainNavigationContainer(filesDir: File) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Las 7 pantallas oficiales extraídas de tu documento técnico
    val navItems = listOf(
        NavigationItem("versiones", "VERSIONS", Icons.Filled.List),
        NavigationItem("perfiles", "PROFILES", Icons.Filled.AccountBox),
        NavigationItem("mods", "MODS", Icons.Filled.Star),
        NavigationItem("cuenta", "ACCOUNT", Icons.Filled.Lock),
        NavigationItem("ajustes", "SETTINGS", Icons.Filled.Settings),
        NavigationItem("hardware", "HARDWARE", Icons.Filled.Warning),
        NavigationItem("servidores", "SERVERS", Icons.Filled.Home)
    )

    Row(modifier = Modifier.fillMaxSize()) {
        // BARRA LATERAL (Navigation Rail) - Optimizado para agarre horizontal
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight().width(82.dp)
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
                    icon = { 
                        Icon(
                            imageVector = item.icon, 
                            contentDescription = item.label,
                            modifier = Modifier.size(20.dp)
                        ) 
                    },
                    label = { 
                        Text(
                            text = item.label, 
                            fontSize = 9.sp, 
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        ) 
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.background,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        // PANEL CENTRAL - El espacio cambia dinámicamente según la ruta activa
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            NavHost(navController = navController, startDestination = "versiones") {
                composable("versiones") { VersionsScreen(filesDir) }
                composable("perfiles") { CyberpunkPlaceholderScreen("PANEL: PERFILES DE JUEGO") }
                composable("mods") { CyberpunkPlaceholderScreen("MODESTACIÓN: GESTOR DE MODS") }
                composable("cuenta") { CyberpunkPlaceholderScreen("AUTENTICACIÓN: CONTROL DE CUENTA") }
                composable("ajustes") { CyberpunkPlaceholderScreen("CONFIGURACIÓN DEL SISTEMA") }
                composable("hardware") { CyberpunkPlaceholderScreen("MONITOR DE HARDWARE & RAM") }
                composable("servidores") { CyberpunkPlaceholderScreen("CONEXIÓN A SERVIDORES MULTIJUGADOR") }
            }
        }
    }
}

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
                title = {
                    Text(
                        "LucyMC // NÚCLEO",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 2.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (status.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text(text = status, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("SINCRO DE RED...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, letterSpacing = 2.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = versionId, fontSize = 16.sp, ... )
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = versionType.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeColor, letterSpacing = 1.sp)
            }
            Button(
                onClick = onDownload,
                colors = ButtonDefaults.buttonColors(containerColor = badgeColor, contentColor = MaterialTheme.colorScheme.onPrimary),
                shape = RoundedCornerShape(2.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("OBTENER", fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

// Pantalla táctica temporal para las secciones que iremos programando paso a paso
@Composable
fun CyberpunkPlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "MÓDULO EN DESARROLLO // PROTOCOLO FASE 1", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

suspend fun downloadVersion(filesDir: File, versionId: String, versionType: String, onStatus: (String) -> Unit) {
    // El código de descarga se mantiene exactamente igual para asegurar compatibilidad total...
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
