package sh.comfy.waves.keyboard.core

import sh.comfy.waves.keyboard.WavesKeyboardService

/**
 * Text processing utilities: auto-period, auto-capitalize, double-space shortcuts.
 */
class TextProcessor(private val service: WavesKeyboardService) {

    private var lastKeyTime = 0L
    private var lastKeyWasSpace = false

    /**
     * Handle space key with double-tap-to-period behavior.
     * Returns true if the space was converted to ". " (period + space).
     */
    fun handleSpace(): Boolean {
        val now = System.currentTimeMillis()
        val timeSinceLastKey = now - lastKeyTime

        if (lastKeyWasSpace && timeSinceLastKey < 400) {
            // Double-tap space: replace trailing space with ". "
            service.deleteBackward(1)
            service.commitText(". ")
            lastKeyWasSpace = false
            lastKeyTime = now
            return true
        }

        service.commitText(" ")
        lastKeyWasSpace = true
        lastKeyTime = now
        return false
    }

    /**
     * Record that a non-space key was pressed (resets double-tap tracking).
     */
    fun onNonSpaceKey() {
        lastKeyWasSpace = false
        lastKeyTime = System.currentTimeMillis()
    }

    /**
     * Check if auto-capitalize should trigger (after sentence-ending punctuation).
     */
    fun shouldAutoCapitalize(): Boolean {
        val textBefore = service.getTextBeforeCursor(3) ?: return false
        val trimmed = textBefore.trimEnd()
        if (trimmed.isEmpty()) return true
        val lastChar = trimmed.last()
        return lastChar in ".!?" || textBefore.isBlank()
    }
}
