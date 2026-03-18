package sh.comfy.waves.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.launcherDataStore by preferencesDataStore(name = "launcher_settings")

/**
 * Persisted launcher configuration — Nova-style customization.
 */
class LauncherSettings(private val context: Context) {

    // Desktop grid
    val desktopColumns: Flow<Int> = context.launcherDataStore.data.map { it[DESKTOP_COLS] ?: 5 }
    val desktopRows: Flow<Int> = context.launcherDataStore.data.map { it[DESKTOP_ROWS] ?: 5 }
    val desktopPageCount: Flow<Int> = context.launcherDataStore.data.map { it[DESKTOP_PAGES] ?: 5 }

    // Dock
    val dockEnabled: Flow<Boolean> = context.launcherDataStore.data.map { it[DOCK_ENABLED] ?: true }
    val dockColumns: Flow<Int> = context.launcherDataStore.data.map { it[DOCK_COLS] ?: 5 }
    val dockPages: Flow<Int> = context.launcherDataStore.data.map { it[DOCK_PAGES] ?: 1 }
    val dockBackground: Flow<Float> = context.launcherDataStore.data.map { it[DOCK_BG_ALPHA] ?: 0.5f }

    // App drawer
    val drawerColumns: Flow<Int> = context.launcherDataStore.data.map { it[DRAWER_COLS] ?: 4 }
    val drawerStyle: Flow<String> = context.launcherDataStore.data.map { it[DRAWER_STYLE] ?: "vertical" }

    // Appearance
    val iconSize: Flow<Float> = context.launcherDataStore.data.map { it[ICON_SIZE] ?: 1.0f }
    val labelSize: Flow<Float> = context.launcherDataStore.data.map { it[LABEL_SIZE] ?: 12f }
    val showLabels: Flow<Boolean> = context.launcherDataStore.data.map { it[SHOW_LABELS] ?: true }
    val showDockLabels: Flow<Boolean> = context.launcherDataStore.data.map { it[SHOW_DOCK_LABELS] ?: false }

    // Search
    val searchBarEnabled: Flow<Boolean> = context.launcherDataStore.data.map { it[SEARCH_BAR] ?: true }
    val searchBarStyle: Flow<String> = context.launcherDataStore.data.map { it[SEARCH_STYLE] ?: "pill" }

    // Gestures
    val swipeUpAction: Flow<String> = context.launcherDataStore.data.map { it[GESTURE_SWIPE_UP] ?: "drawer" }
    val doubleTapAction: Flow<String> = context.launcherDataStore.data.map { it[GESTURE_DOUBLE_TAP] ?: "lock" }
    val pinchAction: Flow<String> = context.launcherDataStore.data.map { it[GESTURE_PINCH] ?: "overview" }
    val swipeDownAction: Flow<String> = context.launcherDataStore.data.map { it[GESTURE_SWIPE_DOWN] ?: "notifications" }

    // Scroll effect
    val scrollEffect: Flow<String> = context.launcherDataStore.data.map { it[SCROLL_EFFECT] ?: "slide" }

    // Foldable
    val foldAwareDock: Flow<Boolean> = context.launcherDataStore.data.map { it[FOLD_AWARE_DOCK] ?: true }
    val foldContinuity: Flow<Boolean> = context.launcherDataStore.data.map { it[FOLD_CONTINUITY] ?: true }

    // Night mode
    val nightMode: Flow<String> = context.launcherDataStore.data.map { it[NIGHT_MODE] ?: "system" }

    suspend fun setDesktopGrid(cols: Int, rows: Int) {
        context.launcherDataStore.edit {
            it[DESKTOP_COLS] = cols.coerceIn(3, 9)
            it[DESKTOP_ROWS] = rows.coerceIn(3, 9)
        }
    }

    suspend fun setDockColumns(cols: Int) {
        context.launcherDataStore.edit { it[DOCK_COLS] = cols.coerceIn(3, 7) }
    }

    suspend fun setDrawerColumns(cols: Int) {
        context.launcherDataStore.edit { it[DRAWER_COLS] = cols.coerceIn(3, 6) }
    }

    suspend fun setIconSize(scale: Float) {
        context.launcherDataStore.edit { it[ICON_SIZE] = scale.coerceIn(0.5f, 1.5f) }
    }

    suspend fun setLabelSize(sp: Float) {
        context.launcherDataStore.edit { it[LABEL_SIZE] = sp.coerceIn(8f, 18f) }
    }

    suspend fun setShowLabels(show: Boolean) {
        context.launcherDataStore.edit { it[SHOW_LABELS] = show }
    }

    suspend fun setGesture(gesture: String, action: String) {
        context.launcherDataStore.edit {
            when (gesture) {
                "swipe_up" -> it[GESTURE_SWIPE_UP] = action
                "double_tap" -> it[GESTURE_DOUBLE_TAP] = action
                "pinch" -> it[GESTURE_PINCH] = action
                "swipe_down" -> it[GESTURE_SWIPE_DOWN] = action
            }
        }
    }

    suspend fun setScrollEffect(effect: String) {
        context.launcherDataStore.edit { it[SCROLL_EFFECT] = effect }
    }

    suspend fun setSearchBarEnabled(enabled: Boolean) {
        context.launcherDataStore.edit { it[SEARCH_BAR] = enabled }
    }

    companion object {
        private val DESKTOP_COLS = intPreferencesKey("desktop_cols")
        private val DESKTOP_ROWS = intPreferencesKey("desktop_rows")
        private val DESKTOP_PAGES = intPreferencesKey("desktop_pages")
        private val DOCK_ENABLED = booleanPreferencesKey("dock_enabled")
        private val DOCK_COLS = intPreferencesKey("dock_cols")
        private val DOCK_PAGES = intPreferencesKey("dock_pages")
        private val DOCK_BG_ALPHA = floatPreferencesKey("dock_bg_alpha")
        private val DRAWER_COLS = intPreferencesKey("drawer_cols")
        private val DRAWER_STYLE = stringPreferencesKey("drawer_style")
        private val ICON_SIZE = floatPreferencesKey("icon_size")
        private val LABEL_SIZE = floatPreferencesKey("label_size")
        private val SHOW_LABELS = booleanPreferencesKey("show_labels")
        private val SHOW_DOCK_LABELS = booleanPreferencesKey("show_dock_labels")
        private val SEARCH_BAR = booleanPreferencesKey("search_bar")
        private val SEARCH_STYLE = stringPreferencesKey("search_style")
        private val GESTURE_SWIPE_UP = stringPreferencesKey("gesture_swipe_up")
        private val GESTURE_DOUBLE_TAP = stringPreferencesKey("gesture_double_tap")
        private val GESTURE_PINCH = stringPreferencesKey("gesture_pinch")
        private val GESTURE_SWIPE_DOWN = stringPreferencesKey("gesture_swipe_down")
        private val SCROLL_EFFECT = stringPreferencesKey("scroll_effect")
        private val FOLD_AWARE_DOCK = booleanPreferencesKey("fold_aware_dock")
        private val FOLD_CONTINUITY = booleanPreferencesKey("fold_continuity")
        private val NIGHT_MODE = stringPreferencesKey("night_mode")
    }
}
