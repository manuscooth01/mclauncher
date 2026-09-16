# LucyMC ProGuard Rules

# Keep Compose
-keep class androidx.compose.** { *; }
-keep class androidx.compose.material3.** { *; }

# Keep ViewModels
-keep class com.miempresa.mclauncher.VersionsViewModel { *; }
-keep class com.miempresa.mclauncher.SettingsViewModel { *; }

# Keep data classes
-keep class com.miempresa.mclauncher.VersionsUiState { *; }
-keep class com.miempresa.mclauncher.VersionSelection { *; }
-keep class com.miempresa.mclauncher.VersionManager$DownloadProgress { *; }

# Keep Navigation
-keep class com.miempresa.mclauncher.Screen$* { *; }

# Remove logging
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
