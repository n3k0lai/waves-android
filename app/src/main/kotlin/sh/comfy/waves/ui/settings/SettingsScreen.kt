package sh.comfy.waves.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import sh.comfy.waves.R
import sh.comfy.waves.data.WallpaperSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    wallpaperSettings: StateFlow<WallpaperSettings>,
    widgetTransparency: StateFlow<Float>,
    onFocalPointChanged: (x: Float, y: Float) -> Unit,
    onWidgetTransparencyChanged: (Float) -> Unit,
) {
    val settings by wallpaperSettings.collectAsStateWithLifecycle()
    val transparency by widgetTransparency.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            item {
                WallpaperSection(
                    focalX = settings.focalPointX,
                    focalY = settings.focalPointY,
                    onFocalPointChanged = onFocalPointChanged,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                HorizontalDivider()
            }

            item {
                IconSection(
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                HorizontalDivider()
            }

            item {
                WidgetSection(
                    transparency = transparency,
                    onTransparencyChanged = onWidgetTransparencyChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                )
            }
        }
    }
}
