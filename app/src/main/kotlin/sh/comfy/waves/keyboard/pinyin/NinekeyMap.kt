package sh.comfy.waves.keyboard.pinyin

/**
 * 九宫格 (jiǔ gōng gé) — 9-key T9-style pinyin input mapping.
 *
 * Keys 2-9 map to letter groups. User taps number sequences,
 * engine resolves all possible pinyin syllables matching those groups,
 * then looks up candidate characters.
 *
 * Key layout:
 *   1(.,?)  2(ABC)  3(DEF)
 *   4(GHI)  5(JKL)  6(MNO)
 *   7(PQRS) 8(TUV)  9(WXYZ)
 *   *(lang) 0(sp)   #(tone)
 */
object NinekeyMap {

    /** Maps digit to possible pinyin letters */
    val digitToLetters: Map<Char, List<Char>> = mapOf(
        '2' to listOf('a', 'b', 'c'),
        '3' to listOf('d', 'e', 'f'),
        '4' to listOf('g', 'h', 'i'),
        '5' to listOf('j', 'k', 'l'),
        '6' to listOf('m', 'n', 'o'),
        '7' to listOf('p', 'q', 'r', 's'),
        '8' to listOf('t', 'u', 'v'),
        '9' to listOf('w', 'x', 'y', 'z'),
    )

    /** Reverse map: letter to digit */
    val letterToDigit: Map<Char, Char> = buildMap {
        digitToLetters.forEach { (digit, letters) ->
            letters.forEach { letter -> put(letter, digit) }
        }
    }

    /**
     * Convert a pinyin syllable to its T9 digit sequence.
     * e.g., "ni" → "64", "hao" → "426"
     */
    fun pinyinToDigits(pinyin: String): String {
        return pinyin.lowercase().map { letterToDigit[it] ?: it }.joinToString("")
    }

    /**
     * Given a digit sequence, generate all possible letter combinations.
     * For short sequences this is tractable; for longer ones we filter
     * against the pinyin syllable table.
     */
    fun digitsToPossibleStrings(digits: String): List<String> {
        if (digits.isEmpty()) return listOf("")
        val letterLists = digits.map { digitToLetters[it] ?: listOf(it) }
        return cartesianProduct(letterLists).map { it.joinToString("") }
    }

    private fun cartesianProduct(lists: List<List<Char>>): List<List<Char>> {
        if (lists.isEmpty()) return listOf(emptyList())
        val first = lists.first()
        val rest = cartesianProduct(lists.drop(1))
        return first.flatMap { item -> rest.map { listOf(item) + it } }
    }
}
