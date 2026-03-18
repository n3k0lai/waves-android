package sh.comfy.waves.keyboard.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sh.comfy.waves.keyboard.WavesKeyboardService

/**
 * Handles continuous backspace deletion while the key is held.
 * Starts slow, accelerates over time (mimics native keyboard behavior).
 */
class BackspaceHandler(
    private val service: WavesKeyboardService,
    private val scope: CoroutineScope,
) {
    private var repeatJob: Job? = null

    fun startRepeating() {
        stopRepeating()
        repeatJob = scope.launch {
            // Initial delay before repeat starts
            delay(400)
            var intervalMs = 80L
            while (isActive) {
                service.deleteBackward()
                delay(intervalMs)
                // Accelerate: drop interval to minimum 30ms
                if (intervalMs > 30) intervalMs -= 5
            }
        }
    }

    fun stopRepeating() {
        repeatJob?.cancel()
        repeatJob = null
    }
}
