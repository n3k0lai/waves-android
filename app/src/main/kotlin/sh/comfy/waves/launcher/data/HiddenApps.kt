package sh.comfy.waves.launcher.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages hidden apps list.
 * Nova-style: hidden apps don't show in drawer but can be added to desktop.
 */
class HiddenApps(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hidden_apps", Context.MODE_PRIVATE)

    fun isHidden(packageName: String): Boolean {
        return prefs.getStringSet(KEY_HIDDEN, emptySet())?.contains(packageName) == true
    }

    fun hide(packageName: String) {
        val current = prefs.getStringSet(KEY_HIDDEN, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(packageName)
        prefs.edit().putStringSet(KEY_HIDDEN, current).apply()
    }

    fun unhide(packageName: String) {
        val current = prefs.getStringSet(KEY_HIDDEN, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.remove(packageName)
        prefs.edit().putStringSet(KEY_HIDDEN, current).apply()
    }

    fun getHiddenPackages(): Set<String> {
        return prefs.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()
    }

    fun filterVisible(apps: List<AppInfo>): List<AppInfo> {
        val hidden = getHiddenPackages()
        return apps.filter { it.packageName !in hidden }
    }

    companion object {
        private const val KEY_HIDDEN = "hidden_packages"
    }
}
