package sh.comfy.waves.launcher.home

import android.app.NotificationManager
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks notification badges per package.
 * Requires NotificationListenerService permission.
 *
 * Nova-style: shows unread count dot/number on app icons.
 */
class NotificationBadgeService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        updateBadges()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        updateBadges()
    }

    private fun updateBadges() {
        try {
            val notifications = activeNotifications ?: return
            val counts = mutableMapOf<String, Int>()
            notifications.forEach { sbn ->
                val pkg = sbn.packageName
                counts[pkg] = (counts[pkg] ?: 0) + 1
            }
            BadgeTracker.updateCounts(counts)
        } catch (_: Exception) {
            // Service might not be connected
        }
    }
}

/**
 * Global badge count tracker.
 * Updated by NotificationBadgeService, consumed by launcher UI.
 */
object BadgeTracker {
    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()

    fun updateCounts(counts: Map<String, Int>) {
        _counts.value = counts
    }

    fun getCount(packageName: String): Int {
        return _counts.value[packageName] ?: 0
    }
}
