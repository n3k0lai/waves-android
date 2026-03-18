package sh.comfy.waves.launcher.data

import android.content.Context
import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Backup and restore launcher settings + layout.
 * Nova-style: export/import to JSON file.
 */
@Serializable
data class LauncherBackup(
    val version: Int = 1,
    val layout: DesktopLayout,
    val settings: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
)

private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

object BackupRestore {

    /**
     * Export current launcher state to JSON string.
     */
    fun export(context: Context): String {
        val layout = DesktopLayoutStore.load(context)
        val backup = LauncherBackup(layout = layout)
        return json.encodeToString(backup)
    }

    /**
     * Export to a file in the app's cache directory.
     * Returns the file path.
     */
    fun exportToFile(context: Context): File {
        val content = export(context)
        val file = File(context.cacheDir, "waves_backup_${System.currentTimeMillis()}.json")
        file.writeText(content)
        return file
    }

    /**
     * Import launcher state from JSON string.
     */
    fun import(context: Context, jsonString: String): Boolean {
        return try {
            val backup = json.decodeFromString<LauncherBackup>(jsonString)
            DesktopLayoutStore.save(context, backup.layout)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Import from a content URI (e.g., from file picker).
     */
    fun importFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val content = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { it.readText() } ?: return false
            import(context, content)
        } catch (_: Exception) {
            false
        }
    }
}
