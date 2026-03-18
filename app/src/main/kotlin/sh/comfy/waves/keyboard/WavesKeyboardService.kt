package sh.comfy.waves.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import sh.comfy.waves.keyboard.view.KeyboardView

/**
 * Waves Keyboard IME Service.
 *
 * This is the main entry point for the keyboard. Android manages its lifecycle:
 * - onCreateInputView: called once when the keyboard is first shown
 * - onStartInputView: called each time a text field gains focus
 * - onFinishInputView: called when the text field loses focus
 *
 * Architecture:
 * - WavesKeyboardService (this) — Android IME lifecycle, commits text to editors
 * - KeyboardView — Compose-based UI rendering (keys, emote panel, theme)
 * - KeyboardController — state machine (qwerty/symbols/emotes/settings)
 * - EmoteRepository — fetches and caches emotes from emotes.comfy.sh
 */
class WavesKeyboardService : InputMethodService() {

    private val controller = KeyboardController()

    override fun onCreateInputView(): View {
        return KeyboardView(this, controller)
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        controller.onEditorStart(info)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        controller.onEditorFinish()
    }

    /**
     * Commit text to the current editor.
     * Called by KeyboardView when a key is tapped or an emote is selected.
     */
    fun commitText(text: CharSequence) {
        currentInputConnection?.commitText(text, 1)
    }

    /**
     * Delete characters before the cursor.
     */
    fun deleteBackward(count: Int = 1) {
        currentInputConnection?.deleteSurroundingText(count, 0)
    }

    /**
     * Send a key event (Enter, etc.)
     */
    fun sendKeyEvent(event: android.view.KeyEvent) {
        currentInputConnection?.sendKeyEvent(event)
    }

    /**
     * Get text before cursor for autocomplete context.
     */
    fun getTextBeforeCursor(length: Int): CharSequence? {
        return currentInputConnection?.getTextBeforeCursor(length, 0)
    }
}
