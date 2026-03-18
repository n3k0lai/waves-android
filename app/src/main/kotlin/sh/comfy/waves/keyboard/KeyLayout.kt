package sh.comfy.waves.keyboard

/**
 * Key layout definitions for QWERTY and symbol panels.
 * Each row is a list of Key objects with type, label, and optional width weight.
 */
object KeyLayout {

    data class Key(
        val label: String,
        val type: KeyType = KeyType.CHARACTER,
        val weight: Float = 1f,
        val icon: String? = null,  // material icon name, null = use label text
    )

    enum class KeyType {
        CHARACTER,      // commits label text
        SHIFT,          // toggles shift state
        BACKSPACE,      // deletes backward
        ENTER,          // sends IME action or newline
        SPACE,          // commits space
        SYMBOLS,        // switches to symbols panel
        ALPHA,          // switches back to QWERTY
        SYMBOLS_2,      // switches to symbols page 2
        EMOTES,         // opens emote panel
        PINYIN,         // switches to 9-key pinyin panel
        COMMA,          // commits comma
        PERIOD,         // commits period
    }

    val qwertyRows: List<List<Key>> = listOf(
        "qwertyuiop".map { Key(it.toString()) },
        "asdfghjkl".map { Key(it.toString()) },
        listOf(
            Key("⇧", KeyType.SHIFT, weight = 1.5f),
            *"zxcvbnm".map { Key(it.toString()) }.toTypedArray(),
            Key("⌫", KeyType.BACKSPACE, weight = 1.5f, icon = "backspace"),
        ),
        listOf(
            Key("123", KeyType.SYMBOLS, weight = 1.2f),
            Key("中", KeyType.PINYIN, weight = 1f),
            Key("😀", KeyType.EMOTES, weight = 1f),
            Key(",", KeyType.COMMA),
            Key(" ", KeyType.SPACE, weight = 3.5f),
            Key(".", KeyType.PERIOD),
            Key("↵", KeyType.ENTER, weight = 1.5f, icon = "enter"),
        ),
    )

    val symbols1Rows: List<List<Key>> = listOf(
        "1234567890".map { Key(it.toString()) },
        listOf(
            Key("@"), Key("#"), Key("$"), Key("_"), Key("&"),
            Key("-"), Key("+"), Key("("), Key(")"), Key("/"),
        ),
        listOf(
            Key("=/<", KeyType.SYMBOLS_2, weight = 1.5f),
            Key("*"), Key("\""), Key("'"), Key(":"), Key(";"),
            Key("!"), Key("?"),
            Key("⌫", KeyType.BACKSPACE, weight = 1.5f, icon = "backspace"),
        ),
        listOf(
            Key("ABC", KeyType.ALPHA, weight = 1.2f),
            Key("😀", KeyType.EMOTES, weight = 1f),
            Key(",", KeyType.COMMA),
            Key(" ", KeyType.SPACE, weight = 4f),
            Key(".", KeyType.PERIOD),
            Key("↵", KeyType.ENTER, weight = 1.8f, icon = "enter"),
        ),
    )

    val symbols2Rows: List<List<Key>> = listOf(
        listOf(
            Key("~"), Key("`"), Key("|"), Key("•"), Key("√"),
            Key("π"), Key("÷"), Key("×"), Key("¶"), Key("∆"),
        ),
        listOf(
            Key("£"), Key("¥"), Key("€"), Key("¢"), Key("^"),
            Key("°"), Key("="), Key("{"), Key("}"), Key("\\"),
        ),
        listOf(
            Key("123", KeyType.SYMBOLS, weight = 1.5f),
            Key("©"), Key("®"), Key("™"), Key("✓"), Key("["),
            Key("]"), Key("<"), Key(">"),
            Key("⌫", KeyType.BACKSPACE, weight = 1.5f, icon = "backspace"),
        ),
        listOf(
            Key("ABC", KeyType.ALPHA, weight = 1.2f),
            Key("😀", KeyType.EMOTES, weight = 1f),
            Key(",", KeyType.COMMA),
            Key(" ", KeyType.SPACE, weight = 4f),
            Key(".", KeyType.PERIOD),
            Key("↵", KeyType.ENTER, weight = 1.8f, icon = "enter"),
        ),
    )
}
