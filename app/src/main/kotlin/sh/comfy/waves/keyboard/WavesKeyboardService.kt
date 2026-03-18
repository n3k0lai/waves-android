package sh.comfy.waves.keyboard

import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.view.View
import android.view.inputmethod.EditorInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import sh.comfy.waves.keyboard.core.BackspaceHandler
import sh.comfy.waves.keyboard.core.KeyboardPreferences
import sh.comfy.waves.keyboard.core.TextProcessor
import sh.comfy.waves.keyboard.view.KeyboardView

/**
 * Waves Keyboard IME Service.
 *
 * This is the main entry point for the keyboard. Android manages its lifecycle:
 * - onCreateInputView: called once when the keyboard is first shown
 * - onStartInputView: called each time a text field gains focus
 * - onFinishInputView: called when the text field loses focus
 */
class WavesKeyboardService : InputMethodService() {

    private val controller = KeyboardController()
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    lateinit var backspaceHandler: BackspaceHandler
        private set
    lateinit var textProcessor: TextProcessor
        private set
    lateinit var prefs: KeyboardPreferences
        private set

    private var audioManager: AudioManager? = null

    override fun onCreate() {
        super.onCreate()
        backspaceHandler = BackspaceHandler(this, scope)
        textProcessor = TextProcessor(this)
        prefs = KeyboardPreferences(this)
        audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
    }

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
        backspaceHandler.stopRepeating()
    }

    override fun onDestroy() {
        super.onDestroy()
        backspaceHandler.stopRepeating()
        scope.cancel()
    }

    /**
     * Commit text to the current editor.
     */
    fun commitText(text: CharSequence) {
        currentInputConnection?.commitText(text, 1)
        textProcessor.onNonSpaceKey()
        playKeySound()
    }

    /**
     * Handle space with double-tap-to-period logic.
     */
    fun handleSpace() {
        if (prefs.doubleTapPeriod) {
            textProcessor.handleSpace()
        } else {
            currentInputConnection?.commitText(" ", 1)
        }
        playKeySound()
    }

    /**
     * Delete characters before the cursor.
     */
    fun deleteBackward(count: Int = 1) {
        currentInputConnection?.deleteSurroundingText(count, 0)
        playKeySound()
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

    /**
     * Check if auto-capitalize should engage after committing text.
     */
    fun shouldAutoCapitalize(): Boolean {
        return prefs.autoCapitalize && textProcessor.shouldAutoCapitalize()
    }

    /**
     * Play key click sound if enabled.
     */
    private fun playKeySound() {
        if (!prefs.soundEnabled) return
        audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1f)
    }
}
