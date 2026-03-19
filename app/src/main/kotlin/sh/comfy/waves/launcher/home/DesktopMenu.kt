package sh.comfy.waves.launcher.home

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import sh.comfy.waves.MainActivity
import sh.comfy.waves.wallpaper.WavesWallpaperService

/**
 * Nova-style popup menu on long-press of desktop background.
 * Fades up from center with scale animation.
 */
@Composable
fun DesktopMenu(
    visible: Boolean,
    pageCount: Int,
    onDismiss: () -> Unit,
    onAddPage: () -> Unit,
    onRemovePage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + scaleIn(
            initialScale = 0.85f,
            animationSpec = tween(250),
        ),
        exit = fadeOut(tween(150)) + scaleOut(
            targetScale = 0.85f,
            animationSpec = tween(200),
        ),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                ),
                modifier = Modifier
                    .padding(horizontal = 48.dp)
                    .clickable(enabled = false) { /* consume clicks */ },
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    DesktopMenuItem(
                        icon = Icons.Default.Wallpaper,
                        label = "Wallpaper",
                        onClick = {
                            onDismiss()
                            // Will be handled by the caller with context
                        },
                        tag = "wallpaper",
                    )

                    DesktopMenuItem(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        onClick = {
                            onDismiss()
                        },
                        tag = "settings",
                    )

                    DesktopMenuItem(
                        icon = Icons.Default.Add,
                        label = "Add Page",
                        onClick = {
                            onAddPage()
                        },
                    )

                    DesktopMenuItem(
                        icon = Icons.Default.Remove,
                        label = "Remove Page",
                        enabled = pageCount > 1,
                        onClick = {
                            onRemovePage()
                        },
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "$pageCount page${if (pageCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tag: String = "",
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                when (tag) {
                    "wallpaper" -> {
                        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                            putExtra(
                                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                ComponentName(context, WavesWallpaperService::class.java),
                            )
                        }
                        context.startActivity(intent)
                    }
                    "settings" -> {
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                    else -> onClick()
                }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
    }
}
