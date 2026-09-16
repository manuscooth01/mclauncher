package com.miempresa.mclauncher

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VersionsViewModel(
    private val versionManager: VersionManager,
    private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(VersionsUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<VersionsEffect>()
    val effects = _effects.asSharedFlow()

    val availableLoaders = listOf("Vanilla", "Fabric", "Forge", "OptiFine", "Quilt")

    fun getLoaderVersions(loader: String): List<String> {
        return when (loader) {
            "Fabric" -> listOf("0.16.2", "0.16.1", "0.16.0", "0.15.11")
            "Forge" -> listOf("47.3.0", "47.2.0", "47.1.0")
            "OptiFine" -> listOf("HD_U_H7", "HD_U_H8")
            "Quilt" -> listOf("0.24.0", "0.23.0")
            else -> emptyList()
        }
    }

    init {
        loadVersions()
    }

    fun loadVersions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val cached = versionManager.loadFromCache()
            if (cached != null) {
                _uiState.update {
                    it.copy(
                        versions = cached,
                        installedVersions = versionManager.getInstalledVersionIds(),
                        isLoading = false
                    )
                }
            }

            if (!versionManager.isInternetAvailable()) {
                if (cached == null) _uiState.update { it.copy(isLoading = false) }
                _effects.emit(VersionsEffect.ShowSnackbar("Sin conexión. Mostrando caché."))
                return@launch
            }

            val result = versionManager.fetchVersions()
            result.onSuccess { list ->
                _uiState.update {
                    it.copy(
                        versions = list,
                        installedVersions = versionManager.getInstalledVersionIds(),
                        isLoading = false
                    )
                }
            }.onFailure {
                if (_uiState.value.versions.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false) }
                }
                _effects.emit(VersionsEffect.ShowSnackbar("Error al cargar versiones."))
            }
        }
    }

    fun downloadVersion(versionId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    downloading = true,
                    selectedVersionId = versionId,
                    downloadProgress = VersionManager.DownloadProgress("INIT", 0, 1, "Iniciando...")
                )
            }

            versionManager.downloadVersion(versionId) { progress ->
                _uiState.update { it.copy(downloadProgress = progress) }
            }

            val finalProgress = _uiState.value.downloadProgress
            _uiState.update {
                it.copy(
                    downloading = false,
                    downloadProgress = null,
                    installedVersions = versionManager.getInstalledVersionIds()
                )
            }

            if (finalProgress?.phase == "COMPLETE") {
                _effects.emit(VersionsEffect.ShowSnackbar(finalProgress.detail))
            } else {
                _effects.emit(VersionsEffect.ShowSnackbar(finalProgress?.detail ?: "Error desconocido"))
            }
        }
    }

    fun updateFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectVersion(versionId: String) {
        viewModelScope.launch {
            if (versionManager.isVersionInstalled(versionId)) {
                _uiState.update {
                    it.copy(
                        selectedVersionId = versionId,
                        isBottomSheetOpen = false,
                        selectedLoader = null,
                        selectedLoaderVersion = null
                    )
                }
                _effects.emit(VersionsEffect.ShowSnackbar("Versión $versionId ya instalada"))
                return@launch
            }
            _uiState.update {
                it.copy(
                    selectedVersionId = versionId,
                    isBottomSheetOpen = true,
                    selectedLoader = null,
                    selectedLoaderVersion = null
                )
            }
        }
    }

    fun updateLoader(loader: String?) {
        _uiState.update {
            it.copy(
                selectedLoader = if (loader == "Vanilla") null else loader,
                selectedLoaderVersion = null
            )
        }
    }

    fun updateLoaderVersion(loaderVersion: String) {
        _uiState.update { it.copy(selectedLoaderVersion = loaderVersion) }
    }

    fun confirmSelection() {
        viewModelScope.launch {
            val versionId = _uiState.value.selectedVersionId
            if (versionId != null && !versionManager.isVersionInstalled(versionId)) {
                downloadVersion(versionId)
            }
            _uiState.update { it.copy(isBottomSheetOpen = false) }
        }
    }

    fun cancelSelection() {
        _uiState.update {
            it.copy(
                isBottomSheetOpen = false,
                selectedVersionId = null,
                selectedLoader = null,
                selectedLoaderVersion = null
            )
        }
    }

    fun deleteVersion(versionId: String) {
        viewModelScope.launch {
            versionManager.deleteVersion(versionId)
            _uiState.update { it.copy(installedVersions = versionManager.getInstalledVersionIds()) }
            _effects.emit(VersionsEffect.ShowSnackbar("Versión $versionId eliminada"))
        }
    }

    fun launchGame(ramMb: Int, username: String) {
        viewModelScope.launch {
            val versionId = _uiState.value.selectedVersionId
            if (versionId == null) {
                _effects.emit(VersionsEffect.ShowSnackbar("Selecciona una versión primero"))
                return@launch
            }
            if (!versionManager.isVersionInstalled(versionId)) {
                _effects.emit(VersionsEffect.ShowSnackbar("Descárgala primero"))
                return@launch
            }
            val intent = versionManager.launchGame(versionId, username, ramMb)
            if (intent != null) {
                try {
                    context?.startActivity(intent)
                    _effects.emit(VersionsEffect.ShowSnackbar("Iniciando $versionId..."))
                } catch (_: Exception) {
                    _effects.emit(VersionsEffect.ShowSnackbar("PojavLauncher no encontrado"))
                }
            } else {
                _effects.emit(VersionsEffect.ShowSnackbar("Error al preparar lanzamiento"))
            }
        }
    }
}

data class VersionsUiState(
    val versions: List<Pair<String, String>> = emptyList(),
    val isLoading: Boolean = false,
    val selectedFilter: String = "ALL",
    val searchQuery: String = "",
    val downloading: Boolean = false,
    val downloadProgress: VersionManager.DownloadProgress? = null,
    val selectedVersionId: String? = null,
    val isBottomSheetOpen: Boolean = false,
    val selectedLoader: String? = null,
    val selectedLoaderVersion: String? = null,
    val installedVersions: Set<String> = emptySet()
)

sealed class VersionsEffect {
    data class ShowSnackbar(val message: String) : VersionsEffect()
}
