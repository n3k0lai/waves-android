package sh.comfy.waves.launcher.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import sh.comfy.waves.launcher.data.AppInfo
import sh.comfy.waves.launcher.data.FolderApp
import sh.comfy.waves.launcher.drawer.AppGridItem

/**
 * Folder popup dialog — shows a grid of apps inside a folder.
 * Nova-style: centered dialog with folder name and grid.
 */
@Composable
fun FolderPopup(
    name: String,
    apps: List<FolderApp>,
    resolveApp: (String, String) -> AppInfo?,
    onAppClick: (FolderApp) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = name,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
            ) {
                items(apps) { folderApp ->
                    val app = resolveApp(folderApp.packageName, folderApp.activityName)
                    if (app != null) {
                        AppGridItem(
                            app = app,
                            iconSize = 48.dp,
                            labelSize = 11f,
                            showLabel = true,
                            onClick = { onAppClick(folderApp) },
                            onLongClick = {},
                        )
                    }
                }
            }
        }
    }
}
