package sh.comfy.waves.launcher.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent

/**
 * Manages Android AppWidget hosting for the launcher.
 * Handles widget picking, binding, and lifecycle.
 */
class WavesWidgetHost(
    private val context: Context,
    private val hostId: Int = HOST_ID,
) {
    val host: AppWidgetHost = AppWidgetHost(context, hostId)
    val manager: AppWidgetManager = AppWidgetManager.getInstance(context)

    fun startListening() {
        host.startListening()
    }

    fun stopListening() {
        host.stopListening()
    }

    /**
     * Allocate a new widget ID for binding.
     */
    fun allocateWidgetId(): Int {
        return host.allocateAppWidgetId()
    }

    /**
     * Create the view for a bound widget.
     */
    fun createView(widgetId: Int): android.appwidget.AppWidgetHostView {
        val info = manager.getAppWidgetInfo(widgetId)
        return host.createView(context, widgetId, info)
    }

    /**
     * Delete a widget.
     */
    fun deleteWidget(widgetId: Int) {
        host.deleteAppWidgetId(widgetId)
    }

    /**
     * Get the pick-widget intent for the system widget picker.
     */
    fun getPickWidgetIntent(widgetId: Int): Intent {
        return Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
    }

    /**
     * Get all available widget providers.
     */
    fun getInstalledProviders(): List<AppWidgetProviderInfo> {
        return manager.installedProviders
    }

    companion object {
        const val HOST_ID = 1024
        const val REQUEST_PICK_WIDGET = 1001
        const val REQUEST_BIND_WIDGET = 1002
    }
}
