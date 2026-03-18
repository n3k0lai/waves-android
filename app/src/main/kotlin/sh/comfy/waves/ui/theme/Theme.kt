package sh.comfy.waves.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Waves always defaults to dark — void black + teal ocean.
 * On Android 12+, dynamicDarkColorScheme pulls wallpaper colors,
 * which may actually pick up teal/sand from the waves wallpaper itself.
 * We fall back to our hand-tuned palette otherwise.
 */
private val WavesDarkColorScheme = darkColorScheme(
    primary = WavesPrimary,
    onPrimary = WavesOnPrimary,
    primaryContainer = WavesPrimaryDark,
    onPrimaryContainer = WavesPrimaryLight,
    secondary = WavesSecondary,
    onSecondary = WavesOnSecondary,
    secondaryContainer = WavesSecondaryDark,
    onSecondaryContainer = WavesSecondary,
    tertiary = WavesTertiary,
    onTertiary = WavesOnTertiary,
    background = WavesBackground,
    onBackground = WavesOnBackground,
    surface = WavesSurface,
    onSurface = WavesOnSurface,
    surfaceVariant = WavesSurfaceVariant,
    onSurfaceVariant = WavesOnSurfaceVariant,
    error = WavesError,
    onError = WavesOnError,
    outline = WavesOutline,
    outlineVariant = WavesOutlineVariant,
)

@Composable
fun WavesTheme(
    // Waves is always dark by default — the void calls
    darkTheme: Boolean = true,
    // Material You dynamic color: when the waves wallpaper is set,
    // Android's monet engine will extract teal/sand automatically
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Let Android extract colors from the waves wallpaper
            // This creates a natural feedback loop: wallpaper → theme → UI
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        else -> WavesDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
