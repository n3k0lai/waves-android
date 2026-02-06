package sh.comfy.waves.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import sh.comfy.waves.MainActivity
import sh.comfy.waves.R
import sh.comfy.waves.data.SettingsRepository

class WavesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, WavesWidgetProvider::class.java)
            )
            onUpdate(context, manager, ids)
        }
    }

    companion object {
        const val ACTION_REFRESH = "sh.comfy.waves.WIDGET_REFRESH"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val info = SystemInfoHelper.gather(context)
            val text = SystemInfoHelper.format(info)

            // Read transparency from settings
            val repo = SettingsRepository(context)
            val transparency = runBlocking {
                repo.widgetTransparency.first()
            }
            val alpha = ((1f - transparency) * 255).toInt().coerceIn(0, 255)
            val bgColor = Color.argb(alpha, 0, 0, 0)

            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            views.setTextViewText(R.id.widget_text, text)
            views.setInt(R.id.widget_root, "setBackgroundColor", bgColor)

            // Tap widget → open settings
            val openIntent = Intent(context, MainActivity::class.java)
            val pendingOpen = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingOpen)

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        fun refreshAll(context: Context) {
            val intent = Intent(context, WavesWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            context.sendBroadcast(intent)
        }
    }
}
