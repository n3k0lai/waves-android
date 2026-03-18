package sh.comfy.waves.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Represents an installed app that can be launched.
 */
data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable?,
) : Comparable<AppInfo> {

    override fun compareTo(other: AppInfo): Int {
        return label.compareTo(other.label, ignoreCase = true)
    }

    val launchIntent: Intent
        get() = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(packageName, activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
}

/**
 * Queries all launchable apps from the system.
 */
object AppRepository {

    suspend fun loadApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val pm = context.packageManager
        val userHandle = Process.myUserHandle()

        launcherApps.getActivityList(null, userHandle)
            .map { info ->
                AppInfo(
                    packageName = info.applicationInfo.packageName,
                    activityName = info.componentName.className,
                    label = info.label?.toString() ?: info.applicationInfo.packageName,
                    icon = info.getBadgedIcon(0),
                )
            }
            .sorted()
    }

    /**
     * Get a specific app's icon with icon pack override support.
     */
    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * Search apps by name (case-insensitive substring).
     */
    fun searchApps(apps: List<AppInfo>, query: String): List<AppInfo> {
        if (query.isBlank()) return apps
        val q = query.lowercase()
        return apps.filter { it.label.lowercase().contains(q) }
    }
}
