package sh.comfy.waves.launcher.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persists desktop item positions (app shortcuts, widgets, folders).
 * Saved as JSON to internal storage for fast load.
 */
@Serializable
data class DesktopLayout(
    val pages: List<DesktopPage> = listOf(DesktopPage()),
    val dock: List<DockItem> = emptyList(),
)

@Serializable
data class DesktopPage(
    val items: List<DesktopItem> = emptyList(),
)

@Serializable
sealed class DesktopItem {
    abstract val col: Int
    abstract val row: Int

    @Serializable
    data class AppShortcut(
        override val col: Int,
        override val row: Int,
        val packageName: String,
        val activityName: String,
        val label: String = "",
        val customIconRes: String? = null,
    ) : DesktopItem()

    @Serializable
    data class WidgetItem(
        override val col: Int,
        override val row: Int,
        val widgetId: Int,
        val spanCols: Int = 1,
        val spanRows: Int = 1,
    ) : DesktopItem()

    @Serializable
    data class FolderItem(
        override val col: Int,
        override val row: Int,
        val name: String,
        val apps: List<FolderApp> = emptyList(),
    ) : DesktopItem()
}

@Serializable
data class DockItem(
    val position: Int,
    val page: Int = 0,
    val packageName: String,
    val activityName: String,
    val label: String = "",
)

@Serializable
data class FolderApp(
    val packageName: String,
    val activityName: String,
    val label: String = "",
)

private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

object DesktopLayoutStore {

    private const val FILENAME = "desktop_layout.json"

    fun load(context: Context): DesktopLayout {
        val file = File(context.filesDir, FILENAME)
        if (!file.exists()) return DesktopLayout()
        return try {
            json.decodeFromString<DesktopLayout>(file.readText())
        } catch (_: Exception) {
            DesktopLayout()
        }
    }

    fun save(context: Context, layout: DesktopLayout) {
        val file = File(context.filesDir, FILENAME)
        file.writeText(json.encodeToString(layout))
    }
}
