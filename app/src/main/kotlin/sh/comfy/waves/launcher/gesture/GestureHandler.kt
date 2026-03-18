package sh.comfy.waves.launcher.gesture

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Maps gesture names to launcher actions.
 * Nova-style gesture customization.
 */
object GestureHandler {

    sealed class GestureAction {
        data object OpenDrawer : GestureAction()
        data object OpenSearch : GestureAction()
        data object OpenNotifications : GestureAction()
        data object OpenQuickSettings : GestureAction()
        data object OpenOverview : GestureAction()
        data object LockScreen : GestureAction()
        data object OpenSettings : GestureAction()
        data object OpenWidgets : GestureAction()
        data object None : GestureAction()
        data class LaunchApp(val packageName: String) : GestureAction()
    }

    fun resolveAction(actionName: String): GestureAction {
        return when (actionName) {
            "drawer" -> GestureAction.OpenDrawer
            "search" -> GestureAction.OpenSearch
            "notifications" -> GestureAction.OpenNotifications
            "quick_settings" -> GestureAction.OpenQuickSettings
            "overview" -> GestureAction.OpenOverview
            "lock" -> GestureAction.LockScreen
            "settings" -> GestureAction.OpenSettings
            "widgets" -> GestureAction.OpenWidgets
            "none" -> GestureAction.None
            else -> {
                if (actionName.startsWith("app:")) {
                    GestureAction.LaunchApp(actionName.removePrefix("app:"))
                } else {
                    GestureAction.None
                }
            }
        }
    }

    /**
     * Execute a gesture action. Some actions (lock screen, notifications)
     * require special permissions.
     */
    fun execute(context: Context, action: GestureAction, callbacks: GestureCallbacks) {
        when (action) {
            is GestureAction.OpenDrawer -> callbacks.onOpenDrawer()
            is GestureAction.OpenSearch -> callbacks.onOpenSearch()
            is GestureAction.OpenNotifications -> expandNotifications(context)
            is GestureAction.OpenQuickSettings -> expandQuickSettings(context)
            is GestureAction.OpenOverview -> callbacks.onOpenOverview()
            is GestureAction.LockScreen -> lockScreen(context)
            is GestureAction.OpenSettings -> {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
            is GestureAction.OpenWidgets -> callbacks.onOpenWidgets()
            is GestureAction.None -> { /* no-op */ }
            is GestureAction.LaunchApp -> {
                val intent = context.packageManager.getLaunchIntentForPackage(action.packageName)
                if (intent != null) {
                    context.startActivity(intent)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun expandNotifications(context: Context) {
        try {
            val sbService = context.getSystemService("statusbar")
            val sbClass = Class.forName("android.app.StatusBarManager")
            sbClass.getMethod("expandNotificationsPanel").invoke(sbService)
        } catch (_: Exception) {
            // Fallback: open settings
        }
    }

    @Suppress("DEPRECATION")
    private fun expandQuickSettings(context: Context) {
        try {
            val sbService = context.getSystemService("statusbar")
            val sbClass = Class.forName("android.app.StatusBarManager")
            sbClass.getMethod("expandSettingsPanel").invoke(sbService)
        } catch (_: Exception) {
            // Fallback: open settings
        }
    }

    private fun lockScreen(context: Context) {
        // Try accessibility service first (preferred, no device admin needed)
        if (SleepService.lockScreen()) return

        // Fallback to device admin
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                dpm.lockNow()
            } catch (_: SecurityException) {
                // Needs device admin permission
            }
        }
    }

    interface GestureCallbacks {
        fun onOpenDrawer()
        fun onOpenSearch()
        fun onOpenOverview()
        fun onOpenWidgets()
    }
}
