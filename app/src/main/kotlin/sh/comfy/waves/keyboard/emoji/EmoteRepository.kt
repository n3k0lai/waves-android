package sh.comfy.waves.keyboard.emoji

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.URL

/**
 * Fetches emotes from emotes.comfy.sh and caches them locally.
 * Provides a searchable, paginated emote list for the keyboard panel.
 */
class EmoteRepository(private val context: Context) {

    data class Emote(
        val name: String,
        val url: String,      // CDN URL (webp/avif from 7TV)
        val id: String,
    )

    private val _emotes = MutableStateFlow<List<Emote>>(emptyList())
    val emotes: StateFlow<List<Emote>> = _emotes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Recently used emotes (persisted to shared prefs)
    private val _recentEmotes = MutableStateFlow<List<String>>(emptyList())
    val recentEmotes: StateFlow<List<String>> = _recentEmotes.asStateFlow()

    private val cacheDir by lazy {
        File(context.cacheDir, "emotes").also { it.mkdirs() }
    }

    private val prefs by lazy {
        context.getSharedPreferences("emote_recents", Context.MODE_PRIVATE)
    }

    init {
        loadRecents()
    }

    /**
     * Fetch emote list from emotes.comfy.sh API.
     * Results are cached in memory; images cached to disk.
     */
    suspend fun refresh() {
        if (_isLoading.value) return
        _isLoading.value = true

        try {
            val emoteList = withContext(Dispatchers.IO) {
                val json = URL(API_URL).readText()
                parseEmotes(json)
            }
            _emotes.value = emoteList
        } catch (e: Exception) {
            // On failure, try loading from disk cache
            loadFromCache()
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Search emotes by name (case-insensitive prefix/substring match).
     */
    fun search(query: String): List<Emote> {
        if (query.isBlank()) return _emotes.value
        val q = query.lowercase()
        return _emotes.value.filter { it.name.lowercase().contains(q) }
    }

    /**
     * Record an emote as recently used.
     */
    fun recordUsage(emoteName: String) {
        val current = _recentEmotes.value.toMutableList()
        current.remove(emoteName)
        current.add(0, emoteName)
        val trimmed = current.take(MAX_RECENTS)
        _recentEmotes.value = trimmed
        saveRecents(trimmed)
    }

    /**
     * Get the recently used emotes as Emote objects.
     */
    fun getRecentEmoteObjects(): List<Emote> {
        val all = _emotes.value.associateBy { it.name }
        return _recentEmotes.value.mapNotNull { all[it] }
    }

    private fun parseEmotes(json: String): List<Emote> {
        val array = JSONArray(json)
        val result = mutableListOf<Emote>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                Emote(
                    name = obj.getString("name"),
                    url = obj.getString("url"),
                    id = obj.optString("id", ""),
                )
            )
        }
        return result
    }

    private fun loadFromCache() {
        // Future: load cached JSON from disk
    }

    private fun loadRecents() {
        val saved = prefs.getString(KEY_RECENTS, null) ?: return
        _recentEmotes.value = saved.split(",").filter { it.isNotBlank() }.take(MAX_RECENTS)
    }

    private fun saveRecents(recents: List<String>) {
        prefs.edit().putString(KEY_RECENTS, recents.joinToString(",")).apply()
    }

    companion object {
        private const val API_URL = "https://emotes.comfy.sh/api/emotes.json"
        private const val KEY_RECENTS = "recent_emotes"
        private const val MAX_RECENTS = 30
    }
}
