# LucyMC - Aggressive R8/ProGuard for Low-End Devices

# Aggressive optimization
-optimizationpasses 5
-allowaccessmodification
-overloadaggressively
-repackageclasses ''
-flattenpackagehierarchy ''
-mergeinterfacesaggressively
-overloadaggressively

# Remove unused
-dontwarn **
-ignorewarnings

# Keep entry points
-keep class com.miempresa.mclauncher.MainActivity { *; }
-keep class com.miempresa.mclauncher.VersionManager { *; }
-keep class com.miempresa.mclauncher.VersionsViewModel { *; }
-keep class com.miempresa.mclauncher.SettingsViewModel { *; }
-keep class com.miempresa.mclauncher.SettingsManager { *; }

# Keep data classes used in JSON
-keep class com.miempresa.mclauncher.VersionManager$DownloadProgress { *; }

# Strip all logging
-assumenosideeffects class android.util.Log { *; }
-assumenosideeffects class kotlin.io.* { *; }

# Remove unused compose runtime overhead
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }

# Keep navigation
-keep class androidx.navigation.compose.** { *; }

# Aggressive dead code elimination
-keepattributes !SourceFile,!LineNumberTable,!LocalVariableTable,!LocalVariableTypeTable,!AnnotationDefault,!RuntimeVisibleAnnotations,!RuntimeInvisibleAnnotations,!RuntimeVisibleParameterAnnotations,!RuntimeInvisibleParameterAnnotations,!EnclosingMethod

# Remove unused resources
