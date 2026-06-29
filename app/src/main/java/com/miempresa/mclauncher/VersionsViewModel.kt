package com.miempresa.mclauncher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VersionsViewModel(
    private val versionManager: VersionManager
) : ViewModel() {

    // Estados
    private val _uiState = MutableStateFlow(VersionsUiState())
    val uiState = _uiState.asStateFlow()

    private val _selectedVersion = MutableStateFlow<VersionSelection?>(null)
    val selectedVersion = _selectedVersion.asStateFlow()

    private val _isBottomSheetOpen = MutableStateFlow(false)
    val isBottomSheetOpen = _isBottomSheetOpen.asStateFlow()

    // Efectos (snackbars)
    private val _effects = MutableSharedFlow<VersionsEffect>()
    val effects = _effects.asSharedFlow()

    // Loaders disponibles
    val availableLoaders = listOf("Vanilla", "Fabric", "Forge", "OptiFine", "Quilt")

    fun getLoaderVersions(loader: String, mcVersion: String): List<String> {
        return when (loader) {
            "Fabric" -> listOf("0.15.11", "0.16.0", "0.16.2")
            "Forge" -> listOf("47.2.0", "48.0.1", "49.0.0")
            "OptiFine" -> listOf("HD_U_H7", "HD_U_H8")
            "Quilt" -> listOf("0.23.0", "0.24.0")
            else -> emptyList()
        }
    }

    init {
        loadVersions()
    }

    // Carga de versiones
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

    // Descarga
    fun downloadVersion(versionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                downloading = true,
                downloadStatus = "Iniciando..."
            )
            versionManager.downloadVersion(versionId) { status ->
                _uiState.value = _uiState.value.copy(downloadStatus = status)
            }
            _uiState.value = _uiState.value.copy(downloading = false)
        }
    }

    // Filtros y búsqueda
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
        // Simulación: retorna false siempre (luego se conectará con el sistema de archivos)
        return false
    }

    fun launchGame() {
        viewModelScope.launch {
            val selection = _selectedVersion.value
            if (selection == null) {
                _effects.emit(VersionsEffect.ShowSnackbar("Selecciona una versión primero"))
                return@launch
            }
            _effects.emit(VersionsEffect.ShowSnackbar("Lanzando ${selection.displayName()}..."))
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
    val downloadStatus: String = ""
)

sealed class VersionsEffect {
    data class ShowSnackbar(val message: String) : VersionsEffect()
}
