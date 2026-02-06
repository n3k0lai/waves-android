package sh.comfy.waves.iconpack

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

object IconPackHelper {

    private const val NIAGARA_PACKAGE = "bitpit.launcher"
    private const val WAVES_PACKAGE = "sh.comfy.waves"

    /**
     * Launches Niagara's icon pack application flow.
     */
    fun applyToNiagara(context: Context) {
        val intent = Intent("bitpit.launcher.APPLY_ICONS").apply {
            setPackage(NIAGARA_PACKAGE)
            putExtra("packageName", WAVES_PACKAGE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Niagara not installed — fall back to generic launcher apply
            applyToLauncher(context)
        }
    }

    /**
     * Opens the system's app details or launcher settings so the user can
     * manually select this icon pack in their launcher of choice.
     */
    fun applyToLauncher(context: Context) {
        val intent = Intent("org.adw.launcher.THEMES").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // No launcher responded — try opening the app's own main activity
            // so the user at least sees the icon pack is installed
        }
    }
}
