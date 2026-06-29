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

    // --- Estado de la UI (versiones, filtros, descarga) ---
    private val _uiState = MutableStateFlow(VersionsUiState())
    val uiState = _uiState.asStateFlow()

    // --- Estado de selección (versión + loader) ---
    private val _selectedVersion = MutableStateFlow<VersionSelection?>(null)
    val selectedVersion = _selectedVersion.asStateFlow()

    private val _isBottomSheetOpen = MutableStateFlow(false)
    val isBottomSheetOpen = _isBottomSheetOpen.asStateFlow()

    // --- Efectos (snackbars) ---
    private val _effects = MutableSharedFlow<VersionsEffect>()
    val effects = _effects.asSharedFlow()

    // --- Lista de loaders disponibles (hardcodeada por ahora) ---
    val availableLoaders = listOf("Vanilla", "Fabric", "Forge", "OptiFine", "Quilt")

    // --- Mapa de versiones de loader (simulado, luego se obtendrá de API) ---
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

    // --- Carga de versiones (igual que antes) ---
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

    // --- Descarga (igual que antes) ---
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

    // --- Filtros y búsqueda (igual que antes) ---
    fun updateFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    // --- NUEVAS FUNCIONES PARA SELECCIÓN ---

    /** Abre el bottom sheet para la versión seleccionada */
    fun selectVersion(versionId: String) {
        // Verificar si la versión ya está instalada para no abrir bottom sheet
        if (isVersionInstalled(versionId)) {
            // Si ya está instalada, simplemente la seleccionamos y cerramos
            _selectedVersion.value = VersionSelection(versionId, null, null)
            _isBottomSheetOpen.value = false
            _effects.emit(VersionsEffect.ShowSnackbar("Versión $versionId ya instalada"))
            return
        }
        _selectedVersion.value = VersionSelection(versionId)
        _isBottomSheetOpen.value = true
    }

    /** Actualiza el loader seleccionado */
    fun updateLoader(loader: String?) {
        val current = _selectedVersion.value ?: return
        _selectedVersion.value = current.copy(
            loader = if (loader == "Vanilla") null else loader,
            loaderVersion = null // Resetear versión del loader al cambiar
        )
    }

    /** Actualiza la versión del loader seleccionada */
    fun updateLoaderVersion(loaderVersion: String) {
        val current = _selectedVersion.value ?: return
        _selectedVersion.value = current.copy(loaderVersion = loaderVersion)
    }

    /** Confirma la selección y cierra el bottom sheet */
    fun confirmSelection() {
        val selection = _selectedVersion.value
        if (selection != null) {
            // Aquí podrías guardar la selección en SharedPreferences para persistir
            // También podrías iniciar la descarga si no está instalada
            if (!isVersionInstalled(selection.versionId)) {
                downloadVersion(selection.versionId)
            }
        }
        _isBottomSheetOpen.value = false
    }

    /** Cancela la selección y cierra el bottom sheet */
    fun cancelSelection() {
        _isBottomSheetOpen.value = false
        _selectedVersion.value = null
    }

    /** Verifica si una versión está instalada (simulado) */
    fun isVersionInstalled(versionId: String): Boolean {
        // Aquí iría lógica real: comprobar si existe el JAR en la carpeta
        // Por ahora, simular que ninguna está instalada
        return false
    }

    /** Lanza el juego con la selección actual */
    fun launchGame() {
        val selection = _selectedVersion.value
        if (selection == null) {
            _effects.emit(VersionsEffect.ShowSnackbar("Selecciona una versión primero"))
            return
        }
        // Aquí iría la lógica de lanzamiento (verificar sesión, ejecutar Pojav, etc.)
        _effects.emit(VersionsEffect.ShowSnackbar("Lanzando ${selection.displayName()}..."))
    }
}

// --- Modelo de datos ---
data class VersionSelection(
    val versionId: String,
    val loader: String? = null,      // null = Vanilla
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

// --- Estados de UI (igual que antes) ---
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