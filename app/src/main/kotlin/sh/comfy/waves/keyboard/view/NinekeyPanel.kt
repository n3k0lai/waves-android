package sh.comfy.waves.keyboard.view

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sh.comfy.waves.keyboard.KeyLayout
import sh.comfy.waves.keyboard.KeyboardController
import sh.comfy.waves.keyboard.WavesKeyboardService
import sh.comfy.waves.keyboard.pinyin.PinyinDictionary
import sh.comfy.waves.keyboard.pinyin.PinyinEngine

/**
 * 九宫格 (9-key) pinyin input panel.
 *
 * Layout:
 * ┌─────────────────────────────┐
 * │ [composition] 拼: ni hao    │  ← composing text / pinyin reading
 * │ 你 好 呢 泥 拟 逆 ...       │  ← scrollable candidate bar
 * ├─────────┬─────────┬─────────┤
 * │ 1 .,?!  │ 2 ABC   │ 3 DEF  │
 * │ 4 GHI   │ 5 JKL   │ 6 MNO  │
 * │ 7 PQRS  │ 8 TUV   │ 9 WXYZ │
 * │ 🌐 lang │ 0 space  │ ⌫ del  │
 * └─────────┴─────────┴─────────┘
 */
@Composable
fun NinekeyPanel(
    service: WavesKeyboardService,
    controller: KeyboardController,
    pinyinEngine: PinyinEngine,
    modifier: Modifier = Modifier,
) {
    val pinyinState by pinyinEngine.state.collectAsState()
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp, vertical = 4.dp),
    ) {
        // Composition bar (shows current pinyin reading)
        if (pinyinState.composingText.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "拼:",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text(
                    text = pinyinState.composingText,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // Candidate bar (scrollable)
        if (pinyinState.candidates.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // Group by unique characters (avoid duplicates from different syllables)
                val seen = mutableSetOf<String>()
                pinyinState.candidates.forEach { candidate ->
                    if (seen.add(candidate.char)) {
                        CandidateChip(
                            char = candidate.char,
                            pinyin = candidate.pinyin,
                            isExact = candidate.isExact,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                val committed = pinyinEngine.selectCandidate(candidate)
                                service.commitText(committed)
                            },
                        )
                    }
                }
            }
        } else if (pinyinState.inputDigits.isNotEmpty()) {
            // No candidates found
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "无匹配",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
        }

        // 9-key grid (3x4)
        val keys = listOf(
            listOf(
                NineKey("1", ".,?!", KeyAction.PUNCTUATION),
                NineKey("2", "ABC", KeyAction.DIGIT),
                NineKey("3", "DEF", KeyAction.DIGIT),
            ),
            listOf(
                NineKey("4", "GHI", KeyAction.DIGIT),
                NineKey("5", "JKL", KeyAction.DIGIT),
                NineKey("6", "MNO", KeyAction.DIGIT),
            ),
            listOf(
                NineKey("7", "PQRS", KeyAction.DIGIT),
                NineKey("8", "TUV", KeyAction.DIGIT),
                NineKey("9", "WXYZ", KeyAction.DIGIT),
            ),
            listOf(
                NineKey("🌐", "ABC", KeyAction.SWITCH_LANG),
                NineKey("0", "space", KeyAction.SPACE),
                NineKey("⌫", "", KeyAction.BACKSPACE),
            ),
        )

        keys.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                row.forEach { key ->
                    NinekeyButton(
                        key = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            handleNinekey(key, service, controller, pinyinEngine)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun CandidateChip(
    char: String,
    pinyin: String,
    isExact: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .width(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isExact) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = char,
                fontSize = 20.sp,
                color = if (isExact) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = pinyin,
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
fun NinekeyButton(
    key: NineKey,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bgColor = when (key.action) {
        KeyAction.BACKSPACE -> MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
        KeyAction.SWITCH_LANG -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        KeyAction.SPACE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
    }

    val textColor = when (key.action) {
        KeyAction.SWITCH_LANG -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .pointerInput(key) {
                detectTapGestures(onTap = { onClick() })
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = key.label,
                color = textColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            if (key.subLabel.isNotEmpty() && key.action == KeyAction.DIGIT) {
                Text(
                    text = key.subLabel,
                    color = textColor.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

data class NineKey(
    val label: String,
    val subLabel: String,
    val action: KeyAction,
)

enum class KeyAction {
    DIGIT,          // Pinyin digit input (2-9)
    PUNCTUATION,    // Key 1: cycle through .,?!
    SPACE,          // Key 0: commit space or select first candidate
    BACKSPACE,      // Delete digit from buffer or delete backward
    SWITCH_LANG,    // Switch back to QWERTY
}

private val punctuationCycle = listOf("。", "，", "？", "！", "、", "；", "：", ".", ",", "?", "!")
private var punctuationIndex = 0

private fun handleNinekey(
    key: NineKey,
    service: WavesKeyboardService,
    controller: KeyboardController,
    engine: PinyinEngine,
) {
    when (key.action) {
        KeyAction.DIGIT -> {
            val digit = key.label.firstOrNull() ?: return
            engine.inputDigit(digit)
        }
        KeyAction.BACKSPACE -> {
            if (engine.isComposing()) {
                engine.backspace()
            } else {
                service.deleteBackward()
            }
        }
        KeyAction.SPACE -> {
            if (engine.isComposing()) {
                // Space selects first candidate
                val state = engine.state.value
                val firstCandidate = state.candidates.firstOrNull()
                if (firstCandidate != null) {
                    val committed = engine.selectCandidate(firstCandidate)
                    service.commitText(committed)
                } else {
                    // No candidates — commit the raw digits and clear
                    service.commitText(state.inputDigits)
                    engine.clear()
                }
            } else {
                service.commitText(" ")
            }
        }
        KeyAction.PUNCTUATION -> {
            if (engine.isComposing()) {
                // If composing, commit first candidate then punctuation
                val state = engine.state.value
                val firstCandidate = state.candidates.firstOrNull()
                if (firstCandidate != null) {
                    val committed = engine.selectCandidate(firstCandidate)
                    service.commitText(committed)
                }
            }
            service.commitText(punctuationCycle[punctuationIndex % punctuationCycle.size])
            punctuationIndex++
        }
        KeyAction.SWITCH_LANG -> {
            // Clear any composition and switch to QWERTY
            if (engine.isComposing()) engine.clear()
            controller.switchPanel(KeyboardController.Panel.QWERTY)
        }
    }
}
