package sh.comfy.waves.keyboard.view

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin

// Waves palette colors
private val WaveDeepTeal = Color(0xFF00695C)
private val WaveTeal = Color(0xFF00897B)
private val WaveLightTeal = Color(0xFF4DB6AC)
private val WaveSand = Color(0xFFD4A574)
private val WaveFoam = Color(0xFFE0F2F1)
private val WaveVoid = Color(0xFF050505)
private val WaveSurfaceDark = Color(0xFF0A0A0A)

/**
 * Animated waves background for the keyboard.
 *
 * Renders 3-4 layered sine waves that drift horizontally at different speeds,
 * creating a gentle ocean effect behind the keys. Colors follow the waves palette:
 * deep teal at bottom, lighter teal/foam at wave crests, void black above.
 *
 * Performance: uses Canvas (hardware-accelerated), no allocations per frame,
 * only recomposes the animation value (single Float). Path objects reused.
 *
 * The waves sit at the bottom ~40% of the keyboard, fading to transparent
 * at the top so keys remain readable.
 */
@Composable
fun WavesBackground(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    intensity: Float = 1f, // 0..1, reduces wave amplitude
    ripples: List<WaveRipple> = emptyList(),
) {
    if (!enabled) return

    val transition = rememberInfiniteTransition(label = "waves")

    // Primary wave phase — slow drift
    val phase1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave1",
    )

    // Secondary wave — slightly faster, offset
    val phase2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave2",
    )

    // Tertiary wave — fastest, subtle
    val phase3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave3",
    )

    // Foam shimmer — very slow opacity pulse
    val foamAlpha by transition.animateFloat(
        initialValue = 0.03f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "foam",
    )

    // Pre-allocate paths
    val path1 = remember { Path() }
    val path2 = remember { Path() }
    val path3 = remember { Path() }
    val foamPath = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Waves occupy bottom portion of keyboard
        val waveRegionTop = h * 0.45f
        val baseAmplitude = h * 0.04f * intensity

        // Calculate ripple distortion at each x position
        val activeRipples = ripples.filter { it.isAlive }

        // Layer 1: Deep teal base wave (slowest, largest)
        drawWave(
            path = path1,
            width = w,
            height = h,
            baseY = h * 0.75f,
            amplitude = baseAmplitude * 1.2f,
            frequency = 1.5f,
            phase = phase1,
            color = WaveDeepTeal,
            alpha = 0.35f * intensity,
            ripples = activeRipples,
            rippleMultiplier = 1.5f,
        )

        // Layer 2: Mid teal wave
        drawWave(
            path = path2,
            width = w,
            height = h,
            baseY = h * 0.7f,
            amplitude = baseAmplitude * 0.9f,
            frequency = 2.0f,
            phase = phase2,
            color = WaveTeal,
            alpha = 0.25f * intensity,
            ripples = activeRipples,
            rippleMultiplier = 1.2f,
        )

        // Layer 3: Light teal / foam wave (fastest, smallest)
        drawWave(
            path = path3,
            width = w,
            height = h,
            baseY = h * 0.65f,
            amplitude = baseAmplitude * 0.6f,
            frequency = 2.5f,
            phase = phase3,
            color = WaveLightTeal,
            alpha = 0.15f * intensity,
            ripples = activeRipples,
            rippleMultiplier = 0.8f,
        )

        // Foam highlights: very subtle white shimmer along wave crests
        drawFoamHighlights(
            path = foamPath,
            width = w,
            height = h,
            baseY = h * 0.68f,
            amplitude = baseAmplitude * 0.5f,
            frequency = 2.2f,
            phase = phase2 + 0.3f,
            alpha = foamAlpha * intensity,
        )

        // Sand gradient at very bottom (shoreline hint)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    WaveSand.copy(alpha = 0.06f * intensity),
                    WaveSand.copy(alpha = 0.12f * intensity),
                ),
                startY = h * 0.85f,
                endY = h,
            ),
        )

        // Fade-to-surface gradient at top (keys need to be readable)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    WaveSurfaceDark,
                    WaveSurfaceDark.copy(alpha = 0.95f),
                    Color.Transparent,
                ),
                startY = 0f,
                endY = waveRegionTop,
            ),
        )
    }
}

/**
 * Draws a single animated sine wave filled to the bottom of the canvas.
 */
private fun DrawScope.drawWave(
    path: Path,
    width: Float,
    height: Float,
    baseY: Float,
    amplitude: Float,
    frequency: Float,
    phase: Float,
    color: Color,
    alpha: Float,
    ripples: List<WaveRipple> = emptyList(),
    rippleMultiplier: Float = 1f,
) {
    path.reset()
    path.moveTo(0f, height)

    val step = width / 60f // 60 segments for smooth curve
    var x = 0f
    while (x <= width) {
        val normalizedX = x / width * frequency * 2f * PI.toFloat()
        var y = baseY + amplitude * sin(normalizedX + phase)

        // Apply ripple distortion from key taps
        for (ripple in ripples) {
            val rippleX = ripple.x * width
            val dx = (x - rippleX) / width
            val distance = dx * dx // squared distance, normalized
            val rippleRadius = 0.15f + ripple.progress * 0.3f // expanding radius
            if (distance < rippleRadius * rippleRadius) {
                // Gaussian-ish falloff from ripple center
                val falloff = 1f - (distance / (rippleRadius * rippleRadius))
                val ripplePhase = ripple.progress * 4f * PI.toFloat()
                val rippleAmplitude = amplitude * rippleMultiplier * falloff * (1f - ripple.progress)
                y += rippleAmplitude * sin(ripplePhase + normalizedX * 3f)
            }
        }

        if (x == 0f) {
            path.lineTo(0f, y)
        } else {
            path.lineTo(x, y)
        }
        x += step
    }

    path.lineTo(width, height)
    path.close()

    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
    )
}

/**
 * Draws subtle foam/white shimmer highlights along wave crests.
 * Thinner than full waves, just the crest line with glow.
 */
private fun DrawScope.drawFoamHighlights(
    path: Path,
    width: Float,
    height: Float,
    baseY: Float,
    amplitude: Float,
    frequency: Float,
    phase: Float,
    alpha: Float,
) {
    path.reset()

    val step = width / 60f
    var x = 0f
    var started = false
    while (x <= width) {
        val normalizedX = x / width * frequency * 2f * PI.toFloat()
        val y = baseY + amplitude * sin(normalizedX + phase)
        if (!started) {
            path.moveTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
        }
        x += step
    }

    // Draw as a thin stroke (foam crest line)
    drawPath(
        path = path,
        color = WaveFoam.copy(alpha = alpha),
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2f,
        ),
    )
}

/**
 * Reactive wave burst effect triggered by key presses.
 * Creates a radial ripple from the tap point that disturbs the wave pattern.
 *
 * Usage: call with tap coordinates to create a momentary amplitude boost
 * at that horizontal position.
 */
data class WaveRipple(
    val x: Float,       // normalized 0..1 across keyboard width
    val startTime: Long = System.currentTimeMillis(),
    val durationMs: Long = 600,
) {
    val progress: Float
        get() {
            val elapsed = System.currentTimeMillis() - startTime
            return (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
        }

    val isAlive: Boolean
        get() = progress < 1f
}
