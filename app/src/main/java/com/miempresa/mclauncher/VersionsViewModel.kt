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

    private val _uiState = MutableStateFlow(VersionsUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<VersionsEffect>()
    val effects = _effects.asSharedFlow()

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
                downloadStatus = "Iniciando..."
            )
            versionManager.downloadVersion(versionId) { status ->
                _uiState.value = _uiState.value.copy(downloadStatus = status)
            }
            _uiState.value = _uiState.value.copy(downloading = false)
        }
    }

    fun updateFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
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
