package sh.comfy.waves.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sh.comfy.waves.R
import sh.comfy.waves.widget.SystemInfoHelper

@Composable
fun WidgetSection(
    transparency: Float,
    onTransparencyChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val previewText = remember {
        val info = SystemInfoHelper.gather(context)
        SystemInfoHelper.format(info)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_widget_section),
            style = MaterialTheme.typography.titleLarge,
        )

        // Live preview of the widget
        val alpha = (1f - transparency).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = alpha))
                .padding(16.dp),
        ) {
            Text(
                text = previewText,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Color.White,
            )
        }

        Text(
            text = stringResource(R.string.widget_transparency_label),
            style = MaterialTheme.typography.bodyMedium,
        )

        Slider(
            value = transparency,
            onValueChange = onTransparencyChanged,
            valueRange = 0f..1f,
            steps = 9,
        )
    }
}
