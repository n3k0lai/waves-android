package sh.comfy.waves.launcher.home

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs

/**
 * Nova-style page scroll transition effects.
 * Applied to each page in the HorizontalPager based on offset.
 *
 * pageOffset: 0 = centered, -1 = fully left, +1 = fully right
 */
object ScrollEffects {

    fun applyEffect(effectName: String, pageOffset: Float): Modifier {
        return when (effectName) {
            "slide" -> slideEffect(pageOffset)
            "cube" -> cubeEffect(pageOffset)
            "stack" -> stackEffect(pageOffset)
            "tablet" -> tabletEffect(pageOffset)
            "zoom" -> zoomEffect(pageOffset)
            "rotate" -> rotateEffect(pageOffset)
            "flip" -> flipEffect(pageOffset)
            "accordion" -> accordionEffect(pageOffset)
            "none" -> Modifier
            else -> slideEffect(pageOffset)
        }
    }

    /**
     * Default slide — pages move 1:1 with finger.
     */
    private fun slideEffect(offset: Float): Modifier {
        return Modifier // Default pager behavior
    }

    /**
     * 3D cube rotation — pages rotate like faces of a cube.
     */
    private fun cubeEffect(offset: Float): Modifier {
        return Modifier.graphicsLayer {
            val rotation = offset * -90f
            rotationY = rotation.coerceIn(-90f, 90f)
            transformOrigin = if (offset < 0) {
                TransformOrigin(1f, 0.5f)
            } else {
                TransformOrigin(0f, 0.5f)
            }
            // Slight alpha fade at edges
            alpha = 1f - abs(offset) * 0.3f
            cameraDistance = 12f * density
        }
    }

    /**
     * Stack — pages stack on top of each other, sliding off.
     */
    private fun stackEffect(offset: Float): Modifier {
        return Modifier.graphicsLayer {
            if (offset < 0) {
                // Current page: stays in place
                translationX = -offset * size.width
            }
            // Scale down pages behind
            val scale = 1f - abs(offset) * 0.15f
            scaleX = scale.coerceAtLeast(0.7f)
            scaleY = scale.coerceAtLeast(0.7f)
            alpha = 1f - abs(offset) * 0.4f
        }
    }

    /**
     * Tablet — 3D rotation with perspective, like flipping a tablet.
     */
    private fun tabletEffect(offset: Float): Modifier {
        return Modifier.graphicsLayer {
            val rotation = offset * -30f
            rotationY = rotation
            alpha = 1f - abs(offset) * 0.2f
            cameraDistance = 20f * density
        }
    }

    /**
     * Zoom — pages zoom out as they scroll away.
     */
    private fun zoomEffect(offset: Float): Modifier {
        return Modifier.graphicsLayer {
            val scale = 1f - abs(offset) * 0.25f
            scaleX = scale.coerceAtLeast(0.5f)
            scaleY = scale.coerceAtLeast(0.5f)
            alpha = 1f - abs(offset) * 0.5f
        }
    }

    /**
     * Rotate — pages rotate around center.
     */
    private fun rotateEffect(offset: Float): Modifier {
        return Modifier.graphicsLayer {
            rotationZ = offset * -15f
            val scale = 1f - abs(offset) * 0.1f
            scaleX = scale
            scaleY = scale
        }
    }

    /**
     * Flip — pages flip like a book page.
     */
    private fun flipEffect(offset: Float): Modifier {
        return Modifier.graphicsLayer {
            rotationY = offset * -180f
            cameraDistance = 16f * density
            alpha = if (abs(offset) > 0.5f) 0f else 1f
        }
    }

    /**
     * Accordion — pages compress horizontally.
     */
    private fun accordionEffect(offset: Float): Modifier {
        return Modifier.graphicsLayer {
            val scale = 1f - abs(offset) * 0.5f
            scaleX = scale.coerceAtLeast(0.1f)
            transformOrigin = if (offset < 0) {
                TransformOrigin(1f, 0.5f)
            } else {
                TransformOrigin(0f, 0.5f)
            }
        }
    }

    /**
     * All available effect names for the settings picker.
     */
    val allEffects = listOf(
        "slide" to "Slide",
        "cube" to "Cube",
        "stack" to "Stack",
        "tablet" to "Tablet",
        "zoom" to "Zoom Out",
        "rotate" to "Rotate",
        "flip" to "Flip",
        "accordion" to "Accordion",
        "none" to "None",
    )
}
