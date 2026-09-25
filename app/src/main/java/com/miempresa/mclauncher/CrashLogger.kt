package com.miempresa.mclauncher

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {
    private const val TAG = "LUCYMC_CRASH"
    private const val MAX_FILES = 5

    fun install(context: Context) {
        val appContext = context.applicationContext
        val default = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e(TAG, "CRASH on ${thread.name}", throwable)
                writeCrash(appContext, thread, throwable)
            } catch (_: Throwable) { /* nunca fallar aquí */ }
            default?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrash(context: Context, thread: Thread, throwable: Throwable) {
        val ts = timestamp()
        val content = buildReport(context, thread, throwable, ts)

        // Destino 1 (siempre): carpeta de la app
        try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            dir.mkdirs()
            val f = File(dir, "crash_$ts.txt")
            f.writeText(content)
            rotate(dir)
        } catch (_: Throwable) {}

        // Destino 2 (API 29+): MediaStore → /sdcard/Download/lucymc_crash_<ts>.txt (sin permisos)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "lucymc_crash_$ts.txt")
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> os.write(content.toByteArray()) } }
            } catch (_: Throwable) {}
        }
    }

    private fun buildReport(context: Context, thread: Thread, throwable: Throwable, ts: String): String {
        val sb = StringBuilder()
        sb.appendLine("=== LUCYMC CRASH ===")
        sb.appendLine("Time: $ts")
        sb.appendLine("Thread: ${thread.name}")
        sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        sb.appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        sb.appendLine("App version: ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}")
        sb.appendLine()
        sb.appendLine(throwable.stackTraceToString())
        sb.appendLine()
        return sb.toString()
    }

    private fun rotate(dir: File) {
        dir.listFiles { f -> f.name.startsWith("crash_") && f.name.endsWith(".txt") }
            ?.sortedByDescending { it.name }
            ?.drop(MAX_FILES)
            ?.forEach { it.delete() }
    }

    private fun timestamp() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
}
