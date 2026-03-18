package sh.comfy.waves.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sh.comfy.waves.launcher.data.LauncherSettings
import sh.comfy.waves.launcher.home.ScrollEffects

@Composable
fun LauncherSection(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settings = remember { LauncherSettings(context) }
    val scope = rememberCoroutineScope()

    val desktopCols by settings.desktopColumns.collectAsState(initial = 5)
    val desktopRows by settings.desktopRows.collectAsState(initial = 5)
    val dockCols by settings.dockColumns.collectAsState(initial = 5)
    val drawerCols by settings.drawerColumns.collectAsState(initial = 4)
    val iconScale by settings.iconSize.collectAsState(initial = 1.0f)
    val labelSize by settings.labelSize.collectAsState(initial = 12f)
    val showLabels by settings.showLabels.collectAsState(initial = true)
    val scrollEffect by settings.scrollEffect.collectAsState(initial = "slide")
    val searchEnabled by settings.searchBarEnabled.collectAsState(initial = true)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Launcher",
            style = MaterialTheme.typography.titleLarge,
        )

        Button(
            onClick = {
                // Open default launcher settings
                context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Set as Default Launcher")
        }

        // Desktop Grid
        Text("Desktop", style = MaterialTheme.typography.titleMedium)

        SettingSlider(
            label = "Columns: $desktopCols",
            value = desktopCols.toFloat(),
            range = 3f..9f,
            steps = 5,
            onChanged = { scope.launch { settings.setDesktopGrid(it.toInt(), desktopRows) } },
        )

        SettingSlider(
            label = "Rows: $desktopRows",
            value = desktopRows.toFloat(),
            range = 3f..9f,
            steps = 5,
            onChanged = { scope.launch { settings.setDesktopGrid(desktopCols, it.toInt()) } },
        )

        // Scroll effect picker
        Text("Scroll Effect", style = MaterialTheme.typography.bodyMedium)
        Column {
            ScrollEffects.allEffects.forEach { (key, name) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { scope.launch { settings.setScrollEffect(key) } }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        name,
                        color = if (scrollEffect == key) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    if (scrollEffect == key) {
                        Text("✓", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Dock
        Text("Dock", style = MaterialTheme.typography.titleMedium)
        SettingSlider(
            label = "Dock columns: $dockCols",
            value = dockCols.toFloat(),
            range = 3f..7f,
            steps = 3,
            onChanged = { scope.launch { settings.setDockColumns(it.toInt()) } },
        )

        // App Drawer
        Text("App Drawer", style = MaterialTheme.typography.titleMedium)
        SettingSlider(
            label = "Drawer columns: $drawerCols",
            value = drawerCols.toFloat(),
            range = 3f..6f,
            steps = 2,
            onChanged = { scope.launch { settings.setDrawerColumns(it.toInt()) } },
        )

        // Icons
        Text("Icons", style = MaterialTheme.typography.titleMedium)
        SettingSlider(
            label = "Icon size: ${(iconScale * 100).toInt()}%",
            value = iconScale,
            range = 0.5f..1.5f,
            steps = 9,
            onChanged = { scope.launch { settings.setIconSize(it) } },
        )
        SettingSlider(
            label = "Label size: ${labelSize.toInt()}sp",
            value = labelSize,
            range = 8f..18f,
            steps = 9,
            onChanged = { scope.launch { settings.setLabelSize(it) } },
        )
        SettingToggle(
            label = "Show icon labels",
            checked = showLabels,
            onChanged = { scope.launch { settings.setShowLabels(it) } },
        )

        // Search
        Text("Search", style = MaterialTheme.typography.titleMedium)
        SettingToggle(
            label = "Show search bar",
            checked = searchEnabled,
            onChanged = { scope.launch { settings.setSearchBarEnabled(it) } },
        )
    }
}

@Composable
fun SettingSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChanged: (Float) -> Unit,
) {
    var current by remember(value) { mutableFloatStateOf(value) }
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Slider(
        value = current,
        onValueChange = {
            current = it
            onChanged(it)
        },
        valueRange = range,
        steps = steps,
    )
}

@Composable
fun SettingToggle(
    label: String,
    checked: Boolean,
    onChanged: (Boolean) -> Unit,
) {
    var isChecked by remember(checked) { mutableStateOf(checked) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(
            checked = isChecked,
            onCheckedChange = {
                isChecked = it
                onChanged(it)
            },
        )
    }
}
