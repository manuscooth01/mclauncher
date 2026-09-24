package com.miempresa.mclauncher

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VersionsViewModel(
    val versionManager: VersionManager,
    private val context: Context? = null
) : ViewModel() {

    var versions: List<Pair<String, String>> = emptyList()
    var installedVersions: Set<String> = emptySet()
    var isLoading = false
    var downloading = false
    var downloadProgress: VersionManager.DownloadProgress? = null
    var selectedVersionId: String? = null
    var selectedFilter = 0
    var searchQuery = ""
    var showBottomSheet = false
    var selectedLoader: String? = null
    var selectedLoaderVersion: String? = null

    val availableLoaders = listOf("Vanilla", "Fabric", "Forge", "OptiFine", "Quilt")

    fun getLoaderVersions(loader: String): List<String> = when (loader) {
        "Fabric" -> listOf("0.16.2", "0.16.1", "0.16.0", "0.15.11")
        "Forge" -> listOf("47.3.0", "47.2.0", "47.1.0")
        "OptiFine" -> listOf("HD_U_H7", "HD_U_H8")
        "Quilt" -> listOf("0.24.0", "0.23.0")
        else -> emptyList()
    }

    init { loadVersions() }

    fun loadVersions() {
        isLoading = true
        viewModelScope.launch {
            val cached = versionManager.loadFromCache()
            if (cached != null) {
                versions = cached
                installedVersions = withContext(Dispatchers.IO) { versionManager.getInstalledVersionIds() }
            }
            if (!versionManager.isInternetAvailable()) {
                isLoading = false
                return@launch
            }
            versionManager.fetchVersions().onSuccess { list ->
                versions = list
                installedVersions = withContext(Dispatchers.IO) { versionManager.getInstalledVersionIds() }
                isLoading = false
            }.onFailure {
                if (versions.isEmpty()) isLoading = false
            }
        }
    }

    fun downloadVersion(versionId: String) {
        downloading = true
        downloadProgress = VersionManager.DownloadProgress("INIT", 0, 1, "Iniciando...")
        viewModelScope.launch {
            versionManager.downloadVersion(versionId) { progress ->
                downloadProgress = progress
            }
            installedVersions = withContext(Dispatchers.IO) { versionManager.getInstalledVersionIds() }
            downloading = false
            downloadProgress = null
        }
    }

    fun updateFilter(i: Int) { selectedFilter = i }
    fun updateSearch(q: String) { searchQuery = q }

    fun selectVersion(id: String) {
        if (versionManager.isVersionInstalled(id)) {
            selectedVersionId = id
        } else {
            selectedVersionId = id
            selectedLoader = null
            selectedLoaderVersion = null
            showBottomSheet = true
        }
    }

    fun updateLoader(l: String?) { selectedLoader = l; selectedLoaderVersion = null }
    fun updateLoaderVersion(v: String) { selectedLoaderVersion = v }

    fun confirmDownload() {
        if (selectedVersionId != null) {
            downloadVersion(selectedVersionId!!)
            showBottomSheet = false
        }
    }

    fun cancelSelection() {
        showBottomSheet = false
        selectedVersionId = null
        selectedLoader = null
        selectedLoaderVersion = null
    }

    fun deleteVersion(id: String) {
        viewModelScope.launch {
            versionManager.deleteVersion(id)
            installedVersions = withContext(Dispatchers.IO) { versionManager.getInstalledVersionIds() }
        }
    }

    fun launchGame(ramMb: Int, username: String) {
        viewModelScope.launch {
            val id = selectedVersionId ?: return@launch
            if (!versionManager.isVersionInstalled(id)) return@launch
            val intent = versionManager.launchGame(id, username, ramMb)
            context?.let { it.startActivity(intent!!) }
        }
    }
}