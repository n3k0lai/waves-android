package sh.comfy.waves.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import sh.comfy.waves.keyboard.core.KeyboardPreferences

@Composable
fun KeyboardSection(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isEnabled = remember { isKeyboardEnabled(context) }
    val prefs = remember { KeyboardPreferences(context) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Keyboard",
            style = MaterialTheme.typography.titleLarge,
        )

        if (!isEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    text = "Waves Keyboard is not enabled.\nEnable it in system settings to use it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Enable Keyboard")
            }

            OutlinedButton(
                onClick = {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showInputMethodPicker()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Switch Keyboard")
            }
        }

        Text(
            text = "Input",
            style = MaterialTheme.typography.titleMedium,
        )

        PrefToggle("Haptic feedback", prefs.hapticEnabled) { prefs.hapticEnabled = it }
        PrefToggle("Key sounds", prefs.soundEnabled) { prefs.soundEnabled = it }
        PrefToggle("Double-space period", prefs.doubleTapPeriod) { prefs.doubleTapPeriod = it }
        PrefToggle("Auto-capitalize", prefs.autoCapitalize) { prefs.autoCapitalize = it }

        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
        )

        // Waves background intensity slider
        var wavesIntensity by remember { mutableFloatStateOf(prefs.wavesIntensity) }
        Text(
            text = "Wave intensity",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Slider(
            value = wavesIntensity,
            onValueChange = {
                wavesIntensity = it
                prefs.wavesIntensity = it
            },
            valueRange = 0f..1f,
            steps = 9,
        )

        // Key height slider
        var keyHeight by remember { mutableFloatStateOf(prefs.keyHeight.toFloat()) }
        Text(
            text = "Key height: ${keyHeight.toInt()}dp",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Slider(
            value = keyHeight,
            onValueChange = {
                keyHeight = it
                prefs.keyHeight = it.toInt()
            },
            valueRange = 36f..60f,
            steps = 7,
        )
    }
}

@Composable
fun PrefToggle(
    label: String,
    value: Boolean,
    onChanged: (Boolean) -> Unit,
) {
    var checked by remember { mutableStateOf(value) }

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
            checked = checked,
            onCheckedChange = {
                checked = it
                onChanged(it)
            },
        )
    }
}

private fun isKeyboardEnabled(context: Context): Boolean {
    val enabledInputMethods = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_INPUT_METHODS,
    ) ?: return false
    return enabledInputMethods.contains("sh.comfy.waves")
}
