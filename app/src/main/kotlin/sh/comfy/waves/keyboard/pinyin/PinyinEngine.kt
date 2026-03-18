package sh.comfy.waves.keyboard.pinyin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the pinyin composition state for 9-key input.
 *
 * Flow:
 * 1. User taps digit keys (2-9) → digits accumulate in inputBuffer
 * 2. Engine resolves digit sequence → possible pinyin syllables → candidate characters
 * 3. User taps a candidate → character is committed, buffer clears
 * 4. User can also split ambiguous sequences (e.g., "7464" → "shi" + "g..." or "qing")
 */
class PinyinEngine(private val dictionary: PinyinDictionary) {

    data class State(
        val inputDigits: String = "",           // current T9 digit buffer
        val candidates: List<PinyinDictionary.Candidate> = emptyList(),
        val activeSyllable: String? = null,     // syllable user is browsing
        val composingText: String = "",         // display text in composition bar
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    /**
     * Append a digit (2-9) to the input buffer.
     */
    fun inputDigit(digit: Char) {
        if (digit !in '2'..'9') return
        val newDigits = _state.value.inputDigits + digit
        val candidates = dictionary.lookup(newDigits)
        val composing = buildComposingText(newDigits, candidates)
        _state.value = State(
            inputDigits = newDigits,
            candidates = candidates,
            composingText = composing,
        )
    }

    /**
     * Remove the last digit from the buffer (backspace).
     */
    fun backspace() {
        val current = _state.value.inputDigits
        if (current.isEmpty()) return
        val newDigits = current.dropLast(1)
        if (newDigits.isEmpty()) {
            clear()
            return
        }
        val candidates = dictionary.lookup(newDigits)
        val composing = buildComposingText(newDigits, candidates)
        _state.value = State(
            inputDigits = newDigits,
            candidates = candidates,
            composingText = composing,
        )
    }

    /**
     * Select a candidate character. Returns the character to commit.
     */
    fun selectCandidate(candidate: PinyinDictionary.Candidate): String {
        val selectedChar = candidate.char
        val syllableDigits = NinekeyMap.pinyinToDigits(candidate.pinyin)
        val remaining = _state.value.inputDigits.removePrefix(syllableDigits)

        if (remaining.isEmpty()) {
            clear()
        } else {
            // There are remaining digits — continue composing
            val candidates = dictionary.lookup(remaining)
            val composing = buildComposingText(remaining, candidates)
            _state.value = State(
                inputDigits = remaining,
                candidates = candidates,
                composingText = composing,
            )
        }

        return selectedChar
    }

    /**
     * Clear all composition state.
     */
    fun clear() {
        _state.value = State()
    }

    /**
     * Check if there's an active composition.
     */
    fun isComposing(): Boolean = _state.value.inputDigits.isNotEmpty()

    /**
     * Build the display text shown in the composition bar.
     * Shows the most likely pinyin reading for the current digits.
     */
    private fun buildComposingText(
        digits: String,
        candidates: List<PinyinDictionary.Candidate>,
    ): String {
        // Find the best exact-match syllable
        val bestExact = candidates.firstOrNull { it.isExact }?.pinyin
        if (bestExact != null) return bestExact

        // If no exact match, show the digit sequence with possible readings
        val partials = candidates.map { it.pinyin }.distinct().take(3)
        return if (partials.isNotEmpty()) {
            partials.first() + "..."
        } else {
            digits // fallback: just show the digits
        }
    }
}
