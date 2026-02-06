package sh.comfy.waves.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import sh.comfy.waves.dataStore

data class WallpaperSettings(
    val focalPointX: Float = 0.5f,
    val focalPointY: Float = 0.5f,
)

class SettingsRepository(private val context: Context) {

    private companion object {
        val FOCAL_X = floatPreferencesKey("focal_point_x")
        val FOCAL_Y = floatPreferencesKey("focal_point_y")
        val WIDGET_TRANSPARENCY = floatPreferencesKey("widget_transparency")
    }

    val wallpaperSettings: Flow<WallpaperSettings> = context.dataStore.data.map { prefs ->
        WallpaperSettings(
            focalPointX = prefs[FOCAL_X] ?: 0.5f,
            focalPointY = prefs[FOCAL_Y] ?: 0.5f,
        )
    }

    /** Widget background transparency: 0f = opaque black, 1f = fully transparent */
    val widgetTransparency: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[WIDGET_TRANSPARENCY] ?: 0.5f
    }

    suspend fun updateFocalPoint(x: Float, y: Float) {
        context.dataStore.edit { prefs ->
            prefs[FOCAL_X] = x.coerceIn(0f, 1f)
            prefs[FOCAL_Y] = y.coerceIn(0f, 1f)
        }
    }

    suspend fun updateWidgetTransparency(transparency: Float) {
        context.dataStore.edit { prefs ->
            prefs[WIDGET_TRANSPARENCY] = transparency.coerceIn(0f, 1f)
        }
    }
}
