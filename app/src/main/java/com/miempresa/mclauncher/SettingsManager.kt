package com.miempresa.mclauncher

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lucymc_settings", Context.MODE_PRIVATE)
    private const val DEFAULT_RAM = 2048
    private const val DEFAULT_USER = "Player"

    var ramMb: Int
        get() = prefs.getInt("ram", DEFAULT_RAM)
        set(value) = prefs.edit().putInt("ram", value.coerceIn(512, 4096)).apply()

    var username: String
        get() = prefs.getString("user", DEFAULT_USER) ?: DEFAULT_USER
        set(value) = prefs.edit().putString("user", value).apply()
}