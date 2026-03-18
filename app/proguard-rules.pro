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
