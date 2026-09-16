package com.miempresa.mclauncher

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

class VersionManager(private val filesDir: File, appContext: Context) {

    private val context: Context = appContext.applicationContext

    companion object {
        private const val MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json"
        private const val ASSETS_URL = "https://resources.download.minecraft.net"
        private const val CONNECT_TIMEOUT = 15000
        private const val READ_TIMEOUT = 15000
        private const val PREFS_NAME = "mclauncher_cache"
        private const val KEY_VERSIONS_LIST = "versions_list"
        private const val MAX_VERSIONS_DISPLAY = 100
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

            val max = minOf(versionsArray.length(), MAX_VERSIONS_DISPLAY)
            for (i in 0 until max) {
                val v = versionsArray.getJSONObject(i)
                val type = v.getString("type")
                if (type in VALID_TYPES) {
                    list.add(v.getString("id") to type)
                }
            }

            saveToCache(JSONArray().apply {
                for (i in 0 until max) put(versionsArray.getJSONObject(i))
            })

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
                val type = v.getString("type")
                if (type in VALID_TYPES) {
                    cachedVersions.add(v.getString("id") to type)
                }
            }
            cachedVersions
        } catch (_: Exception) {
            null
        }
    }

    private fun saveToCache(versionsArray: JSONArray) {
        try {
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cacheObject = JSONObject()
            cacheObject.put("versions", versionsArray)
            sharedPrefs.edit().putString(KEY_VERSIONS_LIST, cacheObject.toString()).apply()
        } catch (_: Exception) {
        }
    }

    private fun downloadFile(url: String, dest: File) {
        if (dest.exists() && dest.length() > 0) return
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
        conn.inputStream.use { input ->
            dest.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
        }
    }

    private fun fetchJson(url: String): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
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
        try {
            onProgress(DownloadProgress("MANIFEST", 0, 1, "Obteniendo manifiesto..."))

            val manifest = fetchJson(MANIFEST_URL)
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
                onProgress(DownloadProgress("ERROR", 0, 1, "URL no encontrada"))
                return@withContext
            }

            val versionJson = fetchJson(versionUrl)
            val dir = File(filesDir, "versions/$versionId")
            if (!dir.exists()) dir.mkdirs()

            FileWriter(File(dir, "$versionId.json")).use { it.write(versionJson.toString()) }

            coroutineContext.ensureActive()
            onProgress(DownloadProgress("CLIENT_JAR", 0, 1, "Descargando cliente..."))

            val jarFile = File(dir, "$versionId.jar")
            downloadFile(versionJson.getJSONObject("downloads").getJSONObject("client").getString("url"), jarFile)

            coroutineContext.ensureActive()

            val librariesDir = File(dir, "libraries")
            if (!librariesDir.exists()) librariesDir.mkdirs()

            val libraries = versionJson.getJSONArray("libraries")
            var libCount = 0
            val totalLibs = libraries.length()

            for (i in 0 until totalLibs) {
                coroutineContext.ensureActive()
                val lib = libraries.getJSONObject(i)
                val downloadsLib = lib.optJSONObject("downloads") ?: continue
                val artifact = downloadsLib.optJSONObject("artifact") ?: continue
                val libPath = artifact.getString("path")
                val libSize = artifact.optLong("size", 0)

                val destFile = File(librariesDir, libPath)
                if (!destFile.exists() || destFile.length() != libSize) {
                    destFile.parentFile?.mkdirs()
                    try {
                        downloadFile(artifact.getString("url"), destFile)
                    } catch (_: Exception) {
                    }
                }
                libCount++
                onProgress(DownloadProgress("LIBRARIES", libCount, totalLibs, "Librería $libCount/$totalLibs"))
            }

            coroutineContext.ensureActive()

            val assetIndex = versionJson.optJSONObject("assetIndex")
            if (assetIndex != null) {
                val assetIndexId = assetIndex.getString("id")
                val assetsDir = File(filesDir, "assets")
                val indexesDir = File(assetsDir, "indexes").also { it.mkdirs() }
                val indexFile = File(indexesDir, "$assetIndexId.json")
                downloadFile(assetIndex.getString("url"), indexFile)

                val indexJson = JSONObject(indexFile.readText())
                val objects = indexJson.getJSONObject("objects")
                val assetKeys = objects.keys()
                val assetList = mutableListOf<Pair<String, JSONObject>>()
                while (assetKeys.hasNext()) {
                    val key = assetKeys.next()
                    assetList.add(key to objects.getJSONObject(key))
                }

                val objectsDir = File(assetsDir, "objects").also { it.mkdirs() }
                var assetCount = 0
                val totalAssets = assetList.size

                for ((_, assetInfo) in assetList) {
                    coroutineContext.ensureActive()
                    val hash = assetInfo.getString("hash")
                    val prefix = hash.substring(0, 2)
                    val assetFile = File(objectsDir, "$prefix/$hash")
                    if (!assetFile.exists() || assetFile.length() != assetInfo.getLong("size")) {
                        assetFile.parentFile?.mkdirs()
                        try {
                            downloadFile("$ASSETS_URL/$prefix/$hash", assetFile)
                        } catch (_: Exception) {
                        }
                    }
                    assetCount++
                    if (assetCount % 50 == 0 || assetCount == totalAssets) {
                        onProgress(DownloadProgress("ASSETS", assetCount, totalAssets, "Asset $assetCount/$totalAssets"))
                    }
                }
            }

            generateLaunchProfile(versionId, versionJson)
            onProgress(DownloadProgress("COMPLETE", 1, 1, "✅ $versionId instalado"))
        } catch (e: Exception) {
            onProgress(DownloadProgress("ERROR", 0, 1, "❌ ${e.message ?: "Error desconocido"}"))
        }
    }

    private fun generateLaunchProfile(versionId: String, versionJson: JSONObject) {
        val dir = File(filesDir, "versions/$versionId")
        val librariesDir = File(dir, "libraries")
        val profile = JSONObject()

        profile.put("id", versionId)
        profile.put("mainClass", versionJson.optString("mainClass", "net.minecraft.client.main.Main"))

        val args = versionJson.optJSONObject("arguments")
        if (args != null) {
            val gameArgs = args.optJSONArray("game") ?: JSONArray()
            val jvmArgs = args.optJSONArray("jvm") ?: JSONArray()

            val argList = mutableListOf<String>()
            for (i in 0 until gameArgs.length()) {
                val arg = gameArgs.get(i)
                if (arg is String) argList.add(arg)
            }
            profile.put("gameArgs", JSONArray(argList))

            val jvmArgList = mutableListOf<String>()
            for (i in 0 until jvmArgs.length()) {
                val arg = jvmArgs.get(i)
                if (arg is String) {
                    val resolved = arg
                        .replace("\${auth_player_name}", "Player")
                        .replace("\${version_name}", versionId)
                        .replace("\${game_directory}", filesDir.absolutePath)
                        .replace("\${assets_directory}", File(filesDir, "assets").absolutePath)
                        .replace("\${assets_root}", versionJson.optJSONObject("assetIndex")?.optString("id") ?: "")
                        .replace("\${user_properties}", "{}")
                        .replace("\${auth_uuid}", "0")
                        .replace("\${auth_access_token}", "0")
                    jvmArgList.add(resolved)
                }
            }
            profile.put("jvmArgs", JSONArray(jvmArgList))
        } else {
            val argsStr = versionJson.optString("minecraftArguments", "")
            if (argsStr.isNotEmpty()) {
                profile.put("gameArgs", JSONArray(argsStr.split(" ")))
            }
        }

        val classpath = mutableListOf<String>()
        classpath.add(File(dir, "$versionId.jar").absolutePath)

        val libraries = versionJson.getJSONArray("libraries")
        for (i in 0 until libraries.length()) {
            val lib = libraries.getJSONObject(i)
            val downloads = lib.optJSONObject("downloads")
            val artifact = downloads?.optJSONObject("artifact")
            if (artifact != null) {
                val libFile = File(librariesDir, artifact.getString("path"))
                if (libFile.exists()) classpath.add(libFile.absolutePath)
            }
            val natives = lib.optJSONObject("natives")
            if (natives != null) {
                val nativeClassifier = natives.optString("android", natives.optString("linux", ""))
                if (nativeClassifier.isNotEmpty() && downloads != null) {
                    val classifiers = downloads.optJSONObject("classifiers")
                    val nativeArtifact = classifiers?.optJSONObject(nativeClassifier)
                    if (nativeArtifact != null) {
                        val nativeFile = File(librariesDir, nativeArtifact.getString("path"))
                        if (!nativeFile.exists()) {
                            nativeFile.parentFile?.mkdirs()
                            try { downloadFile(nativeArtifact.getString("url"), nativeFile) } catch (_: Exception) {}
                        }
                        classpath.add(nativeFile.absolutePath)
                    }
                }
            }
        }

        profile.put("classpath", JSONArray(classpath))
        FileWriter(File(dir, "lucymc_profile.json")).use { it.write(profile.toString(2)) }
    }

    fun isVersionInstalled(versionId: String): Boolean {
        val jarFile = File(filesDir, "versions/$versionId/$versionId.jar")
        val profileFile = File(filesDir, "versions/$versionId/lucymc_profile.json")
        return jarFile.exists() && jarFile.length() > 0 && profileFile.exists()
    }

    fun getInstalledVersionIds(): Set<String> {
        val versionsDir = File(filesDir, "versions")
        if (!versionsDir.exists()) return emptySet()
        return versionsDir.listFiles()
            ?.filter { it.isDirectory && File(it, "${it.name}.jar").exists() }
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()
    }

    fun deleteVersion(versionId: String): Boolean {
        return File(filesDir, "versions/$versionId").deleteRecursively()
    }

    fun launchGame(versionId: String, username: String, ramMb: Int): Intent? {
        val profileFile = File(filesDir, "versions/$versionId/lucymc_profile.json")
        if (!profileFile.exists()) return null

        val profile = JSONObject(profileFile.readText())
        val mainClass = profile.getString("mainClass")
        val classpathList = mutableListOf<String>()
        val cpArray = profile.optJSONArray("classpath")
        if (cpArray != null) {
            for (i in 0 until cpArray.length()) classpathList.add(cpArray.getString(i))
        }

        val gameArgsList = mutableListOf<String>()
        val gameArgs = profile.optJSONArray("gameArgs")
        if (gameArgs != null) {
            for (i in 0 until gameArgs.length()) {
                var arg = gameArgs.getString(i)
                arg = arg.replace("\${auth_player_name}", username)
                    .replace("\${version_name}", versionId)
                    .replace("\${game_directory}", filesDir.absolutePath)
                    .replace("\${assets_directory}", File(filesDir, "assets").absolutePath)
                    .replace("\${user_properties}", "{}")
                    .replace("\${auth_uuid}", "0")
                    .replace("\${auth_access_token}", "0")
                gameArgsList.add(arg)
            }
        }

        val jvmArgs = mutableListOf(
            "-Xmx${ramMb}m",
            "-Xms512m",
            "-Djava.library.path=${File(filesDir, "versions/$versionId/natives").absolutePath}",
            "-cp", classpathList.joinToString(":")
        )

        return try {
            Intent(Intent.ACTION_VIEW).apply {
                setClassName("net.kdt.pojavlaunch", "net.kdt.pojavlaunch.PojavLauncherActivity")
                putExtra("launch_version", versionId)
                putExtra("username", username)
                putExtra("java_args", jvmArgs.joinToString(" "))
                putExtra("classpath", classpathList.joinToString(":"))
                putExtra("main_class", mainClass)
                putExtra("game_args", gameArgsList.joinToString(" "))
                putExtra("game_dir", filesDir.absolutePath)
            }
        } catch (_: Exception) {
            null
        }
    }
}
