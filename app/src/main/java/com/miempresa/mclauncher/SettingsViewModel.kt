package com.miempresa.mclauncher

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _ramAllocation = MutableStateFlow(settingsManager.getRamAllocation().toFloat())
    val ramAllocation = _ramAllocation.asStateFlow()

    private val _isDevMode = MutableStateFlow(settingsManager.isDeveloperMode())
    val isDevMode = _isDevMode.asStateFlow()

    private val _gamePath = MutableStateFlow(settingsManager.getGamePath())
    val gamePath = _gamePath.asStateFlow()

    fun setRamAllocation(value: Float) {
        _ramAllocation.value = value
        settingsManager.setRamAllocation(value.toInt())
    }

    fun toggleDevMode() {
        val newValue = !_isDevMode.value
        _isDevMode.value = newValue
        settingsManager.setDeveloperMode(newValue)
    }
}
