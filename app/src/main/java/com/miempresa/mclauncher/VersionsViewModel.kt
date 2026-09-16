package com.miempresa.mclauncher

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VersionsViewModel(
    private val versionManager: VersionManager,
    private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(VersionsUiState())
    val uiState = _uiState.asStateFlow()

    private val _selectedVersion = MutableStateFlow<VersionSelection?>(null)
    val selectedVersion = _selectedVersion.asStateFlow()

    private val _isBottomSheetOpen = MutableStateFlow(false)
    val isBottomSheetOpen = _isBottomSheetOpen.asStateFlow()

    private val _effects = MutableSharedFlow<VersionsEffect>()
    val effects = _effects.asSharedFlow()

    val availableLoaders = listOf("Vanilla", "Fabric", "Forge", "OptiFine", "Quilt")

    fun getLoaderVersions(loader: String, mcVersion: String): List<String> {
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
            _uiState.value = _uiState.value.copy(isLoading = true)

            val cached = versionManager.loadFromCache()
            if (cached != null) {
                _uiState.value = _uiState.value.copy(
                    versions = cached,
                    isLoading = false
                )
            }

            if (!versionManager.isInternetAvailable()) {
                if (cached == null) _uiState.value = _uiState.value.copy(isLoading = false)
                _effects.emit(VersionsEffect.ShowSnackbar("Sin conexión. Mostrando caché."))
                return@launch
            }

            val result = versionManager.fetchVersions()
            result.onSuccess { list ->
                _uiState.value = _uiState.value.copy(
                    versions = list,
                    isLoading = false
                )
            }.onFailure {
                if (_uiState.value.versions.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                _effects.emit(VersionsEffect.ShowSnackbar("Error al cargar versiones."))
            }
        }
    }

    fun downloadVersion(versionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                downloading = true,
                downloadProgress = VersionManager.DownloadProgress("INIT", 0, 1, "Iniciando...")
            )

            versionManager.downloadVersion(versionId) { progress ->
                _uiState.value = _uiState.value.copy(downloadProgress = progress)
            }

            val finalProgress = _uiState.value.downloadProgress
            _uiState.value = _uiState.value.copy(
                downloading = false,
                downloadProgress = null
            )

            if (finalProgress?.phase == "COMPLETE") {
                _effects.emit(VersionsEffect.ShowSnackbar(finalProgress.detail))
            } else {
                _effects.emit(VersionsEffect.ShowSnackbar(finalProgress?.detail ?: "Error desconocido"))
            }
        }
    }

    fun updateFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun selectVersion(versionId: String) {
        viewModelScope.launch {
            if (isVersionInstalled(versionId)) {
                _selectedVersion.value = VersionSelection(versionId, null, null)
                _isBottomSheetOpen.value = false
                _effects.emit(VersionsEffect.ShowSnackbar("Versión $versionId ya instalada"))
                return@launch
            }
            _selectedVersion.value = VersionSelection(versionId)
            _isBottomSheetOpen.value = true
        }
    }

    fun updateLoader(loader: String?) {
        val current = _selectedVersion.value ?: return
        _selectedVersion.value = current.copy(
            loader = if (loader == "Vanilla") null else loader,
            loaderVersion = null
        )
    }

    fun updateLoaderVersion(loaderVersion: String) {
        val current = _selectedVersion.value ?: return
        _selectedVersion.value = current.copy(loaderVersion = loaderVersion)
    }

    fun confirmSelection() {
        viewModelScope.launch {
            val selection = _selectedVersion.value
            if (selection != null) {
                if (!isVersionInstalled(selection.versionId)) {
                    downloadVersion(selection.versionId)
                }
            }
            _isBottomSheetOpen.value = false
        }
    }

    fun cancelSelection() {
        viewModelScope.launch {
            _isBottomSheetOpen.value = false
            _selectedVersion.value = null
        }
    }

    fun isVersionInstalled(versionId: String): Boolean {
        return versionManager.isVersionInstalled(versionId)
    }

    fun deleteVersion(versionId: String) {
        viewModelScope.launch {
            if (versionManager.deleteVersion(versionId)) {
                _effects.emit(VersionsEffect.ShowSnackbar("Versión $versionId eliminada"))
            }
        }
    }

    fun launchGame(ramMb: Int, username: String) {
        viewModelScope.launch {
            val selection = _selectedVersion.value
            if (selection == null) {
                _effects.emit(VersionsEffect.ShowSnackbar("Selecciona una versión primero"))
                return@launch
            }
            if (!isVersionInstalled(selection.versionId)) {
                _effects.emit(VersionsEffect.ShowSnackbar("La versión no está instalada. Descárgala primero."))
                return@launch
            }

            val intent = versionManager.launchGame(selection.versionId, username, ramMb)
            if (intent != null) {
                try {
                    context?.startActivity(intent)
                    _effects.emit(VersionsEffect.ShowSnackbar("Iniciando ${selection.displayName()}..."))
                } catch (e: Exception) {
                    _effects.emit(VersionsEffect.ShowSnackbar(
                        "PojavLauncher no encontrado. Instálalo desde F-Droid."
                    ))
                }
            } else {
                _effects.emit(VersionsEffect.ShowSnackbar(
                    "Error al preparar el lanzamiento. Reintenta la instalación."
                ))
            }
        }
    }

    fun launchGameSimple() {
        viewModelScope.launch {
            val selection = _selectedVersion.value
            if (selection == null) {
                _effects.emit(VersionsEffect.ShowSnackbar("Selecciona una versión primero"))
                return@launch
            }
            if (!isVersionInstalled(selection.versionId)) {
                _effects.emit(VersionsEffect.ShowSnackbar("La versión no está instalada. Descárgala primero."))
                return@launch
            }
            _effects.emit(VersionsEffect.ShowSnackbar("✅ ${selection.displayName()} está listo para jugar"))
        }
    }
}

data class VersionSelection(
    val versionId: String,
    val loader: String? = null,
    val loaderVersion: String? = null
) {
    fun displayName(): String {
        return if (loader != null && loaderVersion != null) {
            "$versionId + $loader $loaderVersion"
        } else {
            versionId
        }
    }
}

data class VersionsUiState(
    val versions: List<Pair<String, String>> = emptyList(),
    val isLoading: Boolean = false,
    val selectedFilter: String = "ALL",
    val searchQuery: String = "",
    val downloading: Boolean = false,
    val downloadProgress: VersionManager.DownloadProgress? = null
)

sealed class VersionsEffect {
    data class ShowSnackbar(val message: String) : VersionsEffect()
}
