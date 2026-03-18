package sh.comfy.waves.keyboard

import android.view.inputmethod.EditorInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Keyboard state machine. Manages which panel is shown and modifier state.
 *
 * Panels:
 * - QWERTY: standard alpha keys
 * - SYMBOLS_1: numbers + common symbols (!@#$%...)
 * - SYMBOLS_2: less common symbols ({}[]|~...)
 * - EMOTES: emote grid from emotes.comfy.sh
 */
class KeyboardController {

    enum class Panel {
        QWERTY,
        SYMBOLS_1,
        SYMBOLS_2,
        EMOTES,
    }

    data class State(
        val panel: Panel = Panel.QWERTY,
        val shiftState: ShiftState = ShiftState.OFF,
        val inputType: Int = EditorInfo.TYPE_CLASS_TEXT,
        val imeAction: Int = EditorInfo.IME_ACTION_UNSPECIFIED,
    )

    enum class ShiftState {
        OFF,        // lowercase
        SINGLE,     // next letter uppercase, then back to OFF
        CAPS_LOCK,  // all uppercase until toggled off
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun onEditorStart(info: EditorInfo) {
        _state.value = _state.value.copy(
            inputType = info.inputType,
            imeAction = info.imeOptions and EditorInfo.IME_MASK_ACTION,
            // Auto-capitalize first letter
            shiftState = if (shouldAutoCapitalize(info)) ShiftState.SINGLE else ShiftState.OFF,
            panel = panelForInputType(info.inputType),
        )
    }

    fun onEditorFinish() {
        _state.value = State()
    }

    fun toggleShift() {
        _state.value = _state.value.copy(
            shiftState = when (_state.value.shiftState) {
                ShiftState.OFF -> ShiftState.SINGLE
                ShiftState.SINGLE -> ShiftState.CAPS_LOCK
                ShiftState.CAPS_LOCK -> ShiftState.OFF
            }
        )
    }

    fun onKeyTapped() {
        // After a key press in SINGLE shift mode, go back to OFF
        if (_state.value.shiftState == ShiftState.SINGLE) {
            _state.value = _state.value.copy(shiftState = ShiftState.OFF)
        }
    }

    fun switchPanel(panel: Panel) {
        _state.value = _state.value.copy(panel = panel)
    }

    fun isUpperCase(): Boolean {
        return _state.value.shiftState != ShiftState.OFF
    }

    private fun shouldAutoCapitalize(info: EditorInfo): Boolean {
        val caps = info.inputType and EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES
        return caps != 0
    }

    private fun panelForInputType(inputType: Int): Panel {
        val cls = inputType and EditorInfo.TYPE_MASK_CLASS
        return when (cls) {
            EditorInfo.TYPE_CLASS_NUMBER,
            EditorInfo.TYPE_CLASS_PHONE -> Panel.SYMBOLS_1
            else -> Panel.QWERTY
        }
    }
}
