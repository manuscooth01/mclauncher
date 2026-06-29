package com.miempresa.mclauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LauncherScreen(filesDir = filesDir)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(filesDir: File) {
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
                        "MI LAUNCHER PRO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (status.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Cargando versiones...", fontSize = 16.sp)
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
                                    downloadVersion(filesDir, id, type) { msg ->
                                        status = msg
                                    }
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
fun VersionCard(
    versionId: String,
    versionType: String,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDownload),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = versionId,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = versionType.uppercase(),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onDownload) {
                Text("Descargar")
            }
        }
    }
}

suspend fun downloadVersion(
    filesDir: File,
    versionId: String,
    versionType: String,
    onStatus: (String) -> Unit
) {
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

        if (versionUrl.isEmpty()) {
            onStatus("Error: URL no encontrada")
            return
        }

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
        URL(clientUrl).openStream().use { input ->
            jarFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        onStatus("✅ $versionId listo! JSON + JAR guardados")

    } catch (e: Exception) {
        onStatus("❌ Error: ${e.message}")
    }
}
