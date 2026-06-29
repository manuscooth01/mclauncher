package com.miempresa.mclauncher

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class VersionManager(private val filesDir: File, private val context: Context) {

    companion object {
        private const val MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json"
        private const val CONNECT_TIMEOUT = 8000
        private const val READ_TIMEOUT = 8000
        private const val PREFS_NAME = "mclauncher_cache"
        private const val KEY_VERSIONS_LIST = "versions_list"
    }

    fun isInternetAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }

    suspend fun fetchVersions(): Result<List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        try {
            val url = URL(MANIFEST_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) sb.append(line)
            reader.close()

            val manifest = JSONObject(sb.toString())
            val versionsArray = manifest.getJSONArray("versions")
            val list = mutableListOf<Pair<String, String>>()

            // TODAS las versiones (sin límite)
            for (i in 0 until versionsArray.length()) {
                val v = versionsArray.getJSONObject(i)
                list.add(v.getString("id") to v.getString("type"))
            }

            saveToCache(versionsArray)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun loadFromCache(): List<Pair<String, String>>? {
        return try {
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cachedData = sharedPrefs.getString(KEY_VERSIONS_LIST, null) ?: return null

            val jsonArray = JSONObject(cachedData).getJSONArray("versions")
            val cachedVersions = mutableListOf<Pair<String, String>>()
            for (i in 0 until jsonArray.length()) {
                val v = jsonArray.getJSONObject(i)
                cachedVersions.add(v.getString("id") to v.getString("type"))
            }
            cachedVersions
        } catch (e: Exception) {
            null
        }
    }

    private fun saveToCache(versionsArray: org.json.JSONArray) {
        try {
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cacheObject = JSONObject()
            cacheObject.put("versions", versionsArray)
            sharedPrefs.edit().putString(KEY_VERSIONS_LIST, cacheObject.toString()).apply()
        } catch (e: Exception) {
            // Error al guardar en caché, ignorar
        }
    }

    suspend fun downloadVersion(versionId: String, onStatus: (String) -> Unit) = withContext(Dispatchers.IO) {
        try {
            onStatus("CONNECTING_CORE...")

            // 1. Obtener la URL del manifiesto de la versión
            val manifestUrl = URL(MANIFEST_URL)
            val conn = manifestUrl.openConnection() as HttpURLConnection
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT

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
                onStatus("ERR_URL_NOT_FOUND")
                return@withContext
            }

            // 2. Obtener el JSON de la versión específica
            val vConn = URL(versionUrl).openConnection() as HttpURLConnection
            val vReader = BufferedReader(InputStreamReader(vConn.inputStream))
            val vSb = StringBuilder()
            while (vReader.readLine().also { line = it } != null) vSb.append(line)
            vReader.close()

            val versionJson = vSb.toString()

            // 3. Guardar el JSON de la versión
            val dir = File(filesDir, "versions/$versionId")
            if (!dir.exists()) dir.mkdirs()
            FileWriter(File(dir, "$versionId.json")).use { it.write(versionJson) }

            // 4. Descargar el JAR del cliente
            val downloads = JSONObject(versionJson).getJSONObject("downloads")
            val client = downloads.getJSONObject("client")
            val clientUrl = client.getString("url")

            val jarFile = File(dir, "$versionId.jar")
            URL(clientUrl).openStream().use { input ->
                jarFile.outputStream().use { output -> input.copyTo(output) }
            }

            onStatus("✅ MATRIX_${versionId}_ONLINE")
        } catch (e: Exception) {
            onStatus("❌ CORE_FETCH_FAILURE")
        }
    }
}
