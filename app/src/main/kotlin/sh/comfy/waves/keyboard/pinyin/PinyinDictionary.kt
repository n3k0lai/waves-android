package sh.comfy.waves.keyboard.pinyin

import android.content.Context
import org.json.JSONObject

/**
 * Pinyin → Hanzi dictionary.
 *
 * Loaded from assets/pinyin_dict.json on first use.
 * Format: { "syllable": "char1char2char3...", ... }
 * Characters are ordered by frequency (most common first).
 *
 * Also maintains a pre-computed T9 digit → syllable index
 * for fast lookup during typing.
 */
class PinyinDictionary(context: Context) {

    /** syllable → ordered character string (most frequent first) */
    private val syllableToChars: Map<String, String>

    /** T9 digit sequence → list of matching syllables */
    private val digitIndex: Map<String, List<String>>

    /** All valid pinyin syllables (for filtering T9 expansions) */
    val allSyllables: Set<String>

    init {
        val json = context.assets.open("pinyin_dict.json")
            .bufferedReader().use { it.readText() }
        val obj = JSONObject(json)

        val map = mutableMapOf<String, String>()
        val dIndex = mutableMapOf<String, MutableList<String>>()

        for (key in obj.keys()) {
            val syllable = key.lowercase()
            val chars = obj.getString(key)
            map[syllable] = chars

            // Index by T9 digits
            val digits = NinekeyMap.pinyinToDigits(syllable)
            dIndex.getOrPut(digits) { mutableListOf() }.add(syllable)

            // Also index all prefixes (for incremental matching)
            for (i in 1 until digits.length) {
                val prefix = digits.substring(0, i)
                dIndex.getOrPut(prefix) { mutableListOf() }
                    .let { list ->
                        if (syllable !in list) list.add(syllable)
                    }
            }
        }

        syllableToChars = map
        digitIndex = dIndex
        allSyllables = map.keys
    }

    /**
     * Look up candidate characters for a digit sequence.
     * Returns syllable-character pairs sorted by syllable relevance.
     */
    fun lookup(digits: String): List<Candidate> {
        if (digits.isEmpty()) return emptyList()

        val matchingSyllables = digitIndex[digits] ?: return emptyList()

        // Prefer exact-length matches over prefix matches
        val exact = matchingSyllables.filter { NinekeyMap.pinyinToDigits(it).length == digits.length }
        val partial = matchingSyllables.filter { NinekeyMap.pinyinToDigits(it).length > digits.length }

        val candidates = mutableListOf<Candidate>()

        // Exact matches first — these are what the user most likely meant
        for (syllable in exact) {
            val chars = syllableToChars[syllable] ?: continue
            chars.forEach { char ->
                candidates.add(Candidate(char.toString(), syllable, isExact = true))
            }
        }

        // Then partial matches (user is still typing)
        for (syllable in partial.take(10)) { // limit partials
            val chars = syllableToChars[syllable] ?: continue
            chars.take(3).forEach { char -> // fewer chars per partial
                candidates.add(Candidate(char.toString(), syllable, isExact = false))
            }
        }

        return candidates
    }

    /**
     * Get characters for a specific pinyin syllable.
     */
    fun charsForSyllable(syllable: String): String {
        return syllableToChars[syllable.lowercase()] ?: ""
    }

    data class Candidate(
        val char: String,       // The Chinese character
        val pinyin: String,     // The pinyin syllable it came from
        val isExact: Boolean,   // Whether this is an exact T9 match
    )
}
