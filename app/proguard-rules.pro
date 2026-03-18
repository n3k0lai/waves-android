# Media3 ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# DataStore Preferences
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }

# Window Manager (foldable)
-keep class androidx.window.** { *; }
-dontwarn androidx.window.**

# Quick Settings Tile
-keep class sh.comfy.waves.tile.WavesAnimationTile { *; }

# Wallpaper Service
-keep class sh.comfy.waves.wallpaper.WavesWallpaperService { *; }

# Widget Provider
-keep class sh.comfy.waves.widget.WavesWidgetProvider { *; }

# Keyboard IME Service
-keep class sh.comfy.waves.keyboard.WavesKeyboardService { *; }

# Launcher
-keep class sh.comfy.waves.launcher.LauncherActivity { *; }
-keep class sh.comfy.waves.launcher.data.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class sh.comfy.waves.launcher.data.**$$serializer { *; }
-keepclassmembers class sh.comfy.waves.launcher.data.** { *** Companion; }
-keepclasseswithmembers class sh.comfy.waves.launcher.data.** { kotlinx.serialization.KSerializer serializer(...); }
