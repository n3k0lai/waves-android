package sh.comfy.waves.keyboard.core

import android.content.Context
import android.content.SharedPreferences

/**
 * Persisted keyboard preferences.
 * Uses SharedPreferences for instant reads (no suspend/Flow needed in IME hot path).
 */
class KeyboardPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var hapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, false)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var doubleTapPeriod: Boolean
        get() = prefs.getBoolean(KEY_DOUBLE_SPACE, true)
        set(value) = prefs.edit().putBoolean(KEY_DOUBLE_SPACE, value).apply()

    var autoCapitalize: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CAP, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CAP, value).apply()

    var wavesIntensity: Float
        get() = prefs.getFloat(KEY_WAVES_INTENSITY, 1f)
        set(value) = prefs.edit().putFloat(KEY_WAVES_INTENSITY, value.coerceIn(0f, 1f)).apply()

    var keyHeight: Int
        get() = prefs.getInt(KEY_HEIGHT, 46)
        set(value) = prefs.edit().putInt(KEY_HEIGHT, value.coerceIn(36, 60)).apply()

    companion object {
        private const val PREFS_NAME = "waves_keyboard_prefs"
        private const val KEY_HAPTIC = "haptic_enabled"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_DOUBLE_SPACE = "double_tap_period"
        private const val KEY_AUTO_CAP = "auto_capitalize"
        private const val KEY_WAVES_INTENSITY = "waves_intensity"
        private const val KEY_HEIGHT = "key_height_dp"
    }
}
