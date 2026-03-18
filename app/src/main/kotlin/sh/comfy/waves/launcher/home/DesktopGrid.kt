package sh.comfy.waves.launcher.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import sh.comfy.waves.launcher.data.AppInfo
import sh.comfy.waves.launcher.data.DesktopItem

/**
 * Single desktop page — a grid of app shortcuts, widgets, and folders.
 * Items are placed at specific (col, row) positions.
 * Empty cells are droppable for drag-and-drop.
 */
@Composable
fun DesktopGrid(
    items: List<DesktopItem>,
    columns: Int = 5,
    rows: Int = 5,
    iconSize: Dp = 56.dp,
    labelSize: Float = 12f,
    showLabels: Boolean = true,
    iconScale: Float = 1f,
    resolveApp: (String, String) -> AppInfo?,
    onItemClick: (DesktopItem) -> Unit,
    onItemLongClick: (DesktopItem) -> Unit,
    onEmptyCellClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scaledIconSize = iconSize * iconScale

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (col in 0 until columns) {
                    val item = items.find { it.col == col && it.row == row }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (item != null) {
                            DesktopCell(
                                item = item,
                                iconSize = scaledIconSize,
                                labelSize = labelSize,
                                showLabel = showLabels,
                                resolveApp = resolveApp,
                                onClick = { onItemClick(item) },
                                onLongClick = { onItemLongClick(item) },
                            )
                        } else {
                            // Empty cell — tappable for adding items
                            Box(
                                modifier = Modifier
                                    .size(scaledIconSize)
                                    .clickable { onEmptyCellClick(col, row) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DesktopCell(
    item: DesktopItem,
    iconSize: Dp,
    labelSize: Float,
    showLabel: Boolean,
    resolveApp: (String, String) -> AppInfo?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    when (item) {
        is DesktopItem.AppShortcut -> {
            val app = resolveApp(item.packageName, item.activityName)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
            ) {
                app?.icon?.let { drawable ->
                    val bitmap = remember(item.packageName) {
                        drawable.toBitmap(
                            width = iconSize.value.toInt() * 2,
                            height = iconSize.value.toInt() * 2,
                        )
                    }
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = item.label.ifEmpty { app.label },
                        modifier = Modifier.size(iconSize),
                    )
                }
                if (showLabel) {
                    Text(
                        text = item.label.ifEmpty { app?.label ?: item.packageName },
                        fontSize = labelSize.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        is DesktopItem.FolderItem -> {
            FolderIcon(
                name = item.name,
                appCount = item.apps.size,
                iconSize = iconSize,
                labelSize = labelSize,
                showLabel = showLabel,
                onClick = onClick,
                onLongClick = onLongClick,
            )
        }
        is DesktopItem.WidgetItem -> {
            // Widget rendering handled by AppWidgetHostView — placeholder here
            Box(
                modifier = Modifier
                    .size(iconSize * item.spanCols, iconSize * item.spanRows)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Widget #${item.widgetId}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderIcon(
    name: String,
    appCount: Int,
    iconSize: Dp,
    labelSize: Float,
    showLabel: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
    ) {
        // Simple folder visual: rounded square with count
        Box(
            modifier = Modifier
                .size(iconSize)
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Folder background
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.15f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
                )
            }
            Text(
                text = "$appCount",
                color = Color.White,
                fontSize = 18.sp,
            )
        }
        if (showLabel) {
            Text(
                text = name,
                fontSize = labelSize.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
