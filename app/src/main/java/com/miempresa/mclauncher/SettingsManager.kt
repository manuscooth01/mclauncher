package com.miempresa.mclauncher

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor centralizado de preferencias y configuraciones persistentes del launcher.
 */
class SettingsManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "lucymc_settings"
        private const val KEY_RAM_ALLOCATION = "ram_allocation"
        private const val KEY_GAME_PATH = "game_path"
        private const val KEY_DEVELOPER_MODE = "developer_mode"
        private const val KEY_ACTIVE_USER = "active_user"
        private const val KEY_SESSION_TYPE = "session_type"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"

        private const val DEFAULT_RAM = 3072
        private const val DEFAULT_PATH = "/data/data/com.termux/files/home/.minecraft"
        private const val DEFAULT_USER = "INVITADO_X"
        private const val DEFAULT_SESSION = "NOT_FOUND"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- RAM ---
    fun getRamAllocation(): Int = prefs.getInt(KEY_RAM_ALLOCATION, DEFAULT_RAM)
    fun setRamAllocation(mb: Int) = prefs.edit().putInt(KEY_RAM_ALLOCATION, mb).apply()

    // --- Game Path ---
    fun getGamePath(): String = prefs.getString(KEY_GAME_PATH, DEFAULT_PATH) ?: DEFAULT_PATH
    fun setGamePath(path: String) = prefs.edit().putString(KEY_GAME_PATH, path).apply()

    // --- Developer Mode ---
    fun isDeveloperMode(): Boolean = prefs.getBoolean(KEY_DEVELOPER_MODE, true)
    fun setDeveloperMode(enabled: Boolean) = prefs.edit().putBoolean(KEY_DEVELOPER_MODE, enabled).apply()

    // --- Account ---
    fun getActiveUser(): String = prefs.getString(KEY_ACTIVE_USER, DEFAULT_USER) ?: DEFAULT_USER
    fun setActiveUser(user: String) = prefs.edit().putString(KEY_ACTIVE_USER, user).apply()

    fun getSessionType(): String = prefs.getString(KEY_SESSION_TYPE, DEFAULT_SESSION) ?: DEFAULT_SESSION
    fun setSessionType(type: String) = prefs.edit().putString(KEY_SESSION_TYPE, type).apply()

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun setLoggedIn(logged: Boolean) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, logged).apply()

    fun clearAccount() {
        prefs.edit()
            .putString(KEY_ACTIVE_USER, DEFAULT_USER)
            .putString(KEY_SESSION_TYPE, DEFAULT_SESSION)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }
}
