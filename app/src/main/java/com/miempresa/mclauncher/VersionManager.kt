package com.miempresa.mclauncher

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class VersionManager(private val filesDir: File, appContext: Context) {
    private val context: Context = appContext.applicationContext

    companion object {
        private const val MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json"
        private const val ASSETS_URL = "https://resources.download.minecraft.net"
        private const val CONNECT_TIMEOUT = 10000
        private const val READ_TIMEOUT = 20000
        private const val PREFS_NAME = "mclauncher_cache"
        private const val KEY_VERSIONS_LIST = "versions_list"
        private const val MAX_VERSIONS = 15
        private val VALID_TYPES = setOf("release", "snapshot", "old_beta", "old_alpha")
    }

    data class DownloadProgress(
        val phase: String,
        val current: Int,
        val total: Int,
        val detail: String = ""
    )

    fun isInternetAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork
            val cap = cm.getNetworkCapabilities(network)
            cap?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }

    suspend fun fetchVersions(): Result<List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        try {
            val conn = URL(MANIFEST_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT
            conn.useCaches = false

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) sb.append(line)
            reader.close()

            val manifest = JSONObject(sb.toString())
            val versionsArray = manifest.getJSONArray("versions")
            val list = mutableListOf<Pair<String, String>>()

            for (i in 0 until versionsArray.length().coerceAtMost(MAX_VERSIONS)) {
                val v = versionsArray.getJSONObject(i)
                val type = v.getString("type")
                if (type in VALID_TYPES) {
                    list.add(v.getString("id") to type)
                }
            }

            saveToCache(versionsArray, list.size)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun loadFromCache(): List<Pair<String, String>>? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cached = prefs.getString(KEY_VERSIONS_LIST, null) ?: return null
            val json = JSONObject(cached)
            val arr = json.getJSONArray("versions")
            val list = mutableListOf<Pair<String, String>>()
            for (i in 0 until arr.length()) {
                val v = arr.getJSONObject(i)
                val type = v.getString("type")
                if (type in VALID_TYPES) list.add(v.getString("id") to type)
            }
            list
        } catch (_: Exception) { null }
    }

    private fun saveToCache(versionsArray: JSONArray, count: Int) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cache = JSONObject().put("versions", JSONArray().apply {
                for (i in 0 until count) put(versionsArray.getJSONObject(i))
            })
            prefs.edit().putString(KEY_VERSIONS_LIST, cache.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun downloadFile(url: String, dest: File): Boolean {
        if (dest.exists() && dest.length() > 0) return true
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT
            conn.useCaches = false

            dest.parentFile?.mkdirs()
            conn.inputStream.use { input ->
                FileOutputStream(dest).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                }
            }
            return true
        } catch (_: Exception) {
            return false
        }
    }

    private fun fetchJson(url: String): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
        conn.useCaches = false
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) sb.append(line)
        reader.close()
        return JSONObject(sb.toString())
    }

    suspend fun downloadVersion(
        versionId: String,
        onProgress: suspend (DownloadProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        var lastEmit = 0L
        suspend fun emit(p: DownloadProgress) {
            val now = System.currentTimeMillis()
            if (now - lastEmit >= 100 || p.current == p.total) {
                onProgress(p)
                lastEmit = now
            }
        }

        try {
            emit(DownloadProgress("MANIFEST", 0, 1, "Obteniendo manifiesto..."))

            val manifest = fetchJson(MANIFEST_URL)
            var versionUrl = ""
            val versions = manifest.getJSONArray("versions")
            for (i in 0 until versions.length()) {
                val v = versions.getJSONObject(i)
                if (v.getString("id") == versionId) {
                    versionUrl = v.getString("url")
                    break
                }
            }
            if (versionUrl.isEmpty()) { emit(DownloadProgress("ERROR", 0, 1, "URL no encontrada")); return@withContext }

            val versionJson = fetchJson(versionUrl)
            val dir = File(filesDir, "versions/$versionId")
            dir.mkdirs()
            FileWriter(File(dir, "$versionId.json")).use { it.write(versionJson.toString()) }

            emit(DownloadProgress("CLIENT_JAR", 0, 1, "Descargando cliente..."))
            val jarFile = File(dir, "$versionId.jar")
            downloadFile(versionJson.getJSONObject("downloads").getJSONObject("client").getString("url"), jarFile)

            val libsDir = File(dir, "libraries").apply { mkdirs() }
            val libraries = versionJson.getJSONArray("libraries")
            val totalLibs = libraries.length()
            for (i in 0 until totalLibs) {
                val lib = libraries.getJSONObject(i)
                val dl = lib.optJSONObject("downloads") ?: continue
                val art = dl.optJSONObject("artifact") ?: continue
                val path = art.getString("path")
                val size = art.optLong("size", 0)
                val dest = File(libsDir, path)
                if (!dest.exists() || dest.length() != size) {
                    dest.parentFile?.mkdirs()
                    downloadFile(art.getString("url"), dest)
                }
                emit(DownloadProgress("LIBRARIES", i + 1, totalLibs, "Lib ${i + 1}/$totalLibs"))
            }

            val assetIndex = versionJson.optJSONObject("assetIndex")
            if (assetIndex != null) {
                val assetId = assetIndex.getString("id")
                val assetsDir = File(filesDir, "assets")
                val indexesDir = File(assetsDir, "indexes").apply { mkdirs() }
                val indexFile = File(indexesDir, "$assetId.json")
                downloadFile(assetIndex.getString("url"), indexFile)

                val idxJson = JSONObject(indexFile.readText())
                val objects = idxJson.getJSONObject("objects")
                val keys = mutableListOf<String>()
                val iter = objects.keys()
                while (iter.hasNext()) keys.add(iter.next())
                val totalAssets = keys.size

                val objectsDir = File(assetsDir, "objects").apply { mkdirs() }
                for (i in keys.indices) {
                    val info = objects.getJSONObject(keys[i])
                    val hash = info.getString("hash")
                    val prefix = hash.substring(0, 2)
                    val assetFile = File(objectsDir, "$prefix/$hash")
                    if (!assetFile.exists() || assetFile.length() != info.getLong("size")) {
                        assetFile.parentFile?.mkdirs()
                        downloadFile("$ASSETS_URL/$prefix/$hash", assetFile)
                    }
                    if (i % 50 == 0 || i == totalAssets - 1) {
                        emit(DownloadProgress("ASSETS", i + 1, totalAssets, "Asset ${i + 1}/$totalAssets"))
                    }
                }
            }

            generateLaunchProfile(versionId, versionJson)
            emit(DownloadProgress("COMPLETE", 1, 1, "✅ $versionId lista"))
        } catch (e: Exception) {
            emit(DownloadProgress("ERROR", 0, 1, "❌ ${e.message ?: "Error"}"))
        }
    }

    private fun generateLaunchProfile(versionId: String, vJson: JSONObject) {
        val dir = File(filesDir, "versions/$versionId")
        val libsDir = File(dir, "libraries")
        val profile = JSONObject()

        profile.put("id", versionId)
        profile.put("mainClass", vJson.optString("mainClass", "net.minecraft.client.main.Main"))

        val args = vJson.optJSONObject("arguments")
        if (args != null) {
            val gameArgs = args.optJSONArray("game") ?: JSONArray()
            val jvmArgs = args.optJSONArray("jvm") ?: JSONArray()
            val gList = mutableListOf<String>()
            val jList = mutableListOf<String>()
            for (i in 0 until gameArgs.length()) { val a = gameArgs.get(i); if (a is String) gList.add(a) }
            for (i in 0 until jvmArgs.length()) {
                val a = jvmArgs.get(i)
                if (a is String) {
                    jList.add(a
                        .replace("\${auth_player_name}", "Player")
                        .replace("\${version_name}", versionId)
                        .replace("\${game_directory}", filesDir.absolutePath)
                        .replace("\${assets_directory}", File(filesDir, "assets").absolutePath)
                        .replace("\${assets_root}", vJson.optJSONObject("assetIndex")?.optString("id") ?: "")
                        .replace("\${user_properties}", "{}")
                        .replace("\${auth_uuid}", "0")
                        .replace("\${auth_access_token}", "0"))
                }
            }
            profile.put("gameArgs", JSONArray(gList))
            profile.put("jvmArgs", JSONArray(jList))
        } else {
            val aStr = vJson.optString("minecraftArguments", "")
            if (aStr.isNotEmpty()) profile.put("gameArgs", JSONArray(aStr.split(" ")))
        }

        val cp = mutableListOf<String>()
        cp.add(File(dir, "$versionId.jar").absolutePath)
        val libs = vJson.getJSONArray("libraries")
        for (i in 0 until libs.length()) {
            val lib = libs.getJSONObject(i)
            val dl = lib.optJSONObject("downloads")
            val art = dl?.optJSONObject("artifact")
            if (art != null) {
                val f = File(libsDir, art.getString("path"))
                if (f.exists()) cp.add(f.absolutePath)
            }
            val natives = lib.optJSONObject("natives")
            if (natives != null) {
                val cls = natives.optString("android", natives.optString("linux", ""))
                if (cls.isNotEmpty() && dl != null) {
                    val classif = dl.optJSONObject("classifiers")
                    val nat = classif?.optJSONObject(cls)
                    if (nat != null) {
                        val nf = File(libsDir, nat.getString("path"))
                        if (!nf.exists()) { nf.parentFile?.mkdirs(); downloadFile(nat.getString("url"), nf) }
                        cp.add(nf.absolutePath)
                    }
                }
            }
        }
        profile.put("classpath", JSONArray(cp))
        FileWriter(File(dir, "lucymc_profile.json")).use { it.write(profile.toString()) }
    }

    fun isVersionInstalled(versionId: String): Boolean {
        val jar = File(filesDir, "versions/$versionId/$versionId.jar")
        val prof = File(filesDir, "versions/$versionId/lucymc_profile.json")
        return jar.exists() && jar.length() > 0 && prof.exists()
    }

    suspend fun getInstalledVersionIds(): Set<String> = withContext(Dispatchers.IO) {
        val dir = File(filesDir, "versions")
        if (!dir.exists()) return@withContext emptySet()
        dir.listFiles()?.filter { it.isDirectory && File(it, "${it.name}.jar").exists() }?.map { it.name }?.toSet() ?: emptySet()
    }

    fun deleteVersion(versionId: String): Boolean = File(filesDir, "versions/$versionId").deleteRecursively()

    fun launchGame(versionId: String, username: String, ramMb: Int): Intent? {
        val prof = File(filesDir, "versions/$versionId/lucymc_profile.json")
        if (!prof.exists()) return null
        val p = JSONObject(prof.readText())
        val main = p.getString("mainClass")
        val cp = mutableListOf<String>()
        p.optJSONArray("classpath")?.let { for (i in 0 until it.length()) cp.add(it.getString(i)) }
        val gArgs = mutableListOf<String>()
        p.optJSONArray("gameArgs")?.let { for (i in 0 until it.length()) gArgs.add(it.getString(i)
            .replace("\${auth_player_name}", username)
            .replace("\${version_name}", versionId)
            .replace("\${game_directory}", filesDir.absolutePath)
            .replace("\${assets_directory}", File(filesDir, "assets").absolutePath)
            .replace("\${user_properties}", "{}")
            .replace("\${auth_uuid}", "0")
            .replace("\${auth_access_token}", "0")) }
        val jArgs = listOf("-Xmx${ramMb}m", "-Xms256m", "-Djava.library.path=${File(filesDir, "versions/$versionId/natives").absolutePath}", "-cp", cp.joinToString(":"))
        return try {
            Intent(Intent.ACTION_VIEW).apply {
                setClassName("net.kdt.pojavlaunch", "net.kdt.pojavlaunch.PojavLauncherActivity")
                putExtra("launch_version", versionId)
                putExtra("username", username)
                putExtra("java_args", jArgs.joinToString(" "))
                putExtra("classpath", cp.joinToString(":"))
                putExtra("main_class", main)
                putExtra("game_args", gArgs.joinToString(" "))
                putExtra("game_dir", filesDir.absolutePath)
            }
        } catch (_: Exception) { null }
    }
}