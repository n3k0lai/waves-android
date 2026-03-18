package sh.comfy.waves.keyboard.view

import android.os.Build
import android.view.HapticFeedbackConstants
import android.widget.FrameLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import sh.comfy.waves.keyboard.KeyLayout
import sh.comfy.waves.keyboard.KeyboardController
import sh.comfy.waves.keyboard.WavesKeyboardService
import sh.comfy.waves.keyboard.emoji.EmoteRepository
import sh.comfy.waves.ui.theme.WavesTheme

/**
 * Root view for the keyboard. Wraps a ComposeView inside a FrameLayout
 * because InputMethodService.onCreateInputView expects a traditional View.
 */
class KeyboardView(
    private val service: WavesKeyboardService,
    private val controller: KeyboardController,
) : FrameLayout(service) {

    private val emoteRepo = EmoteRepository(service)

    init {
        val composeView = ComposeView(service).apply {
            setContent {
                WavesTheme {
                    KeyboardRoot(service, controller, emoteRepo)
                }
            }
        }

        composeView.setViewTreeLifecycleOwner(
            androidx.lifecycle.ProcessLifecycleOwner.get()
        )
        composeView.setViewTreeSavedStateRegistryOwner(null)

        addView(composeView)
    }
}

@Composable
fun KeyboardRoot(
    service: WavesKeyboardService,
    controller: KeyboardController,
    emoteRepo: EmoteRepository,
) {
    val state by controller.state.collectAsState()
    // Track ripples from key taps for reactive wave effect
    var ripples by remember { mutableStateOf(listOf<WaveRipple>()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Animated waves behind keys — only on typing panels, not emote grid
        if (state.panel != KeyboardController.Panel.EMOTES) {
            // Clean up dead ripples
            LaunchedEffect(ripples) {
                if (ripples.any { !it.isAlive }) {
                    delay(50)
                    ripples = ripples.filter { it.isAlive }
                }
            }

            WavesBackground(
                enabled = true,
                intensity = 1f,
                ripples = ripples,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp, vertical = 6.dp),
        ) {
            when (state.panel) {
                KeyboardController.Panel.QWERTY -> {
                    KeyRows(
                        rows = KeyLayout.qwertyRows,
                        service = service,
                        controller = controller,
                        isUpperCase = controller.isUpperCase(),
                        onRipple = { normalizedX ->
                            ripples = ripples + WaveRipple(x = normalizedX)
                        },
                    )
                }
                KeyboardController.Panel.SYMBOLS_1 -> {
                    KeyRows(
                        rows = KeyLayout.symbols1Rows,
                        service = service,
                        controller = controller,
                        isUpperCase = false,
                        onRipple = { normalizedX ->
                            ripples = ripples + WaveRipple(x = normalizedX)
                        },
                    )
                }
                KeyboardController.Panel.SYMBOLS_2 -> {
                    KeyRows(
                        rows = KeyLayout.symbols2Rows,
                        service = service,
                        controller = controller,
                        isUpperCase = false,
                        onRipple = { normalizedX ->
                            ripples = ripples + WaveRipple(x = normalizedX)
                        },
                    )
                }
                KeyboardController.Panel.EMOTES -> {
                    EmotePanel(
                        emoteRepo = emoteRepo,
                        onEmoteSelected = { emote ->
                            service.commitText(emote.name)
                            emoteRepo.recordUsage(emote.name)
                        },
                        onBack = { controller.switchPanel(KeyboardController.Panel.QWERTY) },
                    )
                }
            }
        }
    }
}

@Composable
fun KeyRows(
    rows: List<List<KeyLayout.Key>>,
    service: WavesKeyboardService,
    controller: KeyboardController,
    isUpperCase: Boolean,
    onRipple: ((Float) -> Unit)? = null,
) {
    rows.forEach { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            val totalWeight = row.sumOf { it.weight.toDouble() }.toFloat()
            row.forEach { key ->
                KeyButton(
                    key = key,
                    isUpperCase = isUpperCase,
                    service = service,
                    modifier = Modifier.weight(key.weight / totalWeight),
                    onClick = { handleKeyPress(key, service, controller, isUpperCase) },
                    onLongPress = {
                        handleLongPress(key, service, isUpperCase)
                    },
                    onRipple = onRipple,
                )
            }
        }
    }
}

@Composable
fun KeyButton(
    key: KeyLayout.Key,
    isUpperCase: Boolean,
    modifier: Modifier = Modifier,
    service: WavesKeyboardService? = null,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    onRipple: ((Float) -> Unit)? = null,
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }
    var keyGlobalX by remember { mutableStateOf(0f) }
    var keyWidth by remember { mutableStateOf(0f) }
    val rootWidth = view.width.toFloat().coerceAtLeast(1f)

    // Key press scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = if (isPressed) 30 else 80),
        label = "keyScale",
    )

    // Reset pressed state after brief delay
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }

    // Keys are semi-transparent so animated waves show through
    val bgColor = when (key.type) {
        KeyLayout.KeyType.CHARACTER,
        KeyLayout.KeyType.COMMA,
        KeyLayout.KeyType.PERIOD -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
        KeyLayout.KeyType.SPACE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        KeyLayout.KeyType.ENTER -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    }

    val textColor = when (key.type) {
        KeyLayout.KeyType.ENTER -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val displayLabel = when {
        key.type == KeyLayout.KeyType.CHARACTER && isUpperCase -> key.label.uppercase()
        else -> key.label
    }

    val fontSize = when (key.type) {
        KeyLayout.KeyType.CHARACTER -> 20.sp
        KeyLayout.KeyType.SPACE -> 14.sp
        else -> 14.sp
    }

    // Show long-press hint for keys that have alternates
    val hasAlternates = key.type == KeyLayout.KeyType.CHARACTER &&
        LONG_PRESS_MAP.containsKey(key.label.lowercase())

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .height(46.dp)
                .fillMaxWidth()
                .scale(scale)
                .clip(RoundedCornerShape(6.dp))
                .background(bgColor)
                .onGloballyPositioned { coords ->
                    keyGlobalX = coords.positionInRoot().x
                    keyWidth = coords.size.width.toFloat()
                }
                .pointerInput(key) {
                    detectTapGestures(
                        onTap = { offset ->
                            isPressed = true
                            // Haptic feedback
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            } else {
                                @Suppress("DEPRECATION")
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                            // Trigger wave ripple at tap's global X position
                            val globalTapX = keyGlobalX + offset.x
                            onRipple?.invoke((globalTapX / rootWidth).coerceIn(0f, 1f))
                            onClick()
                        },
                        onLongPress = {
                            if (hasAlternates || onLongPress != null) {
                                // Heavier haptic for long press
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                if (hasAlternates) {
                                    showPopup = true
                                }
                                onLongPress?.invoke()
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = displayLabel,
                color = textColor,
                fontSize = fontSize,
                fontWeight = if (key.type == KeyLayout.KeyType.CHARACTER) FontWeight.Normal else FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }

        // Long-press popup with alternate characters
        if (showPopup && hasAlternates) {
            val alternates = LONG_PRESS_MAP[key.label.lowercase()] ?: emptyList()
            AlternateCharsPopup(
                alternates = if (isUpperCase) alternates.map { it.uppercase() } else alternates,
                onSelect = { char ->
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    service?.commitText(char)
                    showPopup = false
                },
                onDismiss = { showPopup = false },
            )
        }
    }
}

@Composable
fun AlternateCharsPopup(
    alternates: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(
        alignment = Alignment.TopCenter,
        offset = IntOffset(0, -56),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.inverseSurface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            alternates.forEach { char ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .pointerInput(char) {
                            detectTapGestures(
                                onTap = { onSelect(char) }
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = char,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        fontSize = 18.sp,
                    )
                }
            }
        }
    }
}

private fun handleKeyPress(
    key: KeyLayout.Key,
    service: WavesKeyboardService,
    controller: KeyboardController,
    isUpperCase: Boolean,
) {
    when (key.type) {
        KeyLayout.KeyType.CHARACTER -> {
            val text = if (isUpperCase) key.label.uppercase() else key.label
            service.commitText(text)
            controller.onKeyTapped()
        }
        KeyLayout.KeyType.SPACE -> {
            service.commitText(" ")
        }
        KeyLayout.KeyType.COMMA -> {
            service.commitText(",")
        }
        KeyLayout.KeyType.PERIOD -> {
            service.commitText(".")
        }
        KeyLayout.KeyType.BACKSPACE -> {
            service.deleteBackward()
        }
        KeyLayout.KeyType.ENTER -> {
            service.sendKeyEvent(
                android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_DOWN,
                    android.view.KeyEvent.KEYCODE_ENTER
                )
            )
            service.sendKeyEvent(
                android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_UP,
                    android.view.KeyEvent.KEYCODE_ENTER
                )
            )
        }
        KeyLayout.KeyType.SHIFT -> {
            controller.toggleShift()
        }
        KeyLayout.KeyType.SYMBOLS -> {
            controller.switchPanel(KeyboardController.Panel.SYMBOLS_1)
        }
        KeyLayout.KeyType.SYMBOLS_2 -> {
            controller.switchPanel(KeyboardController.Panel.SYMBOLS_2)
        }
        KeyLayout.KeyType.ALPHA -> {
            controller.switchPanel(KeyboardController.Panel.QWERTY)
        }
        KeyLayout.KeyType.EMOTES -> {
            controller.switchPanel(KeyboardController.Panel.EMOTES)
        }
    }
}

private fun handleLongPress(
    key: KeyLayout.Key,
    service: WavesKeyboardService,
    isUpperCase: Boolean,
) {
    when (key.type) {
        KeyLayout.KeyType.BACKSPACE -> {
            // Long press backspace: delete word
            val textBefore = service.getTextBeforeCursor(50)
            if (textBefore != null) {
                val lastSpace = textBefore.lastIndexOf(' ')
                val deleteCount = if (lastSpace >= 0) textBefore.length - lastSpace else textBefore.length
                service.deleteBackward(deleteCount)
            }
        }
        KeyLayout.KeyType.SPACE -> {
            // Long press space: insert period + space (double-tap space shortcut)
            service.commitText(". ")
        }
        KeyLayout.KeyType.COMMA -> {
            service.commitText("'")
        }
        KeyLayout.KeyType.PERIOD -> {
            service.commitText("…")
        }
        else -> {} // Character alternates handled via popup
    }
}

/**
 * Long-press alternate characters map.
 * Hold a key to see these options in a popup.
 */
val LONG_PRESS_MAP: Map<String, List<String>> = mapOf(
    // Vowels with diacritics
    "a" to listOf("à", "á", "â", "ä", "ã", "å", "æ"),
    "e" to listOf("è", "é", "ê", "ë", "ē"),
    "i" to listOf("ì", "í", "î", "ï", "ī"),
    "o" to listOf("ò", "ó", "ô", "ö", "õ", "ø", "œ"),
    "u" to listOf("ù", "ú", "û", "ü", "ū"),
    "y" to listOf("ÿ", "ý"),
    // Consonants
    "c" to listOf("ç", "ć", "č"),
    "n" to listOf("ñ", "ń"),
    "s" to listOf("ß", "š", "ś"),
    "l" to listOf("ł"),
    "z" to listOf("ž", "ź", "ż"),
    "d" to listOf("ð"),
    "t" to listOf("þ"),
    "r" to listOf("ř"),
    // Symbols on long press of numbers
    "1" to listOf("¹", "½", "⅓", "¼"),
    "2" to listOf("²", "⅔"),
    "3" to listOf("³", "¾"),
    "4" to listOf("⁴"),
    "5" to listOf("⅕"),
    "0" to listOf("°", "∅"),
    // Punctuation
    "-" to listOf("–", "—"),
    "." to listOf("…", "·"),
    "'" to listOf("'", "'", "‚", "‛"),
    "\"" to listOf(""", """, "„", "‟"),
    "!" to listOf("¡"),
    "?" to listOf("¿"),
    "$" to listOf("€", "£", "¥", "₹", "¢"),
)
