package sh.comfy.waves.launcher.dock

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import sh.comfy.waves.launcher.data.AppInfo

/**
 * Bottom dock — persistent row of favorite apps.
 * Semi-transparent background, rounded corners on top.
 * Supports multiple pages (swipeable) like Nova.
 */
@Composable
fun Dock(
    apps: List<AppInfo>,
    columns: Int = 5,
    backgroundAlpha: Float = 0.5f,
    iconSize: Dp = 48.dp,
    showLabels: Boolean = false,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Color.Black.copy(alpha = backgroundAlpha)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Show up to `columns` apps, pad with empty slots
            for (i in 0 until columns) {
                val app = apps.getOrNull(i)
                if (app != null) {
                    DockIcon(
                        app = app,
                        iconSize = iconSize,
                        showLabel = showLabels,
                        onClick = { onAppClick(app) },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    // Empty dock slot
                    Box(modifier = Modifier.weight(1f).height(iconSize + 8.dp))
                }
            }
        }
    }
}

@Composable
fun DockIcon(
    app: AppInfo,
    iconSize: Dp = 48.dp,
    showLabel: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp),
    ) {
        app.icon?.let { drawable ->
            val bitmap = remember(app.packageName) {
                drawable.toBitmap(
                    width = iconSize.value.toInt() * 2,
                    height = iconSize.value.toInt() * 2,
                )
            }
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier.size(iconSize),
            )
        }

        if (showLabel) {
            Text(
                text = app.label,
                fontSize = 10.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
