package sh.comfy.waves

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import sh.comfy.waves.data.SettingsRepository
import sh.comfy.waves.data.WallpaperSettings
import sh.comfy.waves.ui.settings.SettingsScreen
import sh.comfy.waves.ui.theme.WavesTheme
import sh.comfy.waves.widget.WavesWidgetProvider

class MainActivity : ComponentActivity() {

    private val settingsRepo by lazy { SettingsRepository(this) }
    private val _wallpaperSettings = MutableStateFlow(WallpaperSettings())
    private val wallpaperSettings = _wallpaperSettings.asStateFlow()
    private val _widgetTransparency = MutableStateFlow(0.5f)
    private val widgetTransparency = _widgetTransparency.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            settingsRepo.wallpaperSettings.collectLatest { settings ->
                _wallpaperSettings.value = settings
            }
        }
        lifecycleScope.launch {
            settingsRepo.widgetTransparency.collectLatest { value ->
                _widgetTransparency.value = value
            }
        }

        setContent {
            WavesTheme {
                SettingsScreen(
                    wallpaperSettings = wallpaperSettings,
                    widgetTransparency = widgetTransparency,
                    onFocalPointChanged = { x, y ->
                        lifecycleScope.launch {
                            settingsRepo.updateFocalPoint(x, y)
                        }
                    },
                    onWidgetTransparencyChanged = { value ->
                        lifecycleScope.launch {
                            settingsRepo.updateWidgetTransparency(value)
                            WavesWidgetProvider.refreshAll(this@MainActivity)
                        }
                    },
                )
            }
        }
    }
}
