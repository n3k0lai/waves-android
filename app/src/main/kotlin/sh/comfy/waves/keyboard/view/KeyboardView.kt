package sh.comfy.waves.keyboard.view

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import sh.comfy.waves.keyboard.KeyLayout
import sh.comfy.waves.keyboard.KeyboardController
import sh.comfy.waves.keyboard.WavesKeyboardService
import sh.comfy.waves.ui.theme.WavesTheme

/**
 * Root view for the keyboard. Wraps a ComposeView inside a FrameLayout
 * because InputMethodService.onCreateInputView expects a traditional View.
 */
class KeyboardView(
    private val service: WavesKeyboardService,
    private val controller: KeyboardController,
) : FrameLayout(service) {

    init {
        val composeView = ComposeView(service).apply {
            setContent {
                WavesTheme {
                    KeyboardRoot(service, controller)
                }
            }
        }

        // IME needs a LifecycleOwner for Compose
        composeView.setViewTreeLifecycleOwner(
            androidx.lifecycle.ProcessLifecycleOwner.get()
        )
        composeView.setViewTreeSavedStateRegistryOwner(
            // IME services don't have a SavedStateRegistryOwner by default,
            // but we can use a no-op one since keyboard state is transient
            null
        )

        addView(composeView)
    }
}

@Composable
fun KeyboardRoot(
    service: WavesKeyboardService,
    controller: KeyboardController,
) {
    val state by controller.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 3.dp, vertical = 6.dp),
    ) {
        when (state.panel) {
            KeyboardController.Panel.QWERTY -> {
                KeyRows(
                    rows = KeyLayout.qwertyRows,
                    service = service,
                    controller = controller,
                    isUpperCase = controller.isUpperCase(),
                )
            }
            KeyboardController.Panel.SYMBOLS_1 -> {
                KeyRows(
                    rows = KeyLayout.symbols1Rows,
                    service = service,
                    controller = controller,
                    isUpperCase = false,
                )
            }
            KeyboardController.Panel.SYMBOLS_2 -> {
                KeyRows(
                    rows = KeyLayout.symbols2Rows,
                    service = service,
                    controller = controller,
                    isUpperCase = false,
                )
            }
            KeyboardController.Panel.EMOTES -> {
                // Placeholder — emote panel will be a LazyVerticalGrid
                EmotePanelPlaceholder(
                    onBack = { controller.switchPanel(KeyboardController.Panel.QWERTY) }
                )
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
                    modifier = Modifier.weight(key.weight / totalWeight),
                    onClick = { handleKeyPress(key, service, controller, isUpperCase) },
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
    onClick: () -> Unit,
) {
    val bgColor = when (key.type) {
        KeyLayout.KeyType.CHARACTER,
        KeyLayout.KeyType.COMMA,
        KeyLayout.KeyType.PERIOD -> MaterialTheme.colorScheme.surfaceVariant
        KeyLayout.KeyType.SPACE -> MaterialTheme.colorScheme.surfaceVariant
        KeyLayout.KeyType.ENTER -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
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

    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
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
}

@Composable
fun EmotePanelPlaceholder(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Emote panel loading...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
            Text(
                "ABC",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clickable { onBack() },
            )
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
